package scala.cli.tests

import bloop.rifle.BloopRifleConfig
import cli.tests.TestUtil
import com.eed3si9n.expecty.Expecty.assert as expect
import os.Path

import scala.build.input.ResourceDirectory
import scala.build.options.{BuildOptions, InternalOptions}
import scala.build.tests.util.BloopServer
import scala.build.tests.{TestInputs, TestLogger}
import scala.build.{Build, BuildThreads, Directories, LocalRepo}
import scala.cli.internal.CachedBinary
import scala.util.{Properties, Random}

class CachedBinaryTests extends TestUtil.ScalaCliSuite {
  val buildThreads: BuildThreads    = BuildThreads.create()
  def bloopConfig: BloopRifleConfig = BloopServer.bloopConfig

  val helloFileName = "Hello.scala"

  val inputs: TestInputs = TestInputs(
    os.rel / helloFileName ->
      s"""object Hello extends App {
         |  println("Hello")
         |}
         |""".stripMargin,
    os.rel / "main" / "Main.scala" ->
      s"""object Main extends App {
         |  println("Hello")
         |}
         |""".stripMargin
  )

  val extraRepoTmpDir: Path    = os.temp.dir(prefix = "scala-cli-tests-extra-repo-")
  val directories: Directories = Directories.under(extraRepoTmpDir)

  val defaultOptions: BuildOptions = BuildOptions(
    internal = InternalOptions(
      localRepository = LocalRepo.localRepo(directories.localRepoDir, TestLogger())
    )
  )

  for {
    fromDirectory <- List(false, true)
    additionalMessage = if (fromDirectory) "built from a directory" else "built from a set of files"
  } {
    test(s"should build native app with added test scope at first time ($additionalMessage)") {
      TestInputs(
        os.rel / "main" / "Main.scala" ->
          s"""object Main extends App {
             |  println("Hello")
             |}
             |""".stripMargin,
        os.rel / "test" / "TestScope.scala" ->
          s"""object TestScope extends App {
             |  println("Hello from the test scope")
             |}
             |""".stripMargin
      ).withLoadedBuilds(
        defaultOptions,
        buildThreads,
        Some(bloopConfig),
        fromDirectory
      ) {
        (_, _, builds) =>
          expect(builds.builds.forall(_.success))

          val config =
            builds.main.options.scalaNativeOptions.configCliOptions(resourcesExist = false)
          val nativeWorkDir = builds.main.inputs.nativeWorkDir
          val destPath      = nativeWorkDir / s"main${if (Properties.isWin) ".exe" else ""}"
          // generate dummy output
          os.write(destPath, Random.alphanumeric.take(10).mkString(""), createFolders = true)

          val successfulBuilds = builds.builds.map { case s: Build.Successful => s }
          val cacheData        =
            CachedBinary.getCacheData(successfulBuilds, config, destPath, nativeWorkDir)
          expect(cacheData.changed)
      }
    }

    test(s"should build native app at first time ($additionalMessage)") {
      inputs.withLoadedBuild(defaultOptions, buildThreads, Some(bloopConfig), fromDirectory) {
        (_, _, maybeBuild) =>
          val build = maybeBuild.successfulOpt.get

          val config = build.options.scalaNativeOptions.configCliOptions(resourcesExist = false)
          val nativeWorkDir = build.inputs.nativeWorkDir
          val destPath      = nativeWorkDir / s"main${if (Properties.isWin) ".exe" else ""}"
          // generate dummy output
          os.write(destPath, Random.alphanumeric.take(10).mkString(""), createFolders = true)

          val cacheData =
            CachedBinary.getCacheData(Seq(build), config, destPath, nativeWorkDir)
          expect(cacheData.changed)
      }
    }

    test(s"should not rebuild the second time ($additionalMessage)") {
      inputs.withLoadedBuild(defaultOptions, buildThreads, Some(bloopConfig), fromDirectory) {
        (_, _, maybeBuild) =>
          val build = maybeBuild.successfulOpt.get

          val config = build.options.scalaNativeOptions.configCliOptions(resourcesExist = false)
          val nativeWorkDir = build.inputs.nativeWorkDir
          val destPath      = nativeWorkDir / s"main${if (Properties.isWin) ".exe" else ""}"
          // generate dummy output
          os.write(destPath, Random.alphanumeric.take(10).mkString(""), createFolders = true)

          val cacheData =
            CachedBinary.getCacheData(Seq(build), config, destPath, nativeWorkDir)
          CachedBinary.updateProjectAndOutputSha(
            destPath,
            nativeWorkDir,
            cacheData.projectSha
          )
          expect(cacheData.changed)

          val sameBuildCache =
            CachedBinary.getCacheData(Seq(build), config, destPath, nativeWorkDir)
          expect(!sameBuildCache.changed)
      }
    }

    test(s"should build native if output file was deleted ($additionalMessage)") {
      inputs.withLoadedBuild(defaultOptions, buildThreads, Some(bloopConfig), fromDirectory) {
        (_, _, maybeBuild) =>
          val build = maybeBuild.successfulOpt.get

          val config = build.options.scalaNativeOptions.configCliOptions(resourcesExist = false)
          val nativeWorkDir = build.inputs.nativeWorkDir
          val destPath      = nativeWorkDir / s"main${if (Properties.isWin) ".exe" else ""}"
          // generate dummy output
          os.write(destPath, Random.alphanumeric.take(10).mkString(""), createFolders = true)

          val cacheData =
            CachedBinary.getCacheData(Seq(build), config, destPath, nativeWorkDir)
          CachedBinary.updateProjectAndOutputSha(
            destPath,
            nativeWorkDir,
            cacheData.projectSha
          )
          expect(cacheData.changed)

          os.remove(destPath)
          val afterDeleteCache =
            CachedBinary.getCacheData(Seq(build), config, destPath, nativeWorkDir)
          expect(afterDeleteCache.changed)
      }
    }

    test(s"should build native if output file was changed ($additionalMessage)") {
      inputs.withLoadedBuild(defaultOptions, buildThreads, Some(bloopConfig), fromDirectory) {
        (_, _, maybeBuild) =>
          val build = maybeBuild.successfulOpt.get

          val config = build.options.scalaNativeOptions.configCliOptions(resourcesExist = false)
          val nativeWorkDir = build.inputs.nativeWorkDir
          val destPath      = nativeWorkDir / s"main${if (Properties.isWin) ".exe" else ""}"
          // generate dummy output
          os.write(destPath, Random.alphanumeric.take(10).mkString(""), createFolders = true)

          val cacheData =
            CachedBinary.getCacheData(Seq(build), config, destPath, nativeWorkDir)
          CachedBinary.updateProjectAndOutputSha(
            destPath,
            nativeWorkDir,
            cacheData.projectSha
          )
          expect(cacheData.changed)

          os.write.over(destPath, Random.alphanumeric.take(10).mkString(""))
          val cacheAfterFileUpdate =
            CachedBinary.getCacheData(Seq(build), config, destPath, nativeWorkDir)
          expect(cacheAfterFileUpdate.changed)
      }
    }

    test(s"should build native if input file was changed ($additionalMessage)") {
      inputs.withLoadedBuild(defaultOptions, buildThreads, Some(bloopConfig), fromDirectory) {
        (root, _, maybeBuild) =>
          val build = maybeBuild.successfulOpt.get

          val config = build.options.scalaNativeOptions.configCliOptions(resourcesExist = false)
          val nativeWorkDir = build.inputs.nativeWorkDir
          val destPath      = nativeWorkDir / s"main${if (Properties.isWin) ".exe" else ""}"
          os.write(destPath, Random.alphanumeric.take(10).mkString(""), createFolders = true)

          val cacheData =
            CachedBinary.getCacheData(Seq(build), config, destPath, nativeWorkDir)
          CachedBinary.updateProjectAndOutputSha(
            destPath,
            nativeWorkDir,
            cacheData.projectSha
          )
          expect(cacheData.changed)

          os.write.append(root / helloFileName, Random.alphanumeric.take(10).mkString(""))
          val cacheAfterFileUpdate =
            CachedBinary.getCacheData(Seq(build), config, destPath, nativeWorkDir)
          expect(cacheAfterFileUpdate.changed)
      }
    }

    test(s"should build native if native config was changed ($additionalMessage)") {
      inputs.withLoadedBuild(defaultOptions, buildThreads, Some(bloopConfig), fromDirectory) {
        (_, _, maybeBuild) =>
          val build = maybeBuild.successfulOpt.get

          val config = build.options.scalaNativeOptions.configCliOptions(resourcesExist = false)
          val nativeWorkDir = build.inputs.nativeWorkDir
          val destPath      = nativeWorkDir / s"main${if (Properties.isWin) ".exe" else ""}"
          os.write(destPath, Random.alphanumeric.take(10).mkString(""), createFolders = true)

          val cacheData =
            CachedBinary.getCacheData(Seq(build), config, destPath, nativeWorkDir)
          CachedBinary.updateProjectAndOutputSha(
            destPath,
            nativeWorkDir,
            cacheData.projectSha
          )
          expect(cacheData.changed)

          val updatedBuild = build.copy(
            options = build.options.copy(
              scalaNativeOptions = build.options.scalaNativeOptions.copy(
                clang = Some(Random.alphanumeric.take(10).mkString(""))
              )
            )
          )
          val updatedConfig =
            updatedBuild.options.scalaNativeOptions.configCliOptions(resourcesExist = false)

          val cacheAfterConfigUpdate =
            CachedBinary.getCacheData(
              Seq(updatedBuild),
              updatedConfig,
              destPath,
              nativeWorkDir
            )
          expect(cacheAfterConfigUpdate.changed)
      }
    }

    for {
      slothOpt <- Seq(Some(true), Some(false), None)
    }
      test(s"should rebuild when --sloth changes from $slothOpt ($additionalMessage)") {
        inputs.withLoadedBuild(
          defaultOptions.copy(
            notForBloopOptions = defaultOptions.notForBloopOptions.copy(slothOpt = slothOpt)
          ),
          buildThreads,
          Some(bloopConfig),
          fromDirectory
        ) {
          (_, _, maybeBuild) =>
            val build = maybeBuild.successfulOpt.get

            val config = build.options.scalaNativeOptions.configCliOptions(resourcesExist = false)
            val nativeWorkDir = build.inputs.nativeWorkDir
            val destPath      = nativeWorkDir / s"main${if (Properties.isWin) ".exe" else ""}"
            os.write(destPath, Random.alphanumeric.take(10).mkString(""), createFolders = true)

            val cacheData =
              CachedBinary.getCacheData(Seq(build), config, destPath, nativeWorkDir)
            CachedBinary.updateProjectAndOutputSha(
              destPath,
              nativeWorkDir,
              cacheData.projectSha
            )
            expect(cacheData.changed)

            val updatedBuild = build.copy(
              options = build.options.copy(
                notForBloopOptions = build.options.notForBloopOptions.copy(
                  slothOpt = Some(!slothOpt.getOrElse(false))
                )
              )
            )

            val cacheAfterSlothToggle =
              CachedBinary.getCacheData(Seq(updatedBuild), config, destPath, nativeWorkDir)
            expect(cacheAfterSlothToggle.changed)
        }
      }
  }

  private def withResourcePath(
    build: Build.Successful,
    resourcePath: Path,
    viaInputs: Boolean
  ): Build.Successful =
    if viaInputs then build.copy(inputs = build.inputs.add(Seq(ResourceDirectory(resourcePath))))
    else
      build.copy(options =
        build.options.copy(
          classPathOptions =
            build.options.classPathOptions.copy(resourcePaths = Seq(resourcePath))
        )
      )

  for {
    viaInputs <- Seq(false, true)
    passedVia = if viaInputs then "input elements" else "classpath options"
  } {
    test(s"should handle an individual resource file passed via $passedVia") {
      inputs.withLoadedBuild(defaultOptions, buildThreads, Some(bloopConfig)) {
        (root, _, maybeBuild) =>
          val resourcePath = root / "resources" / "data.txt"
          os.write(resourcePath, "resource content", createFolders = true)
          val build = withResourcePath(maybeBuild.successfulOpt.get, resourcePath, viaInputs)

          val config = build.options.scalaNativeOptions.configCliOptions(resourcesExist = true)
          val nativeWorkDir = build.inputs.nativeWorkDir
          val destPath      = nativeWorkDir / s"main${if (Properties.isWin) ".exe" else ""}"
          os.write(destPath, Random.alphanumeric.take(10).mkString(""), createFolders = true)

          val cacheData = CachedBinary.getCacheData(Seq(build), config, destPath, nativeWorkDir)
          expect(cacheData.changed)
          CachedBinary.updateProjectAndOutputSha(destPath, nativeWorkDir, cacheData.projectSha)

          val sameBuildCache =
            CachedBinary.getCacheData(Seq(build), config, destPath, nativeWorkDir)
          expect(!sameBuildCache.changed)

          os.write.append(resourcePath, Random.alphanumeric.take(10).mkString(""))
          val cacheAfterResourceUpdate =
            CachedBinary.getCacheData(Seq(build), config, destPath, nativeWorkDir)
          expect(cacheAfterResourceUpdate.changed)
      }
    }

    test(s"should handle a non-existent resource path passed via $passedVia") {
      inputs.withLoadedBuild(defaultOptions, buildThreads, Some(bloopConfig)) {
        (root, _, maybeBuild) =>
          val build =
            withResourcePath(maybeBuild.successfulOpt.get, root / "missing", viaInputs)

          val config = build.options.scalaNativeOptions.configCliOptions(resourcesExist = false)
          val nativeWorkDir = build.inputs.nativeWorkDir
          val destPath      = nativeWorkDir / s"main${if (Properties.isWin) ".exe" else ""}"
          os.write(destPath, Random.alphanumeric.take(10).mkString(""), createFolders = true)

          val cacheData = CachedBinary.getCacheData(Seq(build), config, destPath, nativeWorkDir)
          expect(cacheData.changed)
          CachedBinary.updateProjectAndOutputSha(destPath, nativeWorkDir, cacheData.projectSha)

          val sameBuildCache =
            CachedBinary.getCacheData(Seq(build), config, destPath, nativeWorkDir)
          expect(!sameBuildCache.changed)
      }
    }
  }
}
