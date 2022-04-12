package scala.build.preprocessing.directives

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ScalaJsOptions}

case object UsingScalaJsOptionsDirectiveHandler extends UsingDirectiveHandler {

  def name: String = "Scala.js options"

  def description: String = "Add Scala.js options"

  def usage: String = s"//> using ${keys.mkString("|")} _value_"

  override def usageMd: String =
    """
      |`//> using jsVersion` _value_
      |
      |`//> using jsMode` _value_
      |
      |`//> using jsModuleKind` _value_
      |
      |`//> using jsCheckIr` _true|false_
      |
      |`//> using jsEmitSourceMaps` _true|false_
      |
      |`//> using jsDom` _true|false_
      |
      |`//> using jsHeader` _value_
      |
      |`//> using jsAllowBigIntsForLongs` _true|false_
      |
      |`//> using jsAvoidClasses` _true|false_
      |
      |`//> using jsAvoidLetsAndConsts` _true|false_
      |
      |`//> using jsModuleSplitStyleStr` _value_
      |
      |`//> using jsEsVersionStr` _value_
      |""".stripMargin

  override def examples: Seq[String] = Seq(
    "//> using jsModuleKind \"common\""
  )

  override def keys: Seq[String] =
    Seq(
      "jsVersion",
      "jsMode",
      "jsModuleKind",
      "jsCheckIr",
      "jsEmitSourceMaps",
      "jsDom",
      "jsHeader",
      "jsAllowBigIntsForLongs",
      "jsAvoidClasses",
      "jsAvoidLetsAndConsts",
      "jsModuleSplitStyleStr",
      "jsEsVersionStr"
    )

  def getBooleanOption(groupedValues: GroupedScopedValuesContainer): Option[Boolean] =
    groupedValues.scopedBooleanValues.map(_.positioned.value.toBoolean).headOption.orElse(Some(
      true
    ))

  def getBooleanValue(groupedValues: GroupedScopedValuesContainer): Boolean =
    groupedValues.scopedBooleanValues.map(_.positioned.value.toBoolean).headOption.getOrElse(true)

  def getStringOption(groupedValues: GroupedScopedValuesContainer): Option[String] =
    groupedValues.scopedStringValues.headOption.map(_.positioned.value)

  override def getSupportedTypes(key: String) = key match {
    case "jsVersion" | "jsHeader" | "jsModuleKind" | "jsMode" | "jsModuleSplitStyleStr" | "jsEsVersionStr" =>
      Set(UsingDirectiveValueKind.STRING)
    case "jsCheckIr" | "jsAllowBigIntsForLongs" | "jsEmitSourceMaps" | "jsDom" | "jsAvoidClasses" | "jsAvoidLetsAndConsts" =>
      Set(UsingDirectiveValueKind.BOOLEAN, UsingDirectiveValueKind.EMPTY)
  }

  override def getValueNumberBounds(key: String) = key match {
    case "jsVersion" | "jsHeader" | "jsModuleKind" | "jsMode" | "jsModuleSplitStyleStr" | "jsEsVersionStr" =>
      UsingDirectiveValueNumberBounds(1, 1)
    case "jsCheckIr" | "jsAllowBigIntsForLongs" | "jsEmitSourceMaps" | "jsDom" | "jsAvoidClasses" | "jsAvoidLetsAndConsts" =>
      UsingDirectiveValueNumberBounds(0, 1)
  }

  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] =
    checkIfValuesAreExpected(scopedDirective).map { groupedValues =>
      val buildOptions = scopedDirective.directive.key match {
        case "jsVersion" =>
          BuildOptions(scalaJsOptions = ScalaJsOptions(version = getStringOption(groupedValues)))
        case "jsMode" =>
          BuildOptions(scalaJsOptions = ScalaJsOptions(mode = getStringOption(groupedValues)))
        case "jsModuleKind" => BuildOptions(scalaJsOptions =
            ScalaJsOptions(moduleKindStr = getStringOption(groupedValues))
          )
        case "jsCheckIr" =>
          BuildOptions(scalaJsOptions = ScalaJsOptions(checkIr = getBooleanOption(groupedValues)))
        case "jsEmitSourceMaps" => BuildOptions(scalaJsOptions =
            ScalaJsOptions(emitSourceMaps = getBooleanValue(groupedValues))
          )
        case "jsDom" =>
          BuildOptions(scalaJsOptions = ScalaJsOptions(dom = getBooleanOption(groupedValues)))
        case "jsHeader" =>
          BuildOptions(scalaJsOptions = ScalaJsOptions(header = getStringOption(groupedValues)))
        case "jsAllowBigIntsForLongs" => BuildOptions(scalaJsOptions =
            ScalaJsOptions(allowBigIntsForLongs = getBooleanOption(groupedValues))
          )
        case "jsAvoidClasses" => BuildOptions(scalaJsOptions =
            ScalaJsOptions(avoidClasses = getBooleanOption(groupedValues))
          )
        case "jsAvoidLetsAndConsts" => BuildOptions(scalaJsOptions =
            ScalaJsOptions(avoidLetsAndConsts = getBooleanOption(groupedValues))
          )
        case "jsModuleSplitStyleStr" => BuildOptions(scalaJsOptions =
            ScalaJsOptions(moduleSplitStyleStr = getStringOption(groupedValues))
          )
        case "jsEsVersionStr" => BuildOptions(scalaJsOptions =
            ScalaJsOptions(esVersionStr = getStringOption(groupedValues))
          )

      }
      ProcessedDirective(Some(buildOptions), Seq.empty)
    }
}
