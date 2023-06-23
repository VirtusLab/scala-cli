package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import java.io.IOException
import scala.build.Ops.EitherThrowOps
import scala.build.errors.ToolkitDirectiveMissingVersionError
import scala.build.options.{
  BuildOptions,
  InternalOptions,
  MaybeScalaVersion,
  Platform,
  ScalaOptions,
  ScalacOpt,
  Scope,
  ScriptOptions
}
import scala.build.tests.util.BloopServer
import scala.build.{Build, BuildThreads, Directories, LocalRepo, Position, Positioned}

class ScriptWrapperTests extends munit.FunSuite {

  def expectObjectWrapper(wrapperName: String, path: os.Path) = {
    val generatedFileContent = os.read(path)
    assert(
      generatedFileContent.contains(s"object $wrapperName {"),
      clue(s"Generated file content: $generatedFileContent")
    )
    assert(
      !generatedFileContent.contains(s"final class $wrapperName$$_"),
      clue(s"Generated file content: $generatedFileContent")
    )
  }

  def expectClassWrapper(wrapperName: String, path: os.Path) = {
    val generatedFileContent = os.read(path)
    assert(
      generatedFileContent.contains(s"final class $wrapperName$$_"),
      clue(s"Generated file content: $generatedFileContent")
    )
    assert(
      !generatedFileContent.contains(s"object $wrapperName {"),
      clue(s"Generated file content: $generatedFileContent")
    )
  }

  val buildThreads = BuildThreads.create()

  def bloopConfigOpt = Some(BloopServer.bloopConfig)

  val extraRepoTmpDir = os.temp.dir(prefix = "scala-cli-tests-extra-repo-")
  val directories     = Directories.under(extraRepoTmpDir)

  override def afterAll(): Unit = {
    TestInputs.tryRemoveAll(extraRepoTmpDir)
    buildThreads.shutdown()
  }

  val baseOptions = BuildOptions(
    internal = InternalOptions(
      localRepository = LocalRepo.localRepo(directories.localRepoDir),
      keepDiagnostics = true
    )
  )

  val objectWrapperOptions = BuildOptions(
    scriptOptions = ScriptOptions(
      forceObjectWrapper = Some(true)
    )
  )
  val scala213Options = BuildOptions(
    scalaOptions = ScalaOptions(
      scalaVersion = Some(MaybeScalaVersion(Some("2.13")))
    )
  )
  val platfromJsOptions = BuildOptions(
    scalaOptions = ScalaOptions(
      platform = Some(Positioned(List(Position.CommandLine()), Platform.JS))
    )
  )

  test(s"class wrapper for scala 3") {
    val inputs = TestInputs(
      os.rel / "script1.sc" ->
        s"""//> using dep "com.lihaoyi::os-lib:0.9.1"
           |
           |def main(args: String*): Unit = println("Hello")
           |main()
           |""".stripMargin,
      os.rel / "script2.sc" ->
        """//> using dep "com.lihaoyi::os-lib:0.9.1"
          |
          |println("Hello")
          |""".stripMargin
    )

    inputs.withBuild(baseOptions, buildThreads, bloopConfigOpt) {
      (root, _, maybeBuild) =>
        expect(maybeBuild.orThrow.success)
        val projectDir = os.list(root / ".scala-build").filter(
          _.baseName.startsWith(root.baseName + "_")
        )
        expect(projectDir.size == 1)
        expectClassWrapper(
          "script1",
          projectDir.head / "src_generated" / "main" / "script1.scala"
        )
        expectClassWrapper(
          "script2",
          projectDir.head / "src_generated" / "main" / "script2.scala"
        )
    }
  }

  for {
    useDirectives <- Seq(true, false)
    (directive, options, optionName) <- Seq(
      ("//> using object.wrapper", objectWrapperOptions, "--object-wrapper"),
      ("//> using scala 2.13", scala213Options, "--scala 2.13"),
      ("//> using platform js", platfromJsOptions, "--js")
    )
  } {
    val inputs = TestInputs(
      os.rel / "script1.sc" ->
        s"""//> using dep "com.lihaoyi::os-lib:0.9.1"
           |${if (useDirectives) directive else ""}
           |
           |def main(args: String*): Unit = println("Hello")
           |main()
           |""".stripMargin,
      os.rel / "script2.sc" ->
        """//> using dep "com.lihaoyi::os-lib:0.9.1"
          |
          |println("Hello")
          |""".stripMargin
    )

    test(
      s"object wrapper forced with ${if (useDirectives) directive else optionName}"
    ) {
      inputs.withBuild(options orElse baseOptions, buildThreads, bloopConfigOpt) {
        (root, _, maybeBuild) =>
          expect(maybeBuild.orThrow.success)
          val projectDir = os.list(root / ".scala-build").filter(
            _.baseName.startsWith(root.baseName + "_")
          )
          expect(projectDir.size == 1)
          expectObjectWrapper(
            "script1",
            projectDir.head / "src_generated" / "main" / "script1.scala"
          )
          expectObjectWrapper(
            "script2",
            projectDir.head / "src_generated" / "main" / "script2.scala"
          )
      }
    }
  }

  for {
    (targetDirective, enablingDirective) <- Seq(
      ("target.scala 3.2.2", "scala 3.2.2"),
      ("target.platform scala-native", "platform scala-native")
    )
  } {
    val inputs = TestInputs(
      os.rel / "script1.sc" ->
        s"""//> using dep "com.lihaoyi::os-lib:0.9.1"
           |//> using $targetDirective
           |//> using objectWrapper
           |
           |def main(args: String*): Unit = println("Hello")
           |main()
           |""".stripMargin,
      os.rel / "script2.sc" ->
        s"""//> using dep "com.lihaoyi::os-lib:0.9.1"
           |//> using $enablingDirective
           |
           |println("Hello")
           |""".stripMargin
    )

    test(
      s"object wrapper with $targetDirective"
    ) {
      inputs.withBuild(baseOptions, buildThreads, bloopConfigOpt) {
        (root, _, maybeBuild) =>
          expect(maybeBuild.orThrow.success)
          val projectDir = os.list(root / ".scala-build").filter(
            _.baseName.startsWith(root.baseName + "_")
          )
          expect(projectDir.size == 1)

          expectObjectWrapper(
            "script1",
            projectDir.head / "src_generated" / "main" / "script1.scala"
          )
          expectObjectWrapper(
            "script2",
            projectDir.head / "src_generated" / "main" / "script2.scala"
          )
      }
    }
  }
}
