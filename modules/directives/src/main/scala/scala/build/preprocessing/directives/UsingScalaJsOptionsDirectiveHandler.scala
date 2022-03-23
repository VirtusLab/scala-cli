package scala.build.preprocessing.directives

import com.softwaremill.quicklens._

import scala.build.errors.BuildException
import scala.build.options.BuildOptions
import scala.build.preprocessing.ScopePath
import scala.build.{Logger, Positioned}
import scala.util.{Success, Try}

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

  def getSingleString(
    param: String,
    value: Seq[Positioned[String]],
    buildOptionLens: PathModify[BuildOptions, Option[String]]
  ): Either[BuildException, BuildOptions] =
    value match {
      case Seq(head) => Right(buildOptionLens.setTo(Some(head.value)))
      case _         => Left(MultiValue(param, value))
    }

  def getBooleanOpt(
    param: String,
    value: Seq[Positioned[String]],
    buildOptionLens: PathModify[BuildOptions, Option[Boolean]]
  ) =
    value.map(v => v.copy(value = Try(v.value.toBoolean))).toSeq match {
      case Seq(Positioned(_, Success(a))) => Right(buildOptionLens.setTo(Some(a)))
      case Seq(_)                         => Left(NotABoolean(param, value.head))
      case _                              => Left(MultiValue(param, value))
    }

  def getBoolean(
    param: String,
    value: Seq[Positioned[String]],
    buildOptionLens: PathModify[BuildOptions, Boolean]
  ) =
    value.map(v => v.copy(value = Try(v.value.toBoolean))).toSeq match {
      case Seq(Positioned(_, Success(a))) => Right(buildOptionLens.setTo(a))
      case Seq(_)                         => Left(NotABoolean(param, value.head))
      case _                              => Left(MultiValue(param, value))
    }

  lazy val directiveMap
    : Map[String, (String, Seq[Positioned[String]]) => Either[BuildException, BuildOptions]] =
    Map(
      "jsVersion" -> { getSingleString(_, _, BuildOptions().modify(_.scalaJsOptions.version)) },
      "jsMode"    -> { getSingleString(_, _, BuildOptions().modify(_.scalaJsOptions.mode)) },
      "jsModuleKind" -> {
        getSingleString(_, _, BuildOptions().modify(_.scalaJsOptions.moduleKindStr))
      },
      "jsCheckIr" -> { getBooleanOpt(_, _, BuildOptions().modify(_.scalaJsOptions.checkIr)) },
      "jsEmitSourceMaps" -> {
        getBoolean(_, _, BuildOptions().modify(_.scalaJsOptions.emitSourceMaps))
      },
      "jsDom"    -> { getBooleanOpt(_, _, BuildOptions().modify(_.scalaJsOptions.dom)) },
      "jsHeader" -> { getSingleString(_, _, BuildOptions().modify(_.scalaJsOptions.header)) },
      "jsAllowBigIntsForLongs" -> {
        getBooleanOpt(_, _, BuildOptions().modify(_.scalaJsOptions.allowBigIntsForLongs))
      },
      "jsAvoidClasses" -> {
        getBooleanOpt(_, _, BuildOptions().modify(_.scalaJsOptions.avoidClasses))
      },
      "jsAvoidLetsAndConsts" -> {
        getBooleanOpt(_, _, BuildOptions().modify(_.scalaJsOptions.avoidLetsAndConsts))
      },
      "jsModuleSplitStyleStr" -> {
        getSingleString(_, _, BuildOptions().modify(_.scalaJsOptions.moduleSplitStyleStr))
      },
      "jsEsVersionStr" -> {
        getSingleString(_, _, BuildOptions().modify(_.scalaJsOptions.esVersionStr))
      }
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
    intermediate.map(bo => ProcessedDirective(Some(bo), Seq.empty))
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
