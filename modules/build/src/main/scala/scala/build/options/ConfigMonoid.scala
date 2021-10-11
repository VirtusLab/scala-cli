package scala.build.options

import shapeless._

trait ConfigMonoid[T] {
  def zero: T
  def orElse(main: T, defaults: T): T
}

object ConfigMonoid {
  def apply[T](implicit instance: ConfigMonoid[T]): ConfigMonoid[T] = instance

  private def instance[T](zeroValue: => T)(orElseFn: (T, T) => T): ConfigMonoid[T] =
    new ConfigMonoid[T] {
      def zero                         = zeroValue
      def orElse(main: T, defaults: T) = orElseFn(main, defaults)
    }

  trait HListConfigMonoid[T <: HList] {
    def zero: T
    def orElse(main: T, defaults: T): T
  }

  object HListConfigMonoid {

    private def instance[T <: HList](zeroValue: => T)(orElseFn: (T, T) => T): HListConfigMonoid[T] =
      new HListConfigMonoid[T] {
        def zero                         = zeroValue
        def orElse(main: T, defaults: T) = orElseFn(main, defaults)
      }

    implicit val hnil: HListConfigMonoid[HNil] = instance(HNil: HNil) {
      (_, _) =>
        HNil
    }

    implicit def hcons[H, T <: HList](implicit
      headInstance: ConfigMonoid[H],
      tailInstance: Lazy[HListConfigMonoid[T]]
    ): HListConfigMonoid[H :: T] = instance(headInstance.zero :: tailInstance.value.zero) {
      (main, defaults) =>
        val head = headInstance.orElse(main.head, defaults.head)
        val tail = tailInstance.value.orElse(main.tail, defaults.tail)
        head :: tail
    }

  }

  def generic[T, R <: HList](implicit
    gen: Generic.Aux[T, R],
    instance: Lazy[HListConfigMonoid[R]]
  ): ConfigMonoid[T] =
    derive[T, R](gen, instance)

  def derive[T, R <: HList](implicit
    gen: Generic.Aux[T, R],
    reprInstance: Lazy[HListConfigMonoid[R]]
  ): ConfigMonoid[T] = instance(gen.from(reprInstance.value.zero)) {
    (main, defaults) =>
      val r = reprInstance.value.orElse(gen.to(main), gen.to(defaults))
      gen.from(r)
  }

  implicit def option[T]: ConfigMonoid[Option[T]] = instance(Option.empty[T]) {
    (main, defaults) =>
      main.orElse(defaults)
  }

  implicit def seq[T]: ConfigMonoid[Seq[T]] = instance(Seq.empty[T]) {
    (main, defaults) =>
      main ++ defaults
  }

  implicit def list[T]: ConfigMonoid[List[T]] = instance(List.empty[T]) {
    (main, defaults) =>
      main ::: defaults
  }

  implicit def set[T]: ConfigMonoid[Set[T]] = instance(Set.empty[T]) {
    (main, defaults) =>
      main ++ defaults
  }

  implicit val boolean: ConfigMonoid[Boolean] = instance(false) {
    (main, defaults) =>
      main || defaults
  }

}
