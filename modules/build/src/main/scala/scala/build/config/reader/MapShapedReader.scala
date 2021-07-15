package scala.build.config.reader

// Originally adapted from https://github.com/pureconfig/pureconfig/blob/v0.16.0/modules/generic/src/main/scala/pureconfig/generic/MapShapedReader.scala

import shapeless._
import shapeless.labelled.{FieldType, field}

import pureconfig._
import pureconfig.error.KeyNotFound
import pureconfig.generic.ProductHint
import pureconfig.generic.ProductHint.UseOrDefault

sealed abstract class MapShapedReader[Original, -DefaultRepr <: HList, -Descriptions <: HList] {
  type Repr <: HList
  def from(cur: ConfigObjectCursor, default: DefaultRepr, usedFields: Set[String]): ConfigReader.Result[Repr]
  def fields(descriptions: Descriptions): List[Field]
}

object MapShapedReader {

  type Aux[Original, -DefaultRepr <: HList, -Descriptions <: HList, Repr0 <: HList] =
    MapShapedReader[Original, DefaultRepr, Descriptions] { type Repr = Repr0 }

  private def defaultHint[A] = ProductHint[A]()

  implicit def nil[Original]: MapShapedReader.Aux[Original, HNil, HNil, HNil] =
    new MapShapedReader[Original, HNil, HNil] {
      type Repr = HNil
      val hint = defaultHint[Original]
      def from(cur: ConfigObjectCursor, default: HNil, usedFields: Set[String]): ConfigReader.Result[HNil] =
        hint.bottom(cur, usedFields).fold[ConfigReader.Result[HNil]](Right(HNil))(Left.apply)
      def fields(descriptions: HNil) = scala.Nil
    }

  implicit def cons[Original, K <: Symbol, H, T <: HList, D <: HList, TDest <: HList](implicit
    key: Witness.Aux[K],
    hConfigReader: Lazy[ConfigReader[H]],
    typeable: Typeable[H],
    tConfigReader: Lazy[MapShapedReader.Aux[Original, D, TDest, T]]
  ): MapShapedReader.Aux[Original, Option[H] :: D, Option[Description] :: TDest, FieldType[K, H] :: T] =
    new MapShapedReader[Original, Option[H] :: D, Option[Description] :: TDest] {
      type Repr = FieldType[K, H] :: T
      val hint = defaultHint[Original]
      val fieldName = key.value.name
      lazy val reader = hConfigReader.value
      def from(
        cur: ConfigObjectCursor,
        default: Option[H] :: D,
        usedFields: Set[String]
      ): ConfigReader.Result[FieldType[K, H] :: T] = {
        val fieldAction = hint.from(cur, fieldName)
        val headResult = (fieldAction, default.head) match {
          case (UseOrDefault(cursor, _), Some(defaultValue)) if cursor.isUndefined =>
            Right(defaultValue)
          case (action, _) if !action.cursor.isUndefined || reader.isInstanceOf[ReadsMissingKeys] =>
            reader.from(action.cursor)
          case _ =>
            cur.failed[H](KeyNotFound.forKeys(fieldAction.field, cur.keys))
        }
        val tailResult = tConfigReader.value.from(cur, default.tail, usedFields + fieldAction.field)
        ConfigReader.Result.zipWith(headResult, tailResult)((head, tail) => field[K](head) :: tail)
      }
      def fld(description: Option[Description]) = Field(
        Nil,
        fieldName,
        typeable.describe,
        description.map(_.value),
        () => hConfigReader.value
      )
      def fields(descriptions: Option[Description] :: TDest) =
        fld(descriptions.head) :: tConfigReader.value.fields(descriptions.tail)
    }
}
