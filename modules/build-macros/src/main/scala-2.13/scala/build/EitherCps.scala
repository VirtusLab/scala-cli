package scala.build

// originally adapted from https://github.com/scala/scala/blob/96a666989b6bee067b1029553c6684ef1cb6f6b1/src/partest/scala/tools/partest/async/OptionDsl.scala

import scala.annotation.compileTimeOnly
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object EitherCps {
  final case class Helper[E]() {
    def apply[T](body: T): Either[E, T] = macro impl
  }
  def either[E]: Helper[E] = new Helper[E]
  @compileTimeOnly("[async] `value` must be enclosed in `EitherCps.either`")
  def value[E, T](option: Either[E, T]): T = ???
  def impl(c: blackbox.Context)(body: c.Tree): c.Tree = {
    import c.universe._
    val awaitSym = typeOf[EitherCps.type].decl(TermName("value"))
    def mark(t: DefDef): Tree =
      c.internal.markForAsyncTransform(c.internal.enclosingOwner, t, awaitSym, Map.empty)
    val name = TypeName("stateMachine$async")
    val body0 = mark(
      q"""override def apply(tr$$async: _root_.scala.Either[_root_.scala.AnyRef, _root_.scala.AnyRef]) = $body"""
    )
    q"""
      final class $name extends _root_.scala.build.EitherStateMachine {
        $body0
      }
      new $name().start().asInstanceOf[${c.macroApplication.tpe}]
    """
  }
}

abstract class EitherStateMachine
    extends AsyncStateMachine[Either[AnyRef, AnyRef], Either[AnyRef, AnyRef]] {
  var result$async: Either[AnyRef, AnyRef] = _

  // FSM translated method
  def apply(tr$async: Either[AnyRef, AnyRef]): Unit

  // Required methods
  private[this] var state$async: Int                        = 0
  protected def state: Int                                  = state$async
  protected def state_=(s: Int): Unit                       = state$async = s
  protected def completeFailure(t: Throwable): Unit         = throw t
  protected def completeSuccess(value: AnyRef): Unit        = result$async = Right(value)
  protected def onComplete(f: Either[AnyRef, AnyRef]): Unit = ???
  protected def getCompleted(f: Either[AnyRef, AnyRef]): Either[AnyRef, AnyRef] =
    f
  protected def tryGet(tr: Either[AnyRef, AnyRef]): AnyRef = tr match {
    case Right(value) =>
      value.asInstanceOf[AnyRef]
    case Left(e) =>
      result$async = Left(e)
      this // sentinel value to indicate the dispatch loop should exit.
  }
  def start(): Either[AnyRef, AnyRef] = {
    apply(Right(null))
    result$async
  }
}
