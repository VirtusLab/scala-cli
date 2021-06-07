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
    }

  private lazy val inputsHash = {
    val root0 = workspace.toNIO
    val it = elements.iterator.flatMap { elem =>
      val prefix = elem match {
        case _: Inputs.Directory => "dir:"
        case _: Inputs.ResourceDirectory => "resource-dir:"
        case _: Inputs.JavaFile => "java:"
        case _: Inputs.ScalaFile => "scala:"
        case _: Inputs.Script => "sc:"
      }
      Iterator(prefix, elem.path.toString, "\n")
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

  sealed abstract class Element extends Product with Serializable {
    def path: os.Path
  }

  sealed trait SingleFile extends Element {
    def relativeTo: Option[os.Path]
    def withRelativeTo(newRelativeTo: Option[os.Path]): SingleFile
  }
  sealed trait Compiled extends Element
  sealed trait AnyScalaFile extends Compiled

  final case class Script(path: os.Path, relativeTo: Option[os.Path]) extends SingleFile with AnyScalaFile {
    def withRelativeTo(newRelativeTo: Option[os.Path]): Script =
      copy(relativeTo = newRelativeTo)
  }
  final case class ScalaFile(path: os.Path, relativeTo: Option[os.Path]) extends SingleFile with AnyScalaFile {
    def withRelativeTo(newRelativeTo: Option[os.Path]): ScalaFile =
      copy(relativeTo = newRelativeTo)
  }
  final case class JavaFile(path: os.Path, relativeTo: Option[os.Path]) extends SingleFile with Compiled {
    def withRelativeTo(newRelativeTo: Option[os.Path]): JavaFile =
      copy(relativeTo = newRelativeTo)
  }
  final case class Directory(path: os.Path) extends Compiled
  final case class ResourceDirectory(path: os.Path) extends Element

  private def forValidatedElems(
    validElems: Seq[Compiled],
    baseProjectName: String
  ): Inputs = {

    val hasFiles = validElems.exists { case _: SingleFile => true; case _ => false }
    val dirCount = validElems.count { case _: Directory => true; case _ => false }

    val workspace = validElems
      .collectFirst {
        case d: Directory => d.path
      }
      .getOrElse {
        val elem = validElems.head
        assert(elem.isInstanceOf[SingleFile])
        elem.asInstanceOf[SingleFile].path / os.up
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
    baseProjectName: String
  ): Either[String, Inputs] = {
    val validatedArgs = args.map { arg =>
      val path = os.Path(arg, cwd)
      if (arg.endsWith(".sc")) Right(Script(path, None))
      else if (arg.endsWith(".scala")) Right(ScalaFile(path, None))
      else if (arg.endsWith(".java")) Right(JavaFile(path, None))
      else if (os.isDir(path)) Right(Directory(path))
      else {
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

      Right(forValidatedElems(validElems, baseProjectName))
    } else
      Left(invalid.mkString(System.lineSeparator()))
  }

  def apply(
    args: Seq[String],
    cwd: os.Path,
    baseProjectName: String = "project",
    defaultInputs: Option[Inputs] = None
  ): Either[String, Inputs] =
    if (args.isEmpty)
      defaultInputs.toRight("No inputs provided (expected files with .scala or .sc extensions, and / or directories).")
    else
      forNonEmptyArgs(args, cwd, baseProjectName)

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
