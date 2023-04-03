package scala.build.internal.util

import scala.build.internal.Constants

object WarningMessages {
  private val scalaCliGithubUrl = s"https://github.com/${Constants.ghOrg}/${Constants.ghName}"
  def experimentalFeatureUsed(featureName: String): String =
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
}
