package scala.build.internal.util

import scala.build.input.ScalaCliInvokeData
import scala.build.internal.Constants
import scala.build.internals.FeatureType
import scala.build.preprocessing.directives.{DirectiveHandler, ScopedDirective}
import scala.cli.commands.{SpecificationLevel, tags}
import scala.cli.config.Key

object WarningMessages {
  private val scalaCliGithubUrl = s"https://github.com/${Constants.ghOrg}/${Constants.ghName}"

  private val experimentalNote =
    s"""Please bear in mind that non-ideal user experience should be expected.
       |If you encounter any bugs or have feedback to share, make sure to reach out to the maintenance team at $scalaCliGithubUrl""".stripMargin
  def experimentalFeaturesUsed(namesAndFeatureTypes: Seq[(String, FeatureType)]): String = {
    val message = namesAndFeatureTypes match {
      case Seq((name, featureType)) => s"The `$name` $featureType is experimental"
      case namesAndTypes =>
        val nl                   = System.lineSeparator()
        val distinctFeatureTypes = namesAndTypes.map(_._2).distinct
        val (bulletPointList, featureNameToPrint) = if (distinctFeatureTypes.size == 1)
          (
            namesAndTypes.map((name, fType) => s" - `$name`")
              .mkString(nl),
            s"${distinctFeatureTypes.head}s" // plural form
          )
        else
          (
            namesAndTypes.map((name, fType) => s" - `$name` $fType")
              .mkString(nl),
            "features"
          )

        s"""Some utilized $featureNameToPrint are marked as experimental:
           |$bulletPointList""".stripMargin
    }
    s"""$message
       |$experimentalNote""".stripMargin
  }

  def experimentalSubcommandWarning(name: String): String =
    s"""The `$name` sub-command is experimental.
       |$experimentalNote""".stripMargin

  def rawValueNotWrittenToPublishFile(
    rawValue: String,
    valueName: String,
    directiveName: String
  ): String =
    s"""The value of $valueName ${Console.BOLD}will not${Console.RESET} be written to a potentially public file!
       |Provide it as an option to the publish subcommand with:
       | $directiveName value:$rawValue
       |""".stripMargin

  /** Using @main is impossible in new [[scala.build.internal.ClassCodeWrapper]] since none of the
    * definitions can be accessed statically, so those errors are swapped with this text
    * @param annotationIgnored
    *   will annotation be ignored (or will compilation fail)
    */
  def mainAnnotationNotSupported(annotationIgnored: Boolean): String =
    val consequencesString = if annotationIgnored then s", it will be ignored" else ""
    s"Annotation @main in .sc scripts is not supported$consequencesString, use .scala format instead"

  private def powerFeatureUsedInSip(
    featureName: String,
    featureType: String,
    specificationLevel: SpecificationLevel
  )(using invokeData: ScalaCliInvokeData): String = {
    val powerType =
      if specificationLevel == SpecificationLevel.EXPERIMENTAL then "experimental" else "restricted"
    s"""The `$featureName` $featureType is $powerType.
       |You can run it with the `--power` flag or turn power mode on globally by running:
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

  val chainingUsingFileDirective: String =
    "Chaining the 'using file' directive is not supported, the source won't be included in the build."

  val offlineModeBloopNotFound =
    "Offline mode is ON and Bloop could not be fetched from the local cache, using scalac as fallback"

  val offlineModeBloopJvmNotFound =
    "Offline mode is ON and a JVM for Bloop could not be fetched from the local cache, using scalac as fallback"

  def directivesInMultipleFilesWarning(
    projectFilePath: String,
    pathsToReport: Iterable[String] = Nil
  ) = {
    val detectedMsg = "Using directives detected in multiple files"
    val recommendedMsg =
      s"It is recommended to keep them centralized in the $projectFilePath file."
    if pathsToReport.isEmpty then
      s"$detectedMsg. $recommendedMsg"
    else
      s"""$detectedMsg:
         |${pathsToReport.mkString("- ", s"${System.lineSeparator}- ", "")}
         |$recommendedMsg
         |""".stripMargin
  }
}
