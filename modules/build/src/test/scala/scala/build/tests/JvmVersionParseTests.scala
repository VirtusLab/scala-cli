package scala.build.tests

import com.eed3si9n.expecty.Expecty.{assert => expect}
import scala.util.Try
import scala.build.options.{BuildOptions, BuildRequirements}

import scala.build.options.ParseJavaVersion

class JvmVersionParseTests extends munit.FunSuite {

  private implicit class Parse(s: String) {
    implicit def p() = ParseJavaVersion.parse(s)
  }

  test("parse jvm version") {
    expect("""openjdk version "1.8.0_292" """.p == Some(8))
    expect("""openjdk version "9" """.p == Some(9))
    expect("""openjdk version "11.0.11" 2021-04-20 """.p == Some(11))
    expect("""openjdk version "16" 2021-03-16 """.p == Some(16))
  }
}
