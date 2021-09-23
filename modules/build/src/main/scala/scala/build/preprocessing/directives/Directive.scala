package scala.build.preprocessing.directives

final case class Directive(
  tpe: Directive.Type,
  values: Seq[String]
)

object Directive {
  sealed abstract class Type(val name: String) extends Product with Serializable
  case object Using                            extends Type("using")
  case object Require                          extends Type("require")
}
