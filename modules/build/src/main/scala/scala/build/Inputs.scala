package scala.build

import java.math.BigInteger
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.security.MessageDigest

import scala.util.Properties

final case class Inputs(
  head: Inputs.Element,
  tail: Seq[Inputs.Element],
  mainClassElement: Option[Inputs.Element],
  workspace: os.Path,
  baseProjectName: String,
  mayAppendHash: Boolean
) {
  lazy val elements: Seq[Inputs.Element] = head +: tail

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
            case p if p.last == "scala.conf" || p.last.endsWith(".scala.conf") =>
              Inputs.ConfigFile(p)
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
            case p if p.last == "scala.conf" || p.last.endsWith(".scala.conf") =>
              Inputs.ConfigFile(p)
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
      case _ => true
    })
    if (needsSuffix) baseProjectName + "-" + inputsHash
    else baseProjectName
  }

  def add(elements: Seq[Inputs.Element]): Inputs =
    if (elements.isEmpty) this
    else copy(tail = tail ++ elements)

  def generatedSrcRoot: os.Path =
    workspace / ".scala" / projectName / "src_generated"
}

object Inputs {

  sealed abstract class Element extends Product with Serializable

  sealed trait SingleElement extends Element

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
  }

  sealed trait SingleFile extends OnDisk with SingleElement
  sealed trait SourceFile extends SingleFile
  sealed trait Compiled extends Element
  sealed trait AnyScalaFile extends Compiled

  final case class Script(base: os.Path, subPath: os.SubPath) extends OnDisk with SourceFile with AnyScalaFile {
    lazy val path = base / subPath
  }
  final case class ScalaFile(base: os.Path, subPath: os.SubPath) extends OnDisk with SourceFile with AnyScalaFile {
    lazy val path = base / subPath
  }
  final case class JavaFile(base: os.Path, subPath: os.SubPath) extends OnDisk with SourceFile with Compiled {
    lazy val path = base / subPath
  }
  final case class Directory(path: os.Path) extends OnDisk with Compiled
  final case class ResourceDirectory(path: os.Path) extends OnDisk

  final case class ConfigFile(path: os.Path) extends SingleFile

  final case class VirtualScript(content: Array[Byte], source: String, wrapperPath: os.SubPath) extends Virtual with AnyScalaFile
  final case class VirtualScalaFile(content: Array[Byte], source: String) extends Virtual with AnyScalaFile
  final case class VirtualJavaFile(content: Array[Byte], source: String) extends Virtual with Compiled

  private def inputsHash(elements: Seq[Element]): String = {
    def bytes(s: String): Array[Byte] = s.getBytes(StandardCharsets.UTF_8)
    val it = elements.iterator.flatMap {
      case elem: Inputs.OnDisk =>
        val prefix = elem match {
          case _: Inputs.Directory => "dir:"
          case _: Inputs.ResourceDirectory => "resource-dir:"
          case _: Inputs.JavaFile => "java:"
          case _: Inputs.ScalaFile => "scala:"
          case _: Inputs.Script => "sc:"
          case _: Inputs.ConfigFile => "config:"
        }
        Iterator(prefix, elem.path.toString, "\n").map(bytes)
      case v: Inputs.Virtual =>
        Iterator(bytes("virtual:"), v.content, bytes("\n"))
    }
    val md = MessageDigest.getInstance("SHA-1")
    it.foreach(md.update(_))
    val digest = md.digest()
    val calculatedSum = new BigInteger(1, digest)
    String.format(s"%040x", calculatedSum).take(10)
  }

  private def forValidatedElems(
    validElems: Seq[Compiled],
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
            val hash0 = inputsHash(validElems)
            val dir = directories.virtualProjectsDir / hash0.take(2) / s"project-${hash0.drop(2)}"
            os.makeDir.all(dir)
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
      case _: Virtual => true
    }
    val mainClassElemOpt = validElems
      .collectFirst {
        case f: SourceFile => f
      }
    Inputs(updatedElems.head, updatedElems.tail, mainClassElemOpt, workspace, baseProjectName, mayAppendHash = needsHash)
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
        lazy val path = os.Path(arg, cwd)
        lazy val dir = path / os.up
        lazy val subPath = path.subRelativeTo(dir)
        lazy val stdinOpt0 = stdinOpt
        if ((arg == "-" || arg == "-.scala" || arg == "_" || arg == "_.scala") && stdinOpt0.nonEmpty) Right(VirtualScalaFile(stdinOpt0.get, "<stdin>"))
        else if ((arg == "-.sc" || arg == "_.sc") && stdinOpt0.nonEmpty) Right(VirtualScript(stdinOpt0.get, "<stdin>", os.sub / "stdin.sc"))
        else if (arg.contains("://"))
          download(arg).map { content =>
            val wrapperPath = {
              val u = new URI(arg) // FIXME Ignore parsing errors?
              val it = Option(u.getScheme).iterator ++
                Option(u.getAuthority).iterator ++
                Option(u.getPath).iterator.flatMap(_.split('/').iterator)
              os.sub / it.filter(_.nonEmpty).toVector
            }
            if (arg.endsWith(".scala")) VirtualScalaFile(content, arg)
            else if (arg.endsWith(".java")) VirtualJavaFile(content, arg)
            else VirtualScript(content, arg, wrapperPath)
          }
        else if (arg.endsWith(".sc")) Right(Script(dir, subPath))
        else if (arg.endsWith(".scala")) Right(ScalaFile(dir, subPath))
        else if (arg.endsWith(".java")) Right(JavaFile(dir, subPath))
        else if (os.isDir(path)) Right(Directory(path))
        else if (acceptFds && arg.startsWith("/dev/fd/")) {
          val content = os.read.bytes(os.Path(arg, cwd))
          Right(VirtualScript(content, arg, os.sub / s"input-${idx + 1}.sc"))
        } else {
          val msg =
            if (os.exists(path)) s"$arg: unrecognized source type (expected .scala or .sc extension, or a directory)"
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
      }
      assert(validElems.nonEmpty)

      Right(forValidatedElems(validElems, baseProjectName, directories))
    } else
      Left(invalid.mkString(System.lineSeparator()))
  }

  def apply(
    args: Seq[String],
    cwd: os.Path,
    directories: Directories,
    baseProjectName: String = "project",
    defaultInputs: Option[Inputs] = None,
    download: String => Either[String, Array[Byte]] = _ => Left("URL not supported"),
    stdinOpt: => Option[Array[Byte]] = None,
    acceptFds: Boolean = false
  ): Either[String, Inputs] =
    if (args.isEmpty)
      defaultInputs.toRight("No inputs provided (expected files with .scala or .sc extensions, and / or directories).")
    else
      forNonEmptyArgs(args, cwd, directories, baseProjectName, download, stdinOpt, acceptFds)

  def default(cwd: os.Path = Os.pwd): Option[Inputs] = {
    val hasConf = os.isFile(cwd / "scala.conf") ||
      os.list(cwd).filter(os.isFile(_)).exists(_.last.endsWith(".scala.conf"))
    if (hasConf)
      Some {
        Inputs(
          head = Directory(cwd),
          tail = Nil,
          mainClassElement = None,
          workspace = cwd,
          baseProjectName = "project",
          mayAppendHash = true
        )
      }
    else
      None
  }
}
