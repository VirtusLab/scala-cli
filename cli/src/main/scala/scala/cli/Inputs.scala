package scala.cli

import java.nio.file.Files
import java.nio.file.Paths

final case class Inputs(
  head: Inputs.Element,
  tail: Seq[Inputs.Element],
  root: os.Path
) {
  lazy val elements: Seq[Inputs.Element] = head +: tail
  def paths: Seq[String] = elements.map(_.path)
}

object Inputs {

  sealed abstract class Element extends Product with Serializable {
    def path: String
  }

  final case class Script(path: String) extends Element
  final case class ScalaFile(path: String) extends Element
  final case class Directory(path: String) extends Element

  def apply(args: Seq[String], defaultRoot: os.Path = os.pwd): Either[String, Inputs] =
    if (args.isEmpty)
      Left("No inputs provided.")
    else {
      val validatedArgs = args.map { arg =>
        if (arg.endsWith(".sc")) Right(Script(arg))
        else if (arg.endsWith(".scala")) Right(ScalaFile(arg))
        else if (Files.isDirectory(Paths.get(arg))) Right(Directory(arg))
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

        val hasFiles = validElems.exists { case _: Script => true; case _: ScalaFile => true; case _ => false }
        val dirCount = validElems.count { case _: Directory => true; case _ => false }

        val errorOpt = (hasFiles, dirCount) match {
          case (true, n) if n >= 1 => Some("Directories and single files cannot be specified at the same time for now.")
          case (_, n) if n >= 2 => Some("Only single directories are accepted as input for now.")
          case _ => None
        }

        errorOpt match {
          case None =>
            val root = validElems.head match {
              case d: Directory => os.Path(Paths.get(d.path).toAbsolutePath.normalize)
              case _ => defaultRoot
            }
            Right(Inputs(validElems.head, validElems.tail, root))
          case Some(err) => Left(err)
        }
      } else
        Left("Invalid input(s): " + invalid.mkString(", "))
    }
}
