import scala.build.EitherCps.*

class E
class EE1 extends E
class EE2 extends E
class E2
class V
class VV extends V

val vv: Either[E, VV] = Right(new VV)

def ee3: Either[E2, V] = either {
  value(Left(new EE1))
  value(vv.left.map(_ => new EE2))
  new V
}
