package scala.build.options

sealed abstract class Scope(val name: String, private val index: Int) extends Product
    with Serializable {
  def inherits: Seq[Scope]       = Nil
  lazy val allScopes: Set[Scope] = inherits.toSet + this
}
object Scope {
  case object Main extends Scope("main", 0)
  case object Test extends Scope("test", 1) {
    override def inherits: Seq[Scope] =
      Seq(Main)
  }

  val all: Seq[Scope] = Seq(Main, Test)

  implicit val ordering: Ordering[Scope] =
    Ordering.Int.on(_.index)
}
