package scala.cli

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

  final case class Script(path: os.Path, relativeTo: Option[os.Path]) extends SingleFile with Compiled {
    def withRelativeTo(newRelativeTo: Option[os.Path]): Script =
      copy(relativeTo = newRelativeTo)
  }
  final case class ScalaFile(path: os.Path, relativeTo: Option[os.Path]) extends SingleFile with Compiled {
    def withRelativeTo(newRelativeTo: Option[os.Path]): ScalaFile =
      copy(relativeTo = newRelativeTo)
  }
  final case class JavaFile(path: os.Path, relativeTo: Option[os.Path]) extends SingleFile with Compiled {
    def withRelativeTo(newRelativeTo: Option[os.Path]): JavaFile =
      copy(relativeTo = newRelativeTo)
  }
  final case class Directory(path: os.Path) extends Compiled
  final case class ResourceDirectory(path: os.Path) extends Element

  def apply(
    args: Seq[String],
    cwd: os.Path,
    baseProjectName: String = "project"
  ): Either[String, Inputs] =
    if (args.isEmpty)
      Left("No inputs provided.")
    else {
      val validatedArgs = args.map { arg =>
        val path = os.Path(arg, cwd)
        if (arg.endsWith(".sc")) Right(Script(path, None))
        else if (arg.endsWith(".scala")) Right(ScalaFile(path, None))
        else if (arg.endsWith(".java")) Right(JavaFile(path, None))
        else if (os.isDir(path)) Right(Directory(path))
        else Left(arg)
      }
      val invalid = validatedArgs.collect {
        case Left(msg) => msg
      }
      if (invalid.isEmpty) {
        val validElems = validatedArgs.collect {
          case Right(elem) => elem
        }

        // FIXME We should allow for different roots, depending on the files
        //       so that users could pass things like 'shared/src/main/scala jvm/src/main/scala'.
        //       In the mean time, we don't accept single files alongside a directory, or several directories.

        val hasFiles = validElems.exists { case _: SingleFile => true; case _ => false }
        val dirCount = validElems.count { case _: Directory => true; case _ => false }

        val errorOpt = (hasFiles, dirCount) match {
          case (_, n) if n >= 2 => Some("Only single directories are accepted as input for now.")
          case _ => None
        }

        errorOpt match {
          case None =>
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
            Right(Inputs(updatedElems.head, updatedElems.tail, mainClassElemOpt, workspace, baseProjectName, mayAppendHash = true))
          case Some(err) => Left(err)
        }
      } else
        Left("Invalid input(s): " + invalid.mkString(", "))
    }
}
