package scala.build.options

import shapeless._

trait ConfigMonoid[T] {
  def orElse(main: T, defaults: T): T
}

object ConfigMonoid {
  def apply[T](implicit instance: ConfigMonoid[T]): ConfigMonoid[T] = instance

  implicit val hnil: ConfigMonoid[HNil] = {
    (_, _) =>
      HNil
  }

  implicit def hcons[H, T <: HList](implicit
    headInstance: Lazy[ConfigMonoid[H]],
    tailInstance: Lazy[ConfigMonoid[T]]
  ): ConfigMonoid[H :: T] = {
    (main, defaults) =>
      val head = headInstance.value.orElse(main.head, defaults.head)
      val tail = tailInstance.value.orElse(main.tail, defaults.tail)
      head :: tail
  }

  implicit def generic[T, R <: HList](implicit
    gen: Generic.Aux[T, R],
    instance: Lazy[ConfigMonoid[R]]
  ): ConfigMonoid[T] =
    derive[T, R](gen, instance)

  def derive[T, R <: HList](implicit
    gen: Generic.Aux[T, R],
    instance: Lazy[ConfigMonoid[R]]
  ): ConfigMonoid[T] = {
    (main, defaults) =>
      val r = instance.value.orElse(gen.to(main), gen.to(defaults))
      gen.from(r)
  }

  implicit def option[T]: ConfigMonoid[Option[T]] = {
    (main, defaults) =>
      main.orElse(defaults)
  }

  implicit def seq[T]: ConfigMonoid[Seq[T]] = {
    (main, defaults) =>
      main ++ defaults
  }

  implicit def list[T]: ConfigMonoid[List[T]] = {
    (main, defaults) =>
      main ::: defaults
  }

  implicit val boolean: ConfigMonoid[Boolean] = {
    (main, defaults) =>
      main || defaults
  }

}
