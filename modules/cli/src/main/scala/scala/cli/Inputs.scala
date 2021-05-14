package scala.cli

import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import java.nio.charset.StandardCharsets
import java.math.BigInteger

final case class Inputs(
  head: Inputs.Element,
  tail: Seq[Inputs.Element],
  workspace: os.Path,
  cwd: os.Path,
  baseProjectName: String,
  mayAppendHash: Boolean
) {
  lazy val elements: Seq[Inputs.Element] = head +: tail
  def paths: Seq[String] = elements.map(_.path)

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
      val absPath = root0.resolve(elem.path).toAbsolutePath.normalize
      Iterator(prefix, absPath, "\n")
    }
    val md = MessageDigest.getInstance("SHA-1")
    md.update(it.mkString.getBytes(StandardCharsets.UTF_8))
    val digest = md.digest()
    val calculatedSum = new BigInteger(1, digest)
    String.format(s"%040x", calculatedSum).take(10)
  }
  lazy val projectName = {
    val needsSuffix = mayAppendHash && (elements match {
      case Seq(d: Inputs.Directory) => os.FilePath(d.path).resolveFrom(cwd) != workspace
      case _ => true
    })
    if (needsSuffix) baseProjectName + "-" + inputsHash
    else baseProjectName
  }
}

object Inputs {

  sealed abstract class Element extends Product with Serializable {
    def path: String
  }

  sealed trait SingleFile extends Element
  sealed trait Compiled extends Element

  final case class Script(path: String, relativeTo: Option[String]) extends SingleFile with Compiled
  final case class ScalaFile(path: String) extends SingleFile with Compiled
  final case class JavaFile(path: String) extends SingleFile with Compiled
  final case class Directory(path: String) extends Compiled
  final case class ResourceDirectory(path: String) extends Element

  def apply(
    args: Seq[String],
    cwd: os.Path,
    baseProjectName: String = "project"
  ): Either[String, Inputs] =
    if (args.isEmpty)
      Left("No inputs provided.")
    else {
      val validatedArgs = args.map { arg =>
        if (arg.endsWith(".sc")) Right(Script(arg, None))
        else if (arg.endsWith(".scala")) Right(ScalaFile(arg))
        else if (arg.endsWith(".java")) Right(JavaFile(arg))
        else if (os.isDir(os.FilePath(arg).resolveFrom(cwd))) Right(Directory(arg))
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
          case (true, n) if n >= 1 => Some("Directories and single files cannot be specified at the same time for now.")
          case (_, n) if n >= 2 => Some("Only single directories are accepted as input for now.")
          case _ => None
        }

        errorOpt match {
          case None =>
            val workspace = validElems.head match {
              case d: Directory => os.FilePath(d.path).resolveFrom(cwd)
              case e => os.FilePath(e.path).resolveFrom(cwd) / os.up
            }
            Right(Inputs(validElems.head, validElems.tail, workspace, cwd, baseProjectName, mayAppendHash = true))
          case Some(err) => Left(err)
        }
      } else
        Left("Invalid input(s): " + invalid.mkString(", "))
    }
}
