package scala.build.options

import com.eed3si9n.expecty.Expecty.expect

import scala.build.Logger
import scala.build.errors.UnrecognizedJsEsVersionError
import scala.build.internal.ScalaJsLinkerConfig

class ScalaJsOptionsTests extends munit.FunSuite {

  private def esVersionOf(str: String): Either[UnrecognizedJsEsVersionError, String] =
    ScalaJsOptions(esVersionStr = Some(str)).esVersion

  test("no es version yields the default one") {
    expect(ScalaJsOptions().esVersion == Right(ScalaJsLinkerConfig.ESVersion.default))
  }

  test("every supported es version is recognized") {
    for (esVersion <- ScalaJsLinkerConfig.ESVersion.all)
      expect(esVersionOf(esVersion.toLowerCase) == Right(esVersion))
  }

  test("es2022 - es2026 are recognized") {
    expect(esVersionOf("es2022") == Right("ES2022"))
    expect(esVersionOf("es2023") == Right("ES2023"))
    expect(esVersionOf("es2024") == Right("ES2024"))
    expect(esVersionOf("es2025") == Right("ES2025"))
    expect(esVersionOf("es2026") == Right("ES2026"))
  }

  test("es version input is trimmed and case-insensitive") {
    expect(esVersionOf("  ES2022 ") == Right("ES2022"))
  }

  test("an unrecognized es version is an error listing the supported ones") {
    val error = esVersionOf("es9999") match {
      case Left(e)      => e
      case Right(other) => sys.error(s"Expected an error, got $other")
    }
    expect(error.message.contains("Unrecognized Scala.js ECMA Script version: es9999"))
    for (esVersion <- ScalaJsOptions.supportedEsVersions)
      expect(error.message.contains(esVersion))
  }

  test("the es version is passed to the linker") {
    val linkerConfig =
      ScalaJsOptions(esVersionStr = Some("es2022")).linkerConfig(Logger.nop) match {
        case Right(config) => config
        case Left(e)       => sys.error(s"Expected a linker config, got $e")
      }
    expect(linkerConfig.linkerCliArgs.containsSlice(Seq("--esVersion", "ES2022")))
  }
}
