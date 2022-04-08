package scala.build.preprocessing.directives

import shapeless.{Lens, lens}

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

  def passStringToBuildOptionsLens(
                                    value: Seq[Positioned[String]],
                                    buildOptionLens: Lens[BuildOptions, Option[String]]
                                  ): BuildOptions =
    buildOptionLens.set(BuildOptions())(Some(value.head.value))

  def passBooleanOptionToBuildOptionLens(
                                          value: Seq[Positioned[String]],
                                          buildOptionLens: Lens[BuildOptions, Option[Boolean]]
                                        ): BuildOptions =
    buildOptionLens.set(BuildOptions())(Some(value.head.value.toBoolean))

  def passBooleanToBuildOptionsLens(
                                     value: Seq[Positioned[String]],
                                     buildOptionLens: Lens[BuildOptions, Boolean]
                                   ): BuildOptions =
    buildOptionLens.set(BuildOptions())(value.head.value.toBoolean)

  lazy val directiveMap: Map[String, Seq[Positioned[String]] => BuildOptions] = {
    val l = lens[BuildOptions].scalaJsOptions
    Map(
      "jsVersion"              -> { passStringToBuildOptionsLens(_, l.version) },
      "jsMode"                 -> { passStringToBuildOptionsLens(_, l.mode) },
      "jsModuleKind"           -> { passStringToBuildOptionsLens(_, l.moduleKindStr) },
      "jsCheckIr"              -> { passBooleanOptionToBuildOptionLens(_, l.checkIr) },
      "jsEmitSourceMaps"       -> { passBooleanToBuildOptionsLens(_, l.emitSourceMaps) },
      "jsDom"                  -> { passBooleanOptionToBuildOptionLens(_, l.dom) },
      "jsHeader"               -> { passStringToBuildOptionsLens(_, l.header) },
      "jsAllowBigIntsForLongs" -> { passBooleanOptionToBuildOptionLens(_, l.allowBigIntsForLongs) },
      "jsAvoidClasses"         -> { passBooleanOptionToBuildOptionLens(_, l.avoidClasses) },
      "jsAvoidLetsAndConsts"   -> { passBooleanOptionToBuildOptionLens(_, l.avoidLetsAndConsts) },
      "jsModuleSplitStyleStr"  -> { passStringToBuildOptionsLens(_, l.moduleSplitStyleStr) },
      "jsEsVersionStr"         -> { passStringToBuildOptionsLens(_, l.esVersionStr) }
    )
  }

  def keys = directiveMap.keys.toSeq

  override def getSupportedTypes(key: String) = key match {
    case "jsVersion" | "jsHeader" | "jsModuleKind" | "jsMode" | "jsModuleSplitStyleStr" | "jsEsVersionStr" =>
      Set(UsingDirectiveValueKind.STRING)
    case "jsCheckIr" | "jsAllowBigIntsForLongs" | "jsEmitSourceMaps" | "jsDom" | "jsAvoidClasses" | "jsAvoidLetsAndConsts" =>
      Set(UsingDirectiveValueKind.BOOLEAN, UsingDirectiveValueKind.EMPTY)
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