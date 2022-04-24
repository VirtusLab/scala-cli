package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import scala.build.errors.UsingDirectiveValueNumError
import scala.build.options.{BuildOptions, InternalOptions}
import scala.build.tests.util.BloopServer
import scala.build.{BuildThreads, Directories, LocalRepo}
import scala.build.Position
import scala.build.preprocessing.directives.UsingDirectiveError
import scala.build.preprocessing.directives.ValueType

class ScalaNativeUsingDirectiveTests extends munit.FunSuite {

  val buildThreads = BuildThreads.create()
  def bloopConfig  = Some(BloopServer.bloopConfig)

  val extraRepoTmpDir = os.temp.dir(prefix = "scala-cli-tests-extra-repo-")
  val directories     = Directories.under(extraRepoTmpDir)

  val buildOptions = BuildOptions(
    internal = InternalOptions(
      localRepository = LocalRepo.localRepo(directories.localRepoDir),
      keepDiagnostics = true
    )
  )

  def assertUsingDirectiveError(
    kind: UsingDirectiveError.Kind,
    line: Int = 0
  )(directiveCode: String) = {
    val code = s"""$directiveCode
                  |def foo() = println("hello foo")
                  |""".stripMargin
    TestInputs(os.rel / "p.sc" -> code).withBuild(buildOptions, buildThreads, bloopConfig) {
      (_, _, maybeBuild) =>
        expect(maybeBuild.isLeft)
        maybeBuild match {
          case Left(UsingDirectiveError(msg, pos +: _, reportedKind)) =>
            println(msg)
            assertEquals(reportedKind, kind)
            assertEquals(clue(pos).asInstanceOf[Position.File].startPos._1, line)
            assert(msg.nonEmpty)
          case res =>
            fail(s"Expected error related to using directeives, but got $res")
        }
    }
  }

  test("ScalaNativeOptions for native-gc with no values") {
    assertUsingDirectiveError(UsingDirectiveError.NoValueProvided)(
      """//> using `native-gc` """
    )
  }

  test("ScalaNativeOptions for native-gc with multiple values") {
    assertUsingDirectiveError(UsingDirectiveError.ExpectedSingle)(
      """//> using `native-gc` "none", "boehm" """
    )
  }

  test("ScalaNativeOptions for native-gc with wrong value types") {
    assertUsingDirectiveError(UsingDirectiveError.NotMatching)(
      """//> using `native-gc` 1 """
    )
  }

  test("ScalaNativeOptions for native-mode with no values") {
    assertUsingDirectiveError(UsingDirectiveError.NoValueProvided)(
      """//> using `native-mode` """
    )
  }

  test("ScalaNativeOptions for native-mode with multiple values") {
    assertUsingDirectiveError(UsingDirectiveError.ExpectedSingle)(
      """//> using `native-mode` "none", "boehm" """
    )
  }

  test("ScalaNativeOptions for native-mode with wrong value types") {
    assertUsingDirectiveError(UsingDirectiveError.NotMatching)(
      """//> using `native-mode` 1 """
    )
  }

  test("ScalaNativeOptions for native-version with no values") {
    assertUsingDirectiveError(UsingDirectiveError.NoValueProvided)(
      """//> using `native-version` """
    )
  }

  test("ScalaNativeOptions for native-version with multiple values") {
    assertUsingDirectiveError(UsingDirectiveError.ExpectedSingle)(
      """//> using `native-version` "none", "boehm" """
    )
  }

  test("ScalaNativeOptions for native-version with wrong value types") {
    assertUsingDirectiveError(UsingDirectiveError.NotMatching)(
      """//> using `native-version` 1 """
    )
  }

  test("ScalaNativeOptions for native-clang with no values") {
    assertUsingDirectiveError(UsingDirectiveError.NoValueProvided)(
      """//> using `native-clang` """
    )
  }

  test("ScalaNativeOptions for native-clang with multiple values") {
    assertUsingDirectiveError(UsingDirectiveError.ExpectedSingle)(
      """//> using `native-clang` "none", "boehm" """
    )
  }

  test("ScalaNativeOptions for native-clang with wrong value types") {
    assertUsingDirectiveError(UsingDirectiveError.NotMatching)(
      """//> using `native-clang` 1 """
    )
  }

  test("ScalaNativeOptions for native-clang-pp with no values") {
    assertUsingDirectiveError(UsingDirectiveError.NoValueProvided)(
      """//> using `native-clang-pp` """
    )
  }

  test("ScalaNativeOptions for native-clang-pp with multiple values") {
    assertUsingDirectiveError(UsingDirectiveError.ExpectedSingle)(
      """//> using `native-clang-pp` "none", "boehm" """
    )
  }

  test("ScalaNativeOptions for native-clang-pp with wrong value types") {
    assertUsingDirectiveError(UsingDirectiveError.NotMatching)(
      """//> using `native-clang-pp` 1"""
    )
  }

  test("ScalaNativeOptions for native-compile with no values") {
    assertUsingDirectiveError(UsingDirectiveError.NoValueProvided)(
      """//> using `native-compile`"""
    )
  }

  test("ScalaNativeOptions for native-compile with wrong value types") {
    assertUsingDirectiveError(UsingDirectiveError.NotMatching)(
      """//> using `native-compile` 1"""
    )
  }

  test("ScalaNativeOptions for native-linking with no values") {
    assertUsingDirectiveError(UsingDirectiveError.NoValueProvided)(
      """//> using `native-linking`"""
    )
  }

  test("ScalaNativeOptions for native-linking with wrong value types") {
    assertUsingDirectiveError(UsingDirectiveError.NotMatching)(
      """//> using `native-linking` 1"""
    )
  }
}
