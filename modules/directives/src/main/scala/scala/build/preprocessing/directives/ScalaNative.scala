package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ScalaNativeOptions}
import scala.build.{Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Scala Native options")
@DirectiveExamples("//> using nativeVersion 0.4.0")
@DirectiveUsage(
  "//> using nativeGc _value_ | using native-version _value_",
  """`//> using nativeGc` _value_
    |
    |`//> using nativeMode` _value_
    |
    |`//> using nativeLto` _value_
    |
    |`//> using nativeTargetTriple` _value_
    |
    |`//> using nativeVersion` _value_
    |
    |`//> using nativeCompile` _value1_ _value2_ …
    |
    |`//> using nativeLinking` _value1_ _value2_ …
    |
    |`//> using nativeClang` _value_
    |
    |`//> using nativeClangPP` _value_
    |
    |`//> using nativeEmbedResources` _true|false_
    |
    |`//> using nativeTarget` _application|library-dynamic|library-static_
    """.stripMargin.trim
)
@DirectiveDescription("Add Scala Native options")
@DirectiveLevel(SpecificationLevel.SHOULD)
// format: off
final case class ScalaNative(
  nativeGc: Option[String] = None,
  nativeMode: Option[String] = None,
  nativeLto: Option[String] = None,
  nativeTargetTriple: Option[String] = None,
  nativeVersion: Option[String] = None,
  nativeCompile: List[String] = Nil,
  nativeLinking: List[String] = Nil,
  nativeClang: Option[String] = None,
  @DirectiveName("nativeClangPp")
    nativeClangPP: Option[String] = None,
  nativeEmbedResources: Option[Boolean] = None,
  nativeTarget: Option[String] = None,
  nativeMultithreading: Option[Boolean] = None
) extends HasBuildOptions {
  // format: on
  def buildOptions: Either[BuildException, BuildOptions] = {
    val nativeOptions = ScalaNativeOptions(
      gcStr = nativeGc,
      modeStr = nativeMode,
      ltoStr = nativeLto,
      targetTripleStr = nativeTargetTriple,
      version = nativeVersion,
      compileOptions = nativeCompile,
      linkingOptions = nativeLinking,
      clang = nativeClang,
      clangpp = nativeClangPP,
      embedResources = nativeEmbedResources,
      buildTargetStr = nativeTarget,
      multithreading = nativeMultithreading
    )
    val buildOpt = BuildOptions(scalaNativeOptions = nativeOptions)
    Right(buildOpt)
  }
}

object ScalaNative {
  val handler: DirectiveHandler[ScalaNative] = DirectiveHandler.derive
}
