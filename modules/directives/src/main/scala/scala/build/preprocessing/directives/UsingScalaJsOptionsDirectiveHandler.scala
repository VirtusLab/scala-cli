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

  def string(
    param: String,
    values: Seq[Positioned[String]],
    f: String => ScalaJsOptions
  ): Either[BuildException, ScalaJsOptions] =
    values match {
      case Seq(elem) => Right(f(elem.value))
      case _         => Left(MultiValue(param, values))
    }

  def boolean(
    param: String,
    values: Seq[Positioned[String]],
    f: Boolean => ScalaJsOptions
  ): Either[BuildException, ScalaJsOptions] =
    values match {
      case Seq(elem) =>
        elem.value.toBooleanOption match {
          case Some(a) => Right(f(a))
          case None    => Left(NotABoolean(param, elem))
        }
      case _ => Left(MultiValue(param, values))
    }

  lazy val directiveMap =
    Map[String, (String, Seq[Positioned[String]]) => Either[BuildException, ScalaJsOptions]](
      "jsVersion" -> ((p, v) => string(p, v, value => ScalaJsOptions(version = Some(value)))),
      "jsMode"    -> ((p, v) => string(p, v, value => ScalaJsOptions(mode = Some(value)))),
      "jsModuleKind" ->
        ((p, v) => string(p, v, value => ScalaJsOptions(moduleKindStr = Some(value)))),
      "jsCheckIr" -> ((p, v) => boolean(p, v, value => ScalaJsOptions(checkIr = Some(value)))),
      "jsEmitSourceMaps" ->
        ((p, v) => boolean(p, v, value => ScalaJsOptions(emitSourceMaps = value))),
      "jsDom"    -> ((p, v) => boolean(p, v, value => ScalaJsOptions(dom = Some(value)))),
      "jsHeader" -> ((p, v) => string(p, v, value => ScalaJsOptions(header = Some(value)))),
      "jsAllowBigIntsForLongs" ->
        ((p, v) => boolean(p, v, value => ScalaJsOptions(allowBigIntsForLongs = Some(value)))),
      "jsAvoidClasses" ->
        ((p, v) => boolean(p, v, value => ScalaJsOptions(avoidClasses = Some(value)))),
      "jsAvoidLetsAndConsts" ->
        ((p, v) => boolean(p, v, value => ScalaJsOptions(avoidLetsAndConsts = Some(value)))),
      "jsModuleSplitStyleStr" ->
        ((p, v) => string(p, v, value => ScalaJsOptions(moduleSplitStyleStr = Some(value)))),
      "jsEsVersionStr" ->
        ((p, v) => string(p, v, value => ScalaJsOptions(esVersionStr = Some(value))))
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
      ProcessedDirective(Some(buildOptions), Seq.empty)
    }
}