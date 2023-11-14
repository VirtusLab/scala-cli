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

  def expectObjectScalaCliAppWrapper(wrapperName: String, path: os.Path) = {
    val generatedFileContent = os.read(path)
    assert(
      generatedFileContent.contains(s"object $wrapperName extends scala.cli.build.ScalaCliApp {"),
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
      !(generatedFileContent.contains(s"object $wrapperName {") ||
      generatedFileContent.contains(s"object $wrapperName extends scala.cli.build.ScalaCliApp {")),
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

  val delayedInitOptions = BuildOptions(
    scriptOptions = ScriptOptions(
      forceDelayedInitWrapper = Some(true)
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

    inputs.withBuild(delayedInitOptions orElse baseOptions, buildThreads, bloopConfigOpt) {
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
      ("//> using wrapper.object", objectWrapperOptions, "--object-wrapper"),
      ("//> using scala 2.13.1", scala213Options, "--scala 2.13.1"),
      ("//> using platform js", platfromJsOptions, "--js")
    )
  } {
    def script1Code(directives: String*) =
      s"""//> using dep "com.lihaoyi::os-lib:0.9.1"
         |${if (useDirectives) directives.mkString(System.lineSeparator()) else ""}
         |
         |def main(args: String*): Unit = println("Hello")
         |main()
         |""".stripMargin

    val script2Code =
      """//> using dep "com.lihaoyi::os-lib:0.9.1"
        |
        |println("Hello")
        |""".stripMargin

    val inputs = TestInputs(
      os.rel / "script1.sc" -> script1Code(directive),
      os.rel / "script2.sc" -> script2Code
    )

    val testSuffixObject = if (useDirectives) directive else optionName
    val testSuffixDelayedInit =
      if (useDirectives) "//> using wrapper.delayedInit" else "--delayed-init"

    test(s"object wrapper forced with $testSuffixObject") {
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
          assert(
            !os.exists(projectDir.head / "src_generated" / "main" / "delayed-init-wrapper.scala")
          )

      }
    }

    val delayedInitInputs = TestInputs(
      if (useDirectives)
        os.rel / "script1.sc"    -> script1Code(directive, "//> using wrapper.delayedInit")
      else os.rel / "script1.sc" -> script1Code(directive),
      os.rel / "script2.sc" -> script2Code
    )

    test(
      s"object wrapper with ScalaCliApp forced with $testSuffixObject and $testSuffixDelayedInit"
    ) {
      val buildOptions = options orElse delayedInitOptions orElse baseOptions
      delayedInitInputs.withBuild(buildOptions, buildThreads, bloopConfigOpt) {
        (root, _, maybeBuild) =>
          expect(maybeBuild.orThrow.success)
          val projectDir = os.list(root / ".scala-build").filter(
            _.baseName.startsWith(root.baseName + "_")
          )
          expect(projectDir.size == 1)
          expectObjectScalaCliAppWrapper(
            "script1",
            projectDir.head / "src_generated" / "main" / "script1.scala"
          )
          expectObjectScalaCliAppWrapper(
            "script2",
            projectDir.head / "src_generated" / "main" / "script2.scala"
          )
          assert(
            os.isFile(projectDir.head / "src_generated" / "main" / "delayed-init-wrapper.scala")
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
          assert(
            !os.exists(projectDir.head / "src_generated" / "main" / "delayed-init-wrapper.scala")
          )
      }
    }
  }
}
