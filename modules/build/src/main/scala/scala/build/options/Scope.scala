package scala.build.options

sealed abstract class Scope(val name: String) extends Product with Serializable {
  def inherits: Seq[Scope]       = Nil
  lazy val allScopes: Set[Scope] = inherits.toSet + this
}
object Scope {
  case object Main extends Scope("main")
  case object Test extends Scope("test") {
    override def inherits: Seq[Scope] =
      Seq(Main)
  }

  val all: Seq[Scope] = Seq(Main, Test)
}
