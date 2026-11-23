package scala.build.internal

import com.eed3si9n.expecty.Expecty.expect

class CoursierUserAgentTests extends munit.FunSuite {
  private val contact = s"+https://github.com/${Constants.ghOrg}/${Constants.ghName}"

  test("user agent names Scala CLI and coursier, with major.minor versions") {
    expect(
      CoursierUserAgent.userAgent("1.19.1", "2.1.26", isCi = false) ==
        s"Scala-CLI/1.19 ($contact) Coursier/2.1"
    )
  }

  test("user agent has a ci comment token on CI") {
    expect(
      CoursierUserAgent.userAgent("1.19.1-SNAPSHOT", "2.1.27", isCi = true) ==
        s"Scala-CLI/1.19 ($contact; ci) Coursier/2.1"
    )
  }

  test("user agent omits versions it can't parse") {
    expect(
      CoursierUserAgent.userAgent("nightly", "2.1.0-M1", isCi = false) ==
        s"Scala-CLI ($contact) Coursier/2.1"
    )
  }

  test("major.minor") {
    expect(CoursierUserAgent.majorMinor("1.19.1-SNAPSHOT").contains("1.19"))
    expect(CoursierUserAgent.majorMinor("2.1-RC1").contains("2.1"))
    expect(CoursierUserAgent.majorMinor("2.1").contains("2.1"))
    expect(CoursierUserAgent.majorMinor("2").isEmpty)
    expect(CoursierUserAgent.majorMinor("v2.1.0").isEmpty)
    expect(CoursierUserAgent.majorMinor("").isEmpty)
  }

  test("default cache sends the Scala CLI user agent") {
    expect(CoursierUserAgent.defaultCache().userAgent.contains(CoursierUserAgent.value))
  }
}
