package scala.build.preprocessing.directives

import scala.build.Positioned
import scala.build.directives.*
import scala.build.errors.{BuildException, UnrecognizedWasmRuntimeError}
import scala.build.options.{BuildOptions, Platform, ScalaOptions, WasmOptions, WasmRuntime}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("WASM options")
@DirectiveExamples("//> using wasm")
@DirectiveExamples("//> using wasmRuntime node")
@DirectiveExamples("//> using wasmRuntime deno")
@DirectiveExamples("//> using wasmRuntime bun")
@DirectiveUsage(
  "//> using wasm|wasmRuntime _value_",
  """
    |`//> using wasm` _true|false_
    |
    |`//> using wasm`
    |
    |`//> using wasmRuntime` _node|deno|bun_
    |""".stripMargin
)
@DirectiveDescription("Add WebAssembly options")
@DirectiveLevel(SpecificationLevel.EXPERIMENTAL)
final case class Wasm(
  wasm: Option[Boolean] = None,
  wasmRuntime: Option[String] = None
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] = {
    val parsedRuntime =
      wasmRuntime.fold(Right(WasmRuntime.default): Either[BuildException, WasmRuntime]) { rt =>
        WasmRuntime.parse(rt).toRight {
          val validValues = WasmRuntime.all.map(_.name).mkString(", ")
          new UnrecognizedWasmRuntimeError(rt, validValues)
        }
      }
    parsedRuntime.map { runtime =>
      val wasmEnabled = wasm.getOrElse(false) || wasmRuntime.isDefined
      val wasmOptions = WasmOptions(
        enabled = wasmEnabled,
        runtime = runtime
      )
      // When WASM is enabled, force Platform.JS (Scala.js WASM backend requires JS compilation)
      val scalaOptions =
        if (wasmEnabled)
          ScalaOptions(platform = Some(Positioned.none(Platform.JS)))
        else
          ScalaOptions()
      BuildOptions(scalaOptions = scalaOptions, wasmOptions = wasmOptions)
    }
  }
}

object Wasm {
  val handler: DirectiveHandler[Wasm] = DirectiveHandler.derive
}
