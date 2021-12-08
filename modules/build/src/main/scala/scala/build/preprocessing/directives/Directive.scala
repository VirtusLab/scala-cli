package scala.build.preprocessing.directives

import scala.build.Position
import scala.build.errors.{BuildException, ForbiddenPathReferenceError}
import scala.build.preprocessing.ScopePath

final case class Directive(
  tpe: Directive.Type,
  values: Seq[String],
  scope: Option[String],
  isComment: Boolean,
  position: Position
)

object Directive {
  sealed abstract class Type(val name: String) extends Product with Serializable
  case object Using                            extends Type("using")
  case object Require                          extends Type("require")

  def osRootResource(cwd: ScopePath): (Option[os.SubPath], Option[os.Path]) =
    cwd.root match {
      case Left(_)     => (Some(cwd.path), None)
      case Right(root) => (None, Some(root / cwd.path))
    }

  def osRoot(cwd: ScopePath, pos: Option[Position]): Either[BuildException, os.Path] =
    cwd.root match {
      case Left(virtualRoot) =>
        Left(new ForbiddenPathReferenceError(virtualRoot, pos))
      case Right(root) =>
        Right(root / cwd.path)
    }
}
