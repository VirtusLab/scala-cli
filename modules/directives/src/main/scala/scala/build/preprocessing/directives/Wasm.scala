package scala.build.preprocessing.directives

import scala.build.Positioned
import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, Platform, ScalaOptions, WasmOptions, WasmRuntime}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("WASM options")
@DirectiveExamples("//> using wasm")
@DirectiveExamples("//> using wasmRuntime node")
@DirectiveExamples("//> using wasmRuntime deno")
@DirectiveExamples("//> using denoVersion 2.1.4")
@DirectiveUsage(
  "//> using wasm|wasmRuntime|denoVersion _value_",
  """
    |`//> using wasm` _true|false_
    |
    |`//> using wasm`
    |
    |`//> using wasmRuntime` _node|deno|wasmtime|wasmedge|wasmer_
    |
    |`//> using denoVersion` _value_
    |""".stripMargin
)
@DirectiveDescription("Add WebAssembly options")
@DirectiveLevel(SpecificationLevel.EXPERIMENTAL)
final case class Wasm(
  wasm: Option[Boolean] = None,
  wasmRuntime: Option[String] = None,
  denoVersion: Option[String] = None
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] = {
    val parsedRuntime = wasmRuntime.flatMap(WasmRuntime.parse)
    val wasmOptions   = WasmOptions(
      enabled = wasm.getOrElse(false),
      runtime = parsedRuntime.getOrElse(WasmRuntime.default),
      denoVersion = denoVersion
    )
    // When WASM is enabled, force Platform.JS (Scala.js WASM backend requires JS compilation)
    val scalaOptions =
      if (wasm.getOrElse(false))
        ScalaOptions(platform = Some(Positioned.none(Platform.JS)))
      else
        ScalaOptions()
    Right(BuildOptions(scalaOptions = scalaOptions, wasmOptions = wasmOptions))
  }
}

object Wasm {
  val handler: DirectiveHandler[Wasm] = DirectiveHandler.derive
}
