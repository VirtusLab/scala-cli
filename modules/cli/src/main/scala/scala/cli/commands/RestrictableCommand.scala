package scala.cli.commands

import caseapp.core.app.Command
import caseapp.core.parser.Parser

trait RestrictableCommand[T](implicit myParser: Parser[T]) {
  self: Command[T] =>

  def shouldSuppressExperimentalFeatureWarnings: Boolean
  override def parser: Parser[T] =
    RestrictedCommandsParser(myParser, shouldSuppressExperimentalFeatureWarnings)

  final def isRestricted: Boolean = scalaSpecificationLevel == SpecificationLevel.RESTRICTED

  final def isExperimental: Boolean = scalaSpecificationLevel == SpecificationLevel.EXPERIMENTAL

  /** Is that command a MUST / SHOULD / NICE TO have for the Scala runner specification? */
  def scalaSpecificationLevel: SpecificationLevel
  // To reduce imports...
  protected def SpecificationLevel = scala.cli.commands.SpecificationLevel
}
