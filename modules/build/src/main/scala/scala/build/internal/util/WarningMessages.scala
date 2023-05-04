package scala.build.internal.util

import scala.build.input.ScalaCliInvokeData
import scala.build.internal.Constants
import scala.build.preprocessing.directives.{DirectiveHandler, ScopedDirective}
import scala.cli.commands.{SpecificationLevel, tags}
import scala.cli.config.Key

object WarningMessages {
  private val scalaCliGithubUrl = s"https://github.com/${Constants.ghOrg}/${Constants.ghName}"
  private def experimentalFeatureUsed(featureName: String): String =
    s"""$featureName is experimental.
       |Please bear in mind that non-ideal user experience should be expected.
       |If you encounter any bugs or have feedback to share, make sure to reach out to the maintenance team at $scalaCliGithubUrl""".stripMargin
  def experimentalDirectiveUsed(name: String): String =
    experimentalFeatureUsed(s"The '$name' directive")

  def experimentalSubcommandUsed(name: String): String =
    experimentalFeatureUsed(s"The '$name' sub-command")

  def experimentalOptionUsed(name: String): String =
    experimentalFeatureUsed(s"The '$name' option")

  def experimentalConfigKeyUsed(name: String): String =
    experimentalFeatureUsed(s"The '$name' configuration key")

  def rawValueNotWrittenToPublishFile(
    rawValue: String,
    valueName: String,
    directiveName: String
  ): String =
    s"""The value of $valueName ${Console.BOLD}will not${Console.RESET} be written to a potentially public file!
       |Provide it as an option to the publish subcommand with:
       | $directiveName value:$rawValue
       |""".stripMargin

  private def powerFeatureUsedInSip(
    featureName: String,
    featureType: String,
    specificationLevel: SpecificationLevel
  )(using invokeData: ScalaCliInvokeData): String = {
    val powerType =
      if specificationLevel == SpecificationLevel.EXPERIMENTAL then "experimental" else "restricted"
    s"""The '$featureName' $featureType is $powerType.
       |You can run it with the '--power' flag or turn power mode on globally by running:
       |  ${Console.BOLD}${invokeData.progName} config power true${Console.RESET}.""".stripMargin
  }

  def powerCommandUsedInSip(commandName: String, specificationLevel: SpecificationLevel)(using
    ScalaCliInvokeData
  ): String = powerFeatureUsedInSip(commandName, "sub-command", specificationLevel)

  def powerOptionUsedInSip(optionName: String, specificationLevel: SpecificationLevel)(using
    ScalaCliInvokeData
  ): String =
    powerFeatureUsedInSip(optionName, "option", specificationLevel)

  def powerConfigKeyUsedInSip(key: Key[_])(using ScalaCliInvokeData): String =
    powerFeatureUsedInSip(key.fullName, "configuration key", key.specificationLevel)

  def powerDirectiveUsedInSip(
    directive: ScopedDirective,
    handler: DirectiveHandler[_]
  )(using ScalaCliInvokeData): String =
    powerFeatureUsedInSip(
      directive.directive.toString,
      "directive",
      handler.scalaSpecificationLevel
    )
}
