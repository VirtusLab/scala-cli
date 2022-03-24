package scala.build.preprocessing.directives

import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ScalaJsOptions}
import scala.build.preprocessing.ScopePath
import scala.build.{Logger, Positioned}

case object UsingScalaJsOptionsDirectiveHandler extends UsingDirectiveHandler {

  def name: String = "Scala JS options"

  def description: String = "Add Scala JS options"

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

  def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] = {
    val scalaJsOptions   = DirectiveUtil.stringValues(directive.values, path, cwd)
    val positionedValues = scalaJsOptions.map(_._1)
    val intermediate     = directiveMap(directive.key)(directive.key, positionedValues)
    intermediate.map(opts =>
      ProcessedDirective(Some(BuildOptions(scalaJsOptions = opts)), Seq.empty)
    )
  }
}

final case class MultiValue(param: String, values: Seq[Positioned[String]]) extends BuildException(
      s"Expected single value for $param but found $values",
      values.headOption.flatMap(_.positions.headOption).toSeq
    )

final case class NotABoolean(param: String, value: Positioned[String]) extends BuildException(
      s"Boolean expected for $param but ${value.value} found",
      value.positions
    )
