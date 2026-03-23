package scala.build.tests

import com.eed3si9n.expecty.Expecty.assert as expect

import scala.build.options.*

class JavaTestRunnerTests extends TestUtil.ScalaCliBuildSuite {

  private def makeOptions(
    scalaVersionOpt: Option[MaybeScalaVersion],
    addTestRunner: Boolean
  ): BuildOptions =
    BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = scalaVersionOpt
      ),
      internalDependencies = InternalDependenciesOptions(
        addTestRunnerDependencyOpt = Some(addTestRunner)
      )
    )

  test("pure Java build has no scalaParams") {
    val opts   = makeOptions(Some(MaybeScalaVersion.none), addTestRunner = false)
    val params = opts.scalaParams.toOption.flatten
    expect(params.isEmpty, "Pure Java build should have no scalaParams")
  }

  test("Scala build has scalaParams") {
    val opts   = makeOptions(None, addTestRunner = false)
    val params = opts.scalaParams.toOption.flatten
    expect(params.isDefined, "Scala build should have scalaParams")
  }

  test("pure Java test build gets addJvmJavaTestRunner=true in Artifacts params") {
    val opts   = makeOptions(Some(MaybeScalaVersion.none), addTestRunner = true)
    val isJava = opts.scalaParams.toOption.flatten.isEmpty
    expect(isJava, "Expected pure Java build to have no scalaParams")
  }

  test("Scala test build gets addJvmTestRunner=true in Artifacts params") {
    val opts   = makeOptions(None, addTestRunner = true)
    val isJava = opts.scalaParams.toOption.flatten.isEmpty
    expect(!isJava, "Expected Scala build to have scalaParams")
  }

  test("mixed Scala+Java build still gets Scala test runner") {
    val opts   = makeOptions(None, addTestRunner = true)
    val isJava = opts.scalaParams.toOption.flatten.isEmpty
    expect(!isJava, "Mixed Scala+Java build should still use Scala test runner")
  }
}
