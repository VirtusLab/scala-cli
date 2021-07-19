package scala.build.options

import shapeless._
import shapeless.labelled.FieldType

trait HasHashData[T] {
  def add(prefix: String, t: T, update: String => Unit): Unit
}

object HasHashData {
  def apply[T](implicit instance: HasHashData[T]): HasHashData[T] = instance

  def nop[T]: HasHashData[T] = (_, _, _) => ()

  implicit val hnil: HasHashData[HNil] = nop

  implicit def hcons[K <: Symbol, H, T <: HList](implicit
    fieldName: Witness.Aux[K],
    headInstance: Lazy[HashedField[H]],
    tailInstance: Lazy[HasHashData[T]]
  ): HasHashData[FieldType[K, H] :: T] = {
    (prefix, l, update) =>
      val name = prefix + fieldName.value.name
      headInstance.value.add(name, l.head, update)
      tailInstance.value.add(prefix, l.tail, update)
  }

  implicit def generic[T, R <: HList](implicit
    gen: LabelledGeneric.Aux[T, R],
    instance: Lazy[HasHashData[R]]
  ): HasHashData[T] =
    derive[T, R](gen, instance)

  def derive[T, R <: HList](implicit
    gen: LabelledGeneric.Aux[T, R],
    instance: Lazy[HasHashData[R]]
  ): HasHashData[T] = {
    (prefix, t, update) =>
      instance.value.add(prefix, gen.to(t), update)
  }

}
