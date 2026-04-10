package scala.cli.commands

import caseapp.core.app.Command
import caseapp.core.parser.Parser

import scala.build.Logger
import scala.build.input.ScalaCliInvokeData

trait RestrictableCommand[T](implicit myParser: Parser[T]) {
  self: Command[T] =>

  def shouldSuppressExperimentalFeatureWarnings: Boolean
  def shouldSuppressDeprecatedFeatureWarnings: Boolean
  def logger: Logger
  protected def invokeData: ScalaCliInvokeData
  override def parser: Parser[T] =
    RestrictedCommandsParser(
      parser = myParser,
      logger = logger,
      shouldSuppressExperimentalWarnings = shouldSuppressExperimentalFeatureWarnings,
      shouldSuppressDeprecatedWarnings = shouldSuppressDeprecatedFeatureWarnings
    )(using invokeData)

  final def isRestricted: Boolean = scalaSpecificationLevel == SpecificationLevel.RESTRICTED

  final def isExperimental: Boolean = scalaSpecificationLevel == SpecificationLevel.EXPERIMENTAL

  /** Is that command a MUST / SHOULD / NICE TO have for the Scala runner specification? */
  def scalaSpecificationLevel: SpecificationLevel

  /** Override to mark the entire sub-command as deprecated. */
  def deprecationMessage: Option[String] = None

  /** Override to mark specific command name aliases as deprecated. */
  def deprecatedNames: Set[List[String]] = Set.empty

  // To reduce imports...
  protected def SpecificationLevel = scala.cli.commands.SpecificationLevel
}
