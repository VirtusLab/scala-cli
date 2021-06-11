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
              Inputs.JavaFile(p, Some(d.path))
            case p if p.last.endsWith(".scala") =>
              Inputs.ScalaFile(p, Some(d.path))
            case p if p.last.endsWith(".sc") =>
              Inputs.Script(p, Some(d.path))
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

  sealed trait SingleFile extends OnDisk {
    def relativeTo: Option[os.Path]
    def withRelativeTo(newRelativeTo: Option[os.Path]): SingleFile
  }
  sealed trait Compiled extends Element
  sealed trait AnyScalaFile extends Compiled

  final case class Script(path: os.Path, relativeTo: Option[os.Path]) extends OnDisk with SingleFile with AnyScalaFile {
    def withRelativeTo(newRelativeTo: Option[os.Path]): Script =
      copy(relativeTo = newRelativeTo)
  }
  final case class ScalaFile(path: os.Path, relativeTo: Option[os.Path]) extends OnDisk with SingleFile with AnyScalaFile {
    def withRelativeTo(newRelativeTo: Option[os.Path]): ScalaFile =
      copy(relativeTo = newRelativeTo)
  }
  final case class JavaFile(path: os.Path, relativeTo: Option[os.Path]) extends OnDisk with SingleFile with Compiled {
    def withRelativeTo(newRelativeTo: Option[os.Path]): JavaFile =
      copy(relativeTo = newRelativeTo)
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
    def updateSingleFile(f: SingleFile): SingleFile =
      if (f.relativeTo.isEmpty && f.path.relativeTo(workspace).ups != 0) f.withRelativeTo(Some(f.path / os.up))
      else f
    val updatedElems = validElems.flatMap {
      case d: Directory => Seq(d)
      case f: SingleFile =>
        val isInDir = allDirs.exists(f.path.relativeTo(_).ups == 0)
        if (isInDir) Nil
        else Seq(updateSingleFile(f))
      case v: Virtual => Seq(v)
    }
    val mainClassElemOpt = validElems
      .collectFirst {
        case f: SingleFile => updateSingleFile(f)
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
      lazy val stdinOpt0 = stdinOpt
      if ((arg == "-" || arg == "-.scala" || arg == "_" || arg == "_.scala") && stdinOpt0.nonEmpty) Right(VirtualScalaFile(stdinOpt0.get, "stdin"))
      else if ((arg == "-.sc" || arg == "_.sc") && stdinOpt0.nonEmpty) Right(VirtualScript(stdinOpt0.get, "stdin"))
      else if (arg.endsWith(".sc")) Right(Script(path, None))
      else if (arg.endsWith(".scala")) Right(ScalaFile(path, None))
      else if (arg.endsWith(".java")) Right(JavaFile(path, None))
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
