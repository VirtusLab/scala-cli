package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.internal.Constants
import scala.build.options.{BuildOptions, ScalaNativeOptions}
import scala.build.{Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Scala Native options")
@DirectiveExamples(s"//> using nativeGc immix")
@DirectiveExamples(s"//> using nativeMode debug")
@DirectiveExamples(s"//> using nativeLto full")
@DirectiveExamples(s"//> using nativeVersion ${Constants.scalaNativeVersion}")
@DirectiveExamples(s"//> using nativeCompile -flto=thin")
@DirectiveExamples(s"//> using nativeLinking -flto=thin")
@DirectiveExamples(s"//> using nativeClang ./clang")
@DirectiveExamples(s"//> using nativeClangPP ./clang++")
@DirectiveExamples(s"//> using nativeEmbedResources")
@DirectiveExamples(s"//> using nativeEmbedResources true")
@DirectiveExamples(s"//> using nativeTarget library-dynamic")
@DirectiveExamples(s"//> using nativeMultithreading")
@DirectiveExamples(s"//> using nativeMultithreading false")
@DirectiveUsage(
  "//> using nativeGc _value_ | using native-version _value_",
  """`//> using nativeGc` **immix**_|commix|boehm|none_
    |
    |`//> using nativeMode` **debug**_|release-fast|release-size|release-full_
    |
    |`//> using nativeLto` **none**_|full|thin_
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
    |`//> using nativeClangPp` _value_
    |
    |`//> using nativeEmbedResources` _true|false_
    |`//> using nativeEmbedResources`
    |
    |`//> using nativeTarget` _application|library-dynamic|library-static_
    |
    |`//> using nativeMultithreading` _true|false_
    |`//> using nativeMultithreading`
    """.stripMargin.trim
)
@DirectiveDescription("Add Scala Native options")
@DirectiveLevel(SpecificationLevel.SHOULD)
final case class ScalaNative(
  nativeGc: Option[String] = None,
  nativeMode: Option[String] = None,
  nativeLto: Option[String] = None,
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
  def buildOptions: Either[BuildException, BuildOptions] = {
    val nativeOptions = ScalaNativeOptions(
      gcStr = nativeGc,
      modeStr = nativeMode,
      ltoStr = nativeLto,
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
