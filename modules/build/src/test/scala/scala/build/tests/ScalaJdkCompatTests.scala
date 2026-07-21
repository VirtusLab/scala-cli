package scala.build.tests

import com.eed3si9n.expecty.Expecty.assert as expect

import scala.build.internal.ScalaJdkCompat

class ScalaJdkCompatTests extends munit.FunSuite {

  test("normalizeScalaVersion strips suffixes") {
    expect(ScalaJdkCompat.normalizeScalaVersion("3.7.4-RC1") == "3.7.4")
    expect(ScalaJdkCompat.normalizeScalaVersion("3.8.3-nightly-20250101") == "3.8.3")
    expect(ScalaJdkCompat.normalizeScalaVersion("2.13.18-bin-abcd") == "2.13.18")
    expect(ScalaJdkCompat.normalizeScalaVersion("3.3.7") == "3.3.7")
  }

  test("Scala 3.8+ requires JDK 17") {
    val compat = ScalaJdkCompat.forScalaVersion("3.8.3").get
    expect(compat.minJdk == 17)
    expect(compat.maxRecommendedJdk == 26)
    expect(ScalaJdkCompat.forScalaVersion("3.8.0-RC2").get.minJdk == 17)
  }

  test("Scala 3.7.4 supports JDK 8-25") {
    val compat = ScalaJdkCompat.forScalaVersion("3.7.4").get
    expect(compat.minJdk == 8)
    expect(compat.maxRecommendedJdk == 25)
    expect(ScalaJdkCompat.forScalaVersion("3.7.4-RC1").get == compat)
  }

  test("Scala 3.7.0 supports JDK 8-21") {
    val compat = ScalaJdkCompat.forScalaVersion("3.7.0").get
    expect(compat.maxRecommendedJdk == 21)
  }

  test("Scala 3.3 LTS patch-dependent max JDK") {
    expect(ScalaJdkCompat.forScalaVersion("3.3.0").get.maxRecommendedJdk == 17)
    expect(ScalaJdkCompat.forScalaVersion("3.3.1").get.maxRecommendedJdk == 21)
    expect(ScalaJdkCompat.forScalaVersion("3.3.7").get.maxRecommendedJdk == 25)
    expect(ScalaJdkCompat.forScalaVersion("3.3.8").get.maxRecommendedJdk == 26)
  }

  test("Scala 3.4-3.6 supports JDK 8-21") {
    expect(ScalaJdkCompat.forScalaVersion("3.4.0").get.maxRecommendedJdk == 21)
    expect(ScalaJdkCompat.forScalaVersion("3.6.4").get.maxRecommendedJdk == 21)
  }

  test("Scala 2.13 patch-dependent max JDK") {
    expect(ScalaJdkCompat.forScalaVersion("2.13.5").get.maxRecommendedJdk == 11)
    expect(ScalaJdkCompat.forScalaVersion("2.13.10").get.maxRecommendedJdk == 17)
    expect(ScalaJdkCompat.forScalaVersion("2.13.17").get.maxRecommendedJdk == 25)
    expect(ScalaJdkCompat.forScalaVersion("2.13.18").get.maxRecommendedJdk == 26)
  }

  test("Scala 2.12 patch-dependent max JDK") {
    expect(ScalaJdkCompat.forScalaVersion("2.12.3").get.maxRecommendedJdk == 8)
    expect(ScalaJdkCompat.forScalaVersion("2.12.18").get.maxRecommendedJdk == 21)
    expect(ScalaJdkCompat.forScalaVersion("2.12.21").get.maxRecommendedJdk == 26)
  }

  test("unknown Scala version returns None") {
    expect(ScalaJdkCompat.forScalaVersion("4.0.0").isEmpty)
    expect(ScalaJdkCompat.forScalaVersion("not-a-version").isEmpty)
  }
}
