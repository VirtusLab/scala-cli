package scala.build.preprocessing.directives

import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ScalaJsOptions}
import scala.build.preprocessing.ScopePath
import scala.build.{Logger, Positioned}

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

  def string(f: String => ScalaJsOptions)(values: Seq[Positioned[String]]): ScalaJsOptions =
    f(values.head.value)

  def boolean(f: Boolean => ScalaJsOptions)(values: Seq[Positioned[String]]): ScalaJsOptions =
    f(values.head.value.toBoolean)

  lazy val directiveMap =
    Map[String, Seq[Positioned[String]] => ScalaJsOptions](
      "jsVersion" ->  string(value => ScalaJsOptions(version = Some(value))),
      "jsMode"    -> string(value => ScalaJsOptions(mode = Some(value))),
      "jsModuleKind" ->
        string(value => ScalaJsOptions(moduleKindStr = Some(value))),
      "jsCheckIr" -> boolean(value => ScalaJsOptions(checkIr = Some(value))),
      "jsEmitSourceMaps" ->
        boolean(value => ScalaJsOptions(emitSourceMaps = value)),
      "jsDom"    -> boolean(value => ScalaJsOptions(dom = Some(value))),
      "jsHeader" -> string(value => ScalaJsOptions(header = Some(value))),
      "jsAllowBigIntsForLongs" ->
        boolean(value => ScalaJsOptions(allowBigIntsForLongs = Some(value))),
      "jsAvoidClasses" ->
        boolean(value => ScalaJsOptions(avoidClasses = Some(value))),
      "jsAvoidLetsAndConsts" ->
        boolean(value => ScalaJsOptions(avoidLetsAndConsts = Some(value))),
      "jsModuleSplitStyleStr" ->
        string(value => ScalaJsOptions(moduleSplitStyleStr = Some(value))),
      "jsEsVersionStr" ->
        string(value => ScalaJsOptions(esVersionStr = Some(value)))
    )

  def keys = directiveMap.keys.toSeq

  override def getSupportedTypes(key: String) = key match {
    case "jsVersion" | "jsHeader" | "jsModuleKind" | "jsMode" | "jsModuleSplitStyleStr" | "jsEsVersionStr" =>
      Set(UsingDirectiveValueKind.STRING)
    case "jsCheckIr" | "jsAllowBigIntsForLongs" | "jsEmitSourceMaps" | "jsDom" | "jsAvoidClasses" | "jsAvoidLetsAndConsts" =>
      Set(UsingDirectiveValueKind.BOOLEAN)
  }

  override def getValueNumberBounds(key: String) = UsingDirectiveValueNumberBounds(1, 1)

  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] =
    checkIfValuesAreExpected(scopedDirective).map { groupedValues =>
      val scalaJsOptions =
        groupedValues.scopedStringValues ++ groupedValues.scopedNumericValues ++ groupedValues.scopedBooleanValues
      val positionedValues = scalaJsOptions.map(_.positioned)
      val buildOptions = directiveMap(scopedDirective.directive.key)(
        positionedValues
      )
      ProcessedDirective(Some(BuildOptions(scalaJsOptions = buildOptions)), Seq.empty)
    }
}