package scala.build.options

import com.eed3si9n.expecty.Expecty.expect

import scala.build.Logger
import scala.build.errors.UnrecognizedJsEsVersionError
import scala.build.internal.ScalaJsLinkerConfig

class ScalaJsOptionsTests extends munit.FunSuite {

  private def esVersionOf(str: String): Either[UnrecognizedJsEsVersionError, String] =
    ScalaJsOptions(esVersionStr = Some(str)).esVersion

  private def errorFor(str: String): UnrecognizedJsEsVersionError =
    esVersionOf(str) match {
      case Left(e)      => e
      case Right(other) => sys.error(s"Expected an error for '$str', got $other")
    }

  private def linkerArgsFor(str: String): Seq[String] =
    ScalaJsOptions(esVersionStr = Some(str)).linkerConfig(Logger.nop) match {
      case Right(config) => config.linkerCliArgs
      case Left(e)       => sys.error(s"Expected a linker config for '$str', got $e")
    }

  test("no es version yields the default one") {
    expect(ScalaJsOptions().esVersion == Right(ScalaJsLinkerConfig.ESVersion.default))
  }

  test("es5_1 is recognized") {
    expect(esVersionOf("es5_1") == Right(ScalaJsLinkerConfig.ESVersion.ES5_1))
  }

  test("es2022 - es2026 are recognized") {
    expect(esVersionOf("es2022") == Right("ES2022"))
    expect(esVersionOf("es2023") == Right("ES2023"))
    expect(esVersionOf("es2024") == Right("ES2024"))
    expect(esVersionOf("es2025") == Right("ES2025"))
    expect(esVersionOf("es2026") == Right("ES2026"))
  }

  // The point of forwarding rather than enumerating: a version Scala CLI has never heard of is
  // passed on to the Scala.js linker, which is the only component that knows what it supports.
  test("an es version newer than any Scala CLI knows about is forwarded") {
    expect(esVersionOf("es2027") == Right("ES2027"))
    expect(esVersionOf("es2099") == Right("ES2099"))
    expect(linkerArgsFor("es2027").containsSlice(Seq("--esVersion", "ES2027")))
  }

  test("es version input is trimmed and case-insensitive") {
    expect(esVersionOf("  ES2022 ") == Right("ES2022"))
    expect(esVersionOf("Es5_1") == Right(ScalaJsLinkerConfig.ESVersion.ES5_1))
  }

  test("malformed es versions are rejected") {
    for (malformed <- Seq("esnext", "es2O22", "2022", "es22", "es20222", "es", ""))
      expect(esVersionOf(malformed).isLeft)
  }

  test("es versions older than the Scala.js minimum are rejected") {
    expect(esVersionOf("es2014").isLeft)
    expect(esVersionOf("es1999").isLeft)
  }

  test("the rejection message describes the accepted shape") {
    val message = errorFor("esnext").message
    expect(message.contains("Unrecognized Scala.js ECMA Script version: esnext"))
    expect(message.contains("es5_1"))
    expect(message.contains("esYYYY"))
  }

  test("normalizeEsVersion is the shared validation entry point") {
    expect(ScalaJsOptions.normalizeEsVersion("  ES2027 ") == Right("ES2027"))
    expect(ScalaJsOptions.normalizeEsVersion("esnext").isLeft)
    // the instance method must agree with the shared entry point, so the two cannot drift
    for (input <- Seq("es5_1", "es2022", "es2027", "esnext", "es2014", "es2O22"))
      expect(
        ScalaJsOptions(esVersionStr = Some(input)).esVersion.toOption ==
          ScalaJsOptions.normalizeEsVersion(input).toOption
      )
  }

  test("the es version is passed to the linker") {
    expect(linkerArgsFor("es2022").containsSlice(Seq("--esVersion", "ES2022")))
  }
}
