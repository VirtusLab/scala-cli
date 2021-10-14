package scala.build.preprocessing.directives

import scala.build.Position

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
}
