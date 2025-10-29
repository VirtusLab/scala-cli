package scala.cli.integration

import ch.epfl.scala.bsp4j as b
import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*
import scala.util.Properties

class BspTests3NextRc extends BspTestDefinitions with BspTests3Definitions with Test3NextRc {
  test("BSP respects --cli-default-scala-version & --predefined-repository launcher options") {
    // 3.5.0-RC1-fakeversion-bin-SNAPSHOT has too long filenames for Windows.
    // Yes, seriously. Which is why we can't use it there.
    val sv = if (Properties.isWin) Constants.scala3NextRc else "3.5.0-RC1-fakeversion-bin-SNAPSHOT"
    val inputs = TestInputs(
      os.rel / "simple.sc" -> s"""assert(dotty.tools.dotc.config.Properties.versionNumberString == "$sv")"""
    )
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "bloop", "exit", "--power").call(cwd = root)
      val localRepoPath = root / "local-repo"
      if (Properties.isWin) {
        val artifactNames = Seq(
          "scala3-compiler_3",
          "scala3-staging_3",
          "scala3-tasty-inspector_3",
          "scala3-sbt-bridge"
        )
        for { artifactName <- artifactNames } {
          val csRes = os.proc(
            TestUtil.cs,
            "fetch",
            "--cache",
            localRepoPath,
            s"org.scala-lang:$artifactName:$sv"
          )
            .call(cwd = root)
          expect(csRes.exitCode == 0)
        }
      }
      else {
        TestUtil.initializeGit(root)
        os.proc(
          "git",
          "clone",
          "https://github.com/dotty-staging/maven-test-repo.git",
          localRepoPath.toString
        ).call(cwd = root)
      }
      val predefinedRepository =
        if (Properties.isWin)
          (localRepoPath / "https" / "repo1.maven.org" / "maven2").toNIO.toUri.toASCIIString
        else
          (localRepoPath / "thecache" / "https" / "repo1.maven.org" / "maven2").toNIO.toUri.toASCIIString
      os.proc(
        TestUtil.cli,
        "--cli-default-scala-version",
        sv,
        "--predefined-repository",
        predefinedRepository,
        "setup-ide",
        "simple.sc",
        "--with-compiler",
        "--offline",
        "--power"
      )
        .call(cwd = root)
      val ideOptionsPath = root / Constants.workspaceDirName / "ide-options-v2.json"
      expect(ideOptionsPath.toNIO.toFile.exists())
      val ideEnvsPath = root / Constants.workspaceDirName / "ide-envs.json"
      expect(ideEnvsPath.toNIO.toFile.exists())
      val ideLauncherOptionsPath = root / Constants.workspaceDirName / "ide-launcher-options.json"
      expect(ideLauncherOptionsPath.toNIO.toFile.exists())
      val jsonOptions     = List("--json-options", ideOptionsPath.toString)
      val launcherOptions = List("--json-launcher-options", ideLauncherOptionsPath.toString)
      val envOptions      = List("--envs-file", ideEnvsPath.toString)
      val bspOptions      = jsonOptions ++ launcherOptions ++ envOptions
      withBsp(inputs, Seq("."), bspOptions = bspOptions, reuseRoot = Some(root)) {
        (_, _, remoteServer) =>
          Future {
            val targets = remoteServer.workspaceBuildTargets().asScala.await
              .getTargets.asScala
              .filter(!_.getId.getUri.contains("-test"))
              .map(_.getId())
            val compileResult =
              remoteServer.buildTargetCompile(new b.CompileParams(targets.asJava)).asScala.await
            val expectedStatusCode = b.StatusCode.OK
            expect(compileResult.getStatusCode == expectedStatusCode)
            val runResult =
              remoteServer.buildTargetRun(new b.RunParams(targets.head)).asScala.await
            expect(runResult.getStatusCode == expectedStatusCode)
          }
      }
    }
  }
}
