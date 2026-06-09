package scala.build.preprocessing.directives

import scala.build.Positioned
import scala.build.directives.*
import scala.build.errors.{BuildException, UnrecognizedJSRuntimeError}
import scala.build.options.{BuildOptions, JSRuntime, Platform, ScalaJsOptions, ScalaOptions}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Wasm options")
@DirectiveExamples("//> using wasm")
@DirectiveExamples("//> using jsRuntime node")
@DirectiveExamples("//> using jsRuntime deno")
@DirectiveExamples("//> using jsRuntime bun")
@DirectiveUsage(
  "//> using wasm|jsRuntime _value_",
  """
    |`//> using wasm` _true|false_
    |
    |`//> using wasm`
    |
    |`//> using jsRuntime` _node|deno|bun_
    |""".stripMargin
)
@DirectiveDescription("Add WebAssembly options")
@DirectiveLevel(SpecificationLevel.EXPERIMENTAL)
final case class Wasm(
  wasm: Option[Boolean] = None,
  jsRuntime: Option[String] = None
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] = {
    val parsedRuntime =
      jsRuntime.fold(Right(JSRuntime.default): Either[BuildException, JSRuntime]) { rt =>
        JSRuntime.parse(rt).toRight {
          val validValues = JSRuntime.all.map(_.name).mkString(", ")
          new UnrecognizedJSRuntimeError(rt, validValues)
        }
      }
    parsedRuntime.map { runtime =>
      // Selecting a JS runtime (//> using jsRuntime) does not by itself enable Wasm.
      val wasmEnabled = wasm.getOrElse(false)
      // Scala.js Wasm backend requires JS platform. When --platform native is also
      // specified alongside --wasm, an AmbiguousPlatformError is raised in platform resolution.
      val scalaOptions =
        if (wasmEnabled)
          ScalaOptions(platform = Some(Positioned.none(Platform.JS)))
        else
          ScalaOptions()
      BuildOptions(
        scalaOptions = scalaOptions,
        scalaJsOptions = ScalaJsOptions(jsEmitWasm = wasmEnabled, jsRuntime = runtime)
      )
    }
  }
}

object Wasm {
  val handler: DirectiveHandler[Wasm] = DirectiveHandler.derive
}
