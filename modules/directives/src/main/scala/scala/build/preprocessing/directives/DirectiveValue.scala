package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.*

sealed trait DirectiveValue[Out] {
  def extract(v: Value[_]): Option[Out]
  def name: String
}

object DirectiveValue {
  case object String extends DirectiveValue[String] {
    def extract(v: Value[_]): Option[String] = v match {
      case s: StringValue => Some(s.get())
      case _              => None
    }
    def name = "String literal"
  }
  case object Boolean extends DirectiveValue[Boolean] {
    def extract(v: Value[_]): Option[Boolean] = v match {
      case s: BooleanValue => Some(s.get())
      case _: EmptyValue   => Some(true) // TODO it that ok?
      case _               => None
    }
    def name = "String literal"
  }
  case object Number extends DirectiveValue[String] {
    def extract(v: Value[_]): Option[String] = v match {
      case n: NumericValue => Some(n.get())
      case _               => None
    }

    def name = "Number literal"
  }
}
