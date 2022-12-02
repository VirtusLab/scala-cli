package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, JavaOpt, ScalaJsOptions, ShadowingSeq}
import scala.build.{Logger, Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Scala.js options")
@DirectiveExamples("//> using jsModuleKind \"common\"")
@DirectiveUsage(
  "//> using jsVersion|jsMode|jsModuleKind|… _value_",
  """
    |`//> using jsVersion` _value_
    |
    |`//> using jsMode` _value_
    |
    |`//> using jsModuleKind` _value_
    |
    |`//> using jsSmallModuleForPackage` _value1_, _value2_
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
)
@DirectiveDescription("Add Scala.js options")
@DirectiveLevel(SpecificationLevel.SHOULD)
// format: off
final case class ScalaJs(
  jsVersion: Option[String] = None,
  jsMode: Option[String] = None,
  jsModuleKind: Option[String] = None,
  jsCheckIr: Option[Boolean] = None,
  jsEmitSourceMaps: Option[Boolean] = None,
  jsSmallModuleForPackage: List[String] = Nil,
  jsDom: Option[Boolean] = None,
  jsHeader: Option[String] = None,
  jsAllowBigIntsForLongs: Option[Boolean] = None,
  jsAvoidClasses: Option[Boolean] = None,
  jsAvoidLetsAndConsts: Option[Boolean] = None,
  jsModuleSplitStyleStr: Option[String] = None,
  jsEsVersionStr: Option[String] = None
) extends HasBuildOptions {
  // format: on
  def buildOptions: Either[BuildException, BuildOptions] = either {
    val scalaJsOptions = ScalaJsOptions(
      version = jsVersion,
      mode = jsMode,
      moduleKindStr = jsModuleKind,
      checkIr = jsCheckIr,
      emitSourceMaps = jsEmitSourceMaps.getOrElse(ScalaJsOptions().emitSourceMaps),
      smallModuleForPackage = jsSmallModuleForPackage,
      dom = jsDom,
      header = jsHeader,
      allowBigIntsForLongs = jsAllowBigIntsForLongs,
      avoidClasses = jsAvoidClasses,
      avoidLetsAndConsts = jsAvoidLetsAndConsts,
      moduleSplitStyleStr = jsModuleSplitStyleStr,
      esVersionStr = jsEsVersionStr
    )
    BuildOptions(
      scalaJsOptions = scalaJsOptions
    )
  }
}

object ScalaJs {
  val handler: DirectiveHandler[ScalaJs] = DirectiveHandler.derive
}
