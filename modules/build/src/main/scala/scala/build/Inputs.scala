package scala.build

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.zip.{ZipEntry, ZipInputStream}

import scala.annotation.tailrec
import scala.build.options.Scope
import scala.build.preprocessing.ScopePath
import scala.util.matching.Regex

final case class Inputs(
  elements: Seq[Inputs.Element],
  mainClassElement: Option[Inputs.SourceFile],
  workspace: os.Path,
  baseProjectName: String,
  mayAppendHash: Boolean
) {

  def isEmpty: Boolean =
    elements.isEmpty

  def singleFiles(): Seq[Inputs.SingleFile] =
    elements.flatMap {
      case f: Inputs.SingleFile => Seq(f)
      case d: Inputs.Directory =>
        os.walk.stream(d.path)
          .filter { p =>
            !p.relativeTo(d.path).segments.exists(_.startsWith("."))
          }
          .filter(os.isFile(_))
          .collect {
            case p if p.last.endsWith(".java") =>
              Inputs.JavaFile(d.path, p.subRelativeTo(d.path))
            case p if p.last.endsWith(".scala") =>
              Inputs.ScalaFile(d.path, p.subRelativeTo(d.path))
            case p if p.last.endsWith(".sc") =>
              Inputs.Script(d.path, p.subRelativeTo(d.path))
          }
          .toVector
      case _: Inputs.ResourceDirectory =>
        Nil
      case _: Inputs.Virtual =>
        Nil
    }

  def sourceFiles(): Seq[Inputs.SourceFile] =
    singleFiles.collect {
      case f: Inputs.SourceFile => f
    }

  def virtualSourceFiles(): Seq[Inputs.Virtual] =
    elements.flatMap {
      case v: Inputs.Virtual =>
        Seq(v)
      case _ =>
        Nil
    }

  def flattened(): Seq[Inputs.SingleElement] =
    elements.flatMap {
      case f: Inputs.SingleFile => Seq(f)
      case d: Inputs.Directory =>
        os.walk.stream(d.path)
          .filter { p =>
            !p.relativeTo(d.path).segments.exists(_.startsWith("."))
          }
          .filter(os.isFile(_))
          .collect {
            case p if p.last.endsWith(".java") =>
              Inputs.JavaFile(d.path, p.subRelativeTo(d.path))
            case p if p.last.endsWith(".scala") =>
              Inputs.ScalaFile(d.path, p.subRelativeTo(d.path))
            case p if p.last.endsWith(".sc") =>
              Inputs.Script(d.path, p.subRelativeTo(d.path))
          }
          .toVector
      case _: Inputs.ResourceDirectory =>
        Nil
      case v: Inputs.Virtual =>
        Seq(v)
    }

  private lazy val inputsHash: String =
    Inputs.inputsHash(elements)
  lazy val projectName = {
    val needsSuffix = mayAppendHash && (elements match {
      case Seq(d: Inputs.Directory) => d.path != workspace
      case _                        => true
    })
    if (needsSuffix) baseProjectName + "-" + inputsHash
    else baseProjectName
  }

  def scopeProjectName(scope: Scope): String =
    if (scope == Scope.Main) projectName
    else projectName + "-" + scope.name

  def add(extraElements: Seq[Inputs.Element]): Inputs =
    if (elements.isEmpty) this
    else copy(elements = elements ++ extraElements)

  def generatedSrcRoot(scope: Scope): os.Path =
    workspace / ".scala" / projectName / "src_generated" / scope.name

  private def inHomeDir(directories: Directories): Inputs =
    copy(
      workspace = Inputs.homeWorkspace(elements, directories),
      mayAppendHash = false
    )
  def avoid(forbidden: Seq[os.Path], directories: Directories): Inputs =
    if (forbidden.exists(workspace.startsWith)) inHomeDir(directories)
    else this
  def checkAttributes(directories: Directories): Inputs = {
    def existingParent(p: os.Path): Option[os.Path] =
      if (os.exists(p)) Some(p)
      else if (p.segmentCount <= 0) None
      else existingParent(p / os.up)
    val canWrite = existingParent(workspace)
      .map(_.toIO.canWrite()) // Wondering if there's a better way to do that…
      .getOrElse(true)
    if (canWrite) this
    else inHomeDir(directories)
  }
  def sourceHash(): String = {
    def bytes(s: String): Array[Byte] = s.getBytes(StandardCharsets.UTF_8)
    val it = elements.iterator.flatMap {
      case elem: Inputs.OnDisk =>
        val content = elem match {
          case _: Inputs.Directory         => "dir:"
          case _: Inputs.ResourceDirectory => "resource-dir:"
          case _                           => os.read(elem.path)
        }
        Iterator(elem.path.toString, content, "\n").map(bytes)
      case v: Inputs.Virtual =>
        Iterator(v.content, bytes("\n"))
    }
    val md = MessageDigest.getInstance("SHA-1")
    it.foreach(md.update(_))
    val digest        = md.digest()
    val calculatedSum = new BigInteger(1, digest)
    String.format(s"%040x", calculatedSum)
  }
}

object Inputs {

  sealed abstract class Element extends Product with Serializable

  sealed trait SingleElement extends Element

  sealed trait AnyScript extends Element

  sealed abstract class OnDisk extends Element {
    def path: os.Path
  }
  sealed abstract class Virtual extends SingleElement {
    def content: Array[Byte]
    def source: String

    def subPath: os.SubPath = {
      val idx = source.lastIndexOf('/')
      os.sub / source.drop(idx + 1)
    }

    def scopePath: ScopePath =
      ScopePath(Left(source), subPath)
  }

  sealed trait SingleFile   extends OnDisk with SingleElement
  sealed trait SourceFile   extends SingleFile
  sealed trait Compiled     extends Element
  sealed trait AnyScalaFile extends Compiled

  final case class Script(base: os.Path, subPath: os.SubPath)
      extends OnDisk with SourceFile with AnyScalaFile with AnyScript {
    lazy val path = base / subPath
  }
  final case class ScalaFile(base: os.Path, subPath: os.SubPath)
      extends OnDisk with SourceFile with AnyScalaFile {
    lazy val path = base / subPath
  }
  final case class JavaFile(base: os.Path, subPath: os.SubPath)
      extends OnDisk with SourceFile with Compiled {
    lazy val path = base / subPath
  }
  final case class Directory(path: os.Path)         extends OnDisk with Compiled
  final case class ResourceDirectory(path: os.Path) extends OnDisk

  final case class VirtualScript(content: Array[Byte], source: String, wrapperPath: os.SubPath)
      extends Virtual with AnyScalaFile with AnyScript
  final case class VirtualScalaFile(content: Array[Byte], source: String)
      extends Virtual with AnyScalaFile
  final case class VirtualJavaFile(content: Array[Byte], source: String)
      extends Virtual with Compiled
  final case class VirtualData(content: Array[Byte], source: String)
      extends Virtual

  private def inputsHash(elements: Seq[Element]): String = {
    def bytes(s: String): Array[Byte] = s.getBytes(StandardCharsets.UTF_8)
    val it = elements.iterator.flatMap {
      case elem: Inputs.OnDisk =>
        val prefix = elem match {
          case _: Inputs.Directory         => "dir:"
          case _: Inputs.ResourceDirectory => "resource-dir:"
          case _: Inputs.JavaFile          => "java:"
          case _: Inputs.ScalaFile         => "scala:"
          case _: Inputs.Script            => "sc:"
        }
        Iterator(prefix, elem.path.toString, "\n").map(bytes)
      case v: Inputs.Virtual =>
        Iterator(bytes("virtual:"), v.content, bytes("\n"))
    }
    val md = MessageDigest.getInstance("SHA-1")
    it.foreach(md.update(_))
    val digest        = md.digest()
    val calculatedSum = new BigInteger(1, digest)
    String.format(s"%040x", calculatedSum).take(10)
  }

  def homeWorkspace(elements: Seq[Element], directories: Directories) = {
    val hash0 = inputsHash(elements)
    val dir   = directories.virtualProjectsDir / hash0.take(2) / s"project-${hash0.drop(2)}"
    os.makeDir.all(dir)
    dir
  }

  private def forValidatedElems(
    validElems: Seq[Element],
    baseProjectName: String,
    directories: Directories
  ): Inputs = {

    assert(validElems.nonEmpty)

    val (workspace, needsHash) = validElems
      .collectFirst {
        case d: Directory => (d.path, true)
      }
      .getOrElse {
        validElems.head match {
          case elem: SourceFile => (elem.path / os.up, true)
          case _: Virtual =>
            val dir = homeWorkspace(validElems, directories)
            (dir, false)
          case _: Directory => sys.error("Can't happen")
        }
      }
    val allDirs = validElems.collect { case d: Directory => d.path }
    val updatedElems = validElems.filter {
      case f: SourceFile =>
        val isInDir = allDirs.exists(f.path.relativeTo(_).ups == 0)
        !isInDir
      case _: Directory => true
      case _: Virtual   => true
    }
    val mainClassElemOpt = validElems
      .collectFirst {
        case f: SourceFile => f
      }
    Inputs(updatedElems, mainClassElemOpt, workspace, baseProjectName, mayAppendHash = needsHash)
  }

  private val githubGistsArchiveRegex: Regex =
    s""":\\/\\/gist\\.github\\.com\\/[^\\/]*?\\/[^\\/]*$$""".r

  private def resolve(path: String, content: Array[Byte]): Element = {
    val wrapperPath =
      os.sub / path.split("/").last

    if (path.endsWith(".scala")) VirtualScalaFile(content, path)
    else if (path.endsWith(".java")) VirtualJavaFile(content, path)
    else if (path.endsWith(".sc")) VirtualScript(content, path, wrapperPath)
    else VirtualData(content, path)
  }

  private def resolveZipArchive(content: Array[Byte]): Seq[Element] = {
    val zipInputStream = new ZipInputStream(new ByteArrayInputStream(content))
    @tailrec
    def readArchive(acc: Seq[Element]): Seq[Element] =
      zipInputStream.getNextEntry() match {
        case entry: ZipEntry if !entry.isDirectory =>
          val content = {
            val baos = new ByteArrayOutputStream
            val buf  = Array.ofDim[Byte](16 * 1024)
            var read = -1
            while ({
              read = zipInputStream.read(buf)
              read >= 0
            })
              if (read > 0)
                baos.write(buf, 0, read)
            baos.toByteArray
          }
          readArchive(resolve(entry.getName, content) +: acc)
        case _: ZipEntry => readArchive(acc)
        case _           => acc
      }
    readArchive(Nil)
  }

  private def forNonEmptyArgs(
    args: Seq[String],
    cwd: os.Path,
    directories: Directories,
    baseProjectName: String,
    download: String => Either[String, Array[Byte]],
    stdinOpt: => Option[Array[Byte]],
    acceptFds: Boolean
  ): Either[String, Inputs] = {
    val validatedArgs = args.zipWithIndex.map {
      case (arg, idx) =>
        lazy val path      = os.Path(arg, cwd)
        lazy val dir       = path / os.up
        lazy val subPath   = path.subRelativeTo(dir)
        lazy val stdinOpt0 = stdinOpt
        val isStdin = (arg == "-.scala" || arg == "_" || arg == "_.scala") &&
          stdinOpt0.nonEmpty
        if (isStdin) Right(Seq(VirtualScalaFile(stdinOpt0.get, "<stdin>")))
        else if ((arg == "-" || arg == "-.sc" || arg == "_.sc") && stdinOpt0.nonEmpty)
          Right(Seq(VirtualScript(stdinOpt0.get, "stdin", os.sub / "stdin.sc")))
        else if (arg.endsWith(".zip") && os.exists(os.Path(arg, cwd))) {
          val content = os.read.bytes(os.Path(arg, cwd))
          Right(resolveZipArchive(content))
        }
        else if (arg.contains("://")) {
          val url =
            if (githubGistsArchiveRegex.findFirstMatchIn(arg).nonEmpty) s"$arg/download" else arg
          download(url).map { content =>
            if (githubGistsArchiveRegex.findFirstMatchIn(arg).nonEmpty)
              resolveZipArchive(content)
            else
              List(resolve(url, content))
          }
        }
        else if (arg.endsWith(".sc")) Right(Seq(Script(dir, subPath)))
        else if (arg.endsWith(".scala")) Right(Seq(ScalaFile(dir, subPath)))
        else if (arg.endsWith(".java")) Right(Seq(JavaFile(dir, subPath)))
        else if (os.isDir(path)) Right(Seq(Directory(path)))
        else if (acceptFds && arg.startsWith("/dev/fd/")) {
          val content = os.read.bytes(os.Path(arg, cwd))
          Right(Seq(VirtualScript(content, arg, os.sub / s"input-${idx + 1}.sc")))
        }
        else {
          val msg =
            if (os.exists(path))
              s"$arg: unrecognized source type (expected .scala or .sc extension, or a directory)"
            else s"$arg: not found"
          Left(msg)
        }
    }
    val invalid = validatedArgs.collect {
      case Left(msg) => msg
    }
    if (invalid.isEmpty) {
      val validElems = validatedArgs.collect {
        case Right(elem) => elem
      }.flatten
      assert(validElems.nonEmpty)

      Right(forValidatedElems(validElems, baseProjectName, directories))
    }
    else
      Left(invalid.mkString(System.lineSeparator()))
  }

  def apply(
    args: Seq[String],
    cwd: os.Path,
    directories: Directories,
    baseProjectName: String = "project",
    defaultInputs: () => Option[Inputs] = () => None,
    download: String => Either[String, Array[Byte]] = _ => Left("URL not supported"),
    stdinOpt: => Option[Array[Byte]] = None,
    acceptFds: Boolean = false
  ): Either[String, Inputs] =
    if (args.isEmpty)
      defaultInputs().toRight(
        "No inputs provided (expected files with .scala or .sc extensions, and / or directories)."
      )
    else
      forNonEmptyArgs(args, cwd, directories, baseProjectName, download, stdinOpt, acceptFds)

  def default(): Option[Inputs] =
    None

  def empty(workspace: os.Path): Inputs =
    Inputs(
      elements = Nil,
      mainClassElement = None,
      workspace = workspace,
      baseProjectName = "project",
      mayAppendHash = true
    )
}
