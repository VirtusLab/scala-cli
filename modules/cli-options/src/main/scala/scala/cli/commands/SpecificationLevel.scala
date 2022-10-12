package scala.cli.commands

import caseapp.annotation.Tag
import os.copy

sealed trait SpecificationLevel extends Product with Serializable {
  def md = toString() + " have"
}
object SpecificationLevel {
  case object MUST   extends SpecificationLevel
  case object SHOULD extends SpecificationLevel
  case object IMPLEMENTATION extends SpecificationLevel {
    override def md = toString() + " specific"
  }
  case object RESTRICTED extends SpecificationLevel {
    override def md = "Scala CLI specific"
  }
  case object EXPERIMENTAL extends SpecificationLevel {
    override def md: String = toString()
  }

  val inSpecification = Seq(MUST, SHOULD)
}

object tags {
  val experimental   = SpecificationLevel.EXPERIMENTAL.toString()
  val restricted     = SpecificationLevel.RESTRICTED.toString()
  val implementation = SpecificationLevel.IMPLEMENTATION.toString()
  val must           = SpecificationLevel.MUST.toString()
  val should         = SpecificationLevel.SHOULD.toString()

  def levelFor(name: String) = name match {
    case `experimental`   => Some(SpecificationLevel.EXPERIMENTAL)
    case `restricted`     => Some(SpecificationLevel.RESTRICTED)
    case `implementation` => Some(SpecificationLevel.IMPLEMENTATION)
    case `must`           => Some(SpecificationLevel.MUST)
    case `should`         => Some(SpecificationLevel.SHOULD)
    case _                => None
  }
}
