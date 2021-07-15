package scala.build.config.reader

// Originally adapted from https://github.com/pureconfig/pureconfig/blob/v0.16.0/modules/generic/src/main/scala/pureconfig/generic/DerivedConfigReader.scala

import shapeless._

import pureconfig._
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.error.InvalidCoproductOption

trait DerivedConfigReader[A] extends ConfigReader[A] {
  def fields: List[Field]

  lazy val allFields: List[Field] =
    computeAllFields(Set(this))

  private def computeAllFields(ignore: Set[ConfigReader[_]]): List[Field] =
    fields
      .collect {
        case field if !ignore(field.reader()) =>
          (field, field.reader())
      }
      .flatMap {
        case (field, reader: DerivedConfigReader[_]) =>
          reader.computeAllFields(ignore + reader).map { field0 =>
            field0.copy(
              prefix = field.name :: field0.prefix
            )
          }
        case (field, _) =>
          List(field)
      }
}

object DerivedConfigReader {

  def apply[A](implicit reader: DerivedConfigReader[A]): DerivedConfigReader[A] = reader

  implicit def productReader[A, Repr <: HList, DefaultRepr <: HList, Descriptions <: HList](implicit
    gen: LabelledGeneric.Aux[A, Repr],
    default: Default.AsOptions.Aux[A, DefaultRepr],
    descriptions: Annotations.Aux[Description, A, Descriptions],
    cc: Lazy[MapShapedReader.Aux[A, DefaultRepr, Descriptions, Repr]]
  ): DerivedConfigReader[A] =
    new DerivedConfigReader[A] {
      override def from(cur: ConfigCursor): ConfigReader.Result[A] =
        cur.asObjectCursor.flatMap(cc.value.from(_, default(), Set.empty)).map(gen.from)
      def fields = cc.value.fields(descriptions())
    }
}
