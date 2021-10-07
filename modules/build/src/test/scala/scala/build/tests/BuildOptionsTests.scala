package scala.build.tests

import com.eed3si9n.expecty.Expecty.{assert => expect}

import scala.build.options.{BuildOptions, BuildRequirements}

class BuildOptionsTests extends munit.FunSuite {

  test("Empty BuildOptions is actually empty") {
    val empty = BuildOptions()
    val zero  = BuildOptions.monoid.zero
    expect(
      empty == zero,
      "Unexpected Option / Seq / Set / Boolean with a non-empty / non-false default value"
    )
  }

  test("Empty BuildRequirements is actually empty") {
    val empty = BuildRequirements()
    val zero  = BuildRequirements.monoid.zero
    expect(
      empty == zero,
      "Unexpected Option / Seq / Set / Boolean with a non-empty / non-false default value"
    )
  }

}
