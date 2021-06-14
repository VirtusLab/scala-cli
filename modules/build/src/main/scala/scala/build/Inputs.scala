package scala.build

import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import java.nio.charset.StandardCharsets
import java.math.BigInteger

final case class Inputs(
  head: Inputs.Element,
  tail: Seq[Inputs.Element],
  mainClassElement: Option[Inputs.Element],
  workspace: os.Path,
  baseProjectName: String,
  mayAppendHash: Boolean
) {
  lazy val elements: Seq[Inputs.Element] = head +: tail

  def sourceFiles(): Seq[Inputs.SingleFile] =
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

  def virtualSourceFiles(): Seq[Inputs.Virtual] =
    elements.flatMap {
      case v: Inputs.Virtual =>
        Seq(v)
      case _ =>
        Nil
    }

  private lazy val inputsHash = {
    val root0 = workspace.toNIO
    val it = elements.iterator.flatMap {
      case elem: Inputs.OnDisk =>
        val prefix = elem match {
          case _: Inputs.Directory => "dir:"
          case _: Inputs.ResourceDirectory => "resource-dir:"
          case _: Inputs.JavaFile => "java:"
          case _: Inputs.ScalaFile => "scala:"
          case _: Inputs.Script => "sc:"
        }
        Iterator(prefix, elem.path.toString, "\n")
      case v: Inputs.Virtual =>
        Iterator("virtual:", v.source)
    }
    val md = MessageDigest.getInstance("SHA-1")
    md.update(it.mkString.getBytes(StandardCharsets.UTF_8))
    val digest = md.digest()
    val calculatedSum = new BigInteger(1, digest)
    String.format(s"%040x", calculatedSum).take(10)
  }
  lazy val projectName = {
    val needsSuffix = mayAppendHash && (elements match {
      case Seq(d: Inputs.Directory) => d.path != workspace
      case _ => true
    })
    if (needsSuffix) baseProjectName + "-" + inputsHash
    else baseProjectName
  }
}

object Inputs {

  sealed abstract class Element extends Product with Serializable

  sealed abstract class OnDisk extends Element {
    def path: os.Path
  }
  sealed abstract class Virtual extends Element {
    def content: Array[Byte]
    def source: String
  }

  sealed trait SingleFile extends OnDisk
  sealed trait Compiled extends Element
  sealed trait AnyScalaFile extends Compiled

  final case class Script(base: os.Path, subPath: os.SubPath) extends OnDisk with SingleFile with AnyScalaFile {
    lazy val path = base / subPath
  }
  final case class ScalaFile(base: os.Path, subPath: os.SubPath) extends OnDisk with SingleFile with AnyScalaFile {
    lazy val path = base / subPath
  }
  final case class JavaFile(base: os.Path, subPath: os.SubPath) extends OnDisk with SingleFile with Compiled {
    lazy val path = base / subPath
  }
  final case class Directory(path: os.Path) extends OnDisk with Compiled
  final case class ResourceDirectory(path: os.Path) extends OnDisk

  final case class VirtualScript(content: Array[Byte], source: String) extends Virtual with AnyScalaFile
  final case class VirtualScalaFile(content: Array[Byte], source: String) extends Virtual with AnyScalaFile

  private def forValidatedElems(
    validElems: Seq[Compiled],
    baseProjectName: String
  ): Inputs = {

    assert(validElems.nonEmpty)

    val hasFiles = validElems.exists { case _: SingleFile => true; case _ => false }
    val dirCount = validElems.count { case _: Directory => true; case _ => false }

    val workspace = validElems
      .collectFirst {
        case d: Directory => d.path
      }
      .getOrElse {
        validElems.head match {
          case elem: SingleFile => elem.path / os.up
          case _: Virtual => os.pwd
          case _: Directory => sys.error("Can't happen")
        }
      }
    val allDirs = validElems.collect { case d: Directory => d.path }
    val updatedElems = validElems.filter {
      case f: SingleFile =>
        val isInDir = allDirs.exists(f.path.relativeTo(_).ups == 0)
        !isInDir
      case _: Directory => true
      case _: Virtual => true
    }
    val mainClassElemOpt = validElems
      .collectFirst {
        case f: SingleFile => f
      }
    Inputs(updatedElems.head, updatedElems.tail, mainClassElemOpt, workspace, baseProjectName, mayAppendHash = true)
  }

  private def forNonEmptyArgs(
    args: Seq[String],
    cwd: os.Path,
    baseProjectName: String,
    stdinOpt: => Option[Array[Byte]],
    acceptFds: Boolean
  ): Either[String, Inputs] = {
    val validatedArgs = args.map { arg =>
      val path = os.Path(arg, cwd)
      val dir = path / os.up
      val subPath = path.subRelativeTo(dir)
      lazy val stdinOpt0 = stdinOpt
      if ((arg == "-" || arg == "-.scala" || arg == "_" || arg == "_.scala") && stdinOpt0.nonEmpty) Right(VirtualScalaFile(stdinOpt0.get, "stdin"))
      else if ((arg == "-.sc" || arg == "_.sc") && stdinOpt0.nonEmpty) Right(VirtualScript(stdinOpt0.get, "stdin"))
      else if (arg.endsWith(".sc")) Right(Script(dir, subPath))
      else if (arg.endsWith(".scala")) Right(ScalaFile(dir, subPath))
      else if (arg.endsWith(".java")) Right(JavaFile(dir, subPath))
      else if (os.isDir(path)) Right(Directory(path))
      else if (acceptFds && arg.startsWith("/dev/fd/")) {
        val content = os.read.bytes(os.Path(arg, cwd))
        Right(VirtualScript(content, arg))
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

      Right(forValidatedElems(validElems, baseProjectName))
    } else
      Left(invalid.mkString(System.lineSeparator()))
  }

  def apply(
    args: Seq[String],
    cwd: os.Path,
    directories: Directories,
    baseProjectName: String = "project",
    defaultInputs: Option[Inputs] = None,
    stdinOpt: => Option[Array[Byte]] = None,
    acceptFds: Boolean = false
  ): Either[String, Inputs] =
    if (args.isEmpty)
      defaultInputs.toRight("No inputs provided (expected files with .scala or .sc extensions, and / or directories).")
    else
      forNonEmptyArgs(args, cwd, baseProjectName, stdinOpt, acceptFds)

  def default(cwd: os.Path = Os.pwd): Inputs =
    Inputs(
      head = Directory(cwd),
      tail = Nil,
      mainClassElement = None,
      workspace = cwd,
      baseProjectName = "project",
      mayAppendHash = true
    )
}
