package scala.build.tests

import com.eed3si9n.expecty.Expecty.{assert => expect}

import scala.build.blooprifle.BloopRifleConfig
import scala.build.internal.NativeBuilderHelper
import scala.build.options.{BuildOptions, InternalOptions}
import scala.build.{Bloop, BuildThreads, Directories, LocalRepo, Logger}
import scala.util.{Properties, Random}

class NativeBuilderHelperTests extends munit.FunSuite {

  val buildThreads = BuildThreads.create()
  val bloopConfig  = BloopRifleConfig.default(v => Bloop.bloopClassPath(Logger.nop, v))

  val helloFileName = "Hello.scala"

  val inputs = TestInputs(
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

  val extraRepoTmpDir = os.temp.dir(prefix = "scala-cli-tests-extra-repo-")
  val directories     = Directories.under(extraRepoTmpDir)

  val defaultOptions = BuildOptions(
    internal = InternalOptions(
      localRepository = LocalRepo.localRepo(directories.localRepoDir)
    )
  )

  test("should build native app at first time") {

    inputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (root, _, maybeBuild) =>
      val build = maybeBuild.toOption.get.successfulOpt.get

      val nativeConfig  = build.options.scalaNativeOptions.config
      val nativeWorkDir = build.options.scalaNativeOptions.nativeWorkDir(root, "native-test")
      val destPath      = nativeWorkDir / s"main${if (Properties.isWin) ".exe" else ""}"
      // generate dummy output
      os.write(destPath, Random.alphanumeric.take(10).mkString(""), createFolders = true)

      val changed =
        NativeBuilderHelper.shouldBuildIfChanged(build, nativeConfig, destPath, nativeWorkDir)
      expect(changed)
    }
  }

  test("should not rebuild the second time") {
    inputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (root, _, maybeBuild) =>
      val build = maybeBuild.toOption.get.successfulOpt.get

      val nativeConfig  = build.options.scalaNativeOptions.config
      val nativeWorkDir = build.options.scalaNativeOptions.nativeWorkDir(root, "native-test")
      val destPath      = nativeWorkDir / s"main${if (Properties.isWin) ".exe" else ""}"
      // generate dummy output
      os.write(destPath, Random.alphanumeric.take(10).mkString(""), createFolders = true)

      val changed =
        NativeBuilderHelper.shouldBuildIfChanged(build, nativeConfig, destPath, nativeWorkDir)
      NativeBuilderHelper.updateOutputSha(destPath, nativeWorkDir)
      expect(changed)

      val changedSameBuild =
        NativeBuilderHelper.shouldBuildIfChanged(build, nativeConfig, destPath, nativeWorkDir)
      expect(!changedSameBuild)
    }
  }

  test("should build native if output file was deleted") {
    inputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (root, _, maybeBuild) =>
      val build = maybeBuild.toOption.get.successfulOpt.get

      val nativeConfig  = build.options.scalaNativeOptions.config
      val nativeWorkDir = build.options.scalaNativeOptions.nativeWorkDir(root, "native-test")
      val destPath      = nativeWorkDir / s"main${if (Properties.isWin) ".exe" else ""}"
      // generate dummy output
      os.write(destPath, Random.alphanumeric.take(10).mkString(""), createFolders = true)

      val changed =
        NativeBuilderHelper.shouldBuildIfChanged(build, nativeConfig, destPath, nativeWorkDir)
      NativeBuilderHelper.updateOutputSha(destPath, nativeWorkDir)
      expect(changed)

      os.remove(destPath)
      val changedAfterDelete =
        NativeBuilderHelper.shouldBuildIfChanged(build, nativeConfig, destPath, nativeWorkDir)
      expect(changedAfterDelete)
    }
  }

  test("should build native if output file was changed") {
    inputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (root, _, maybeBuild) =>
      val build = maybeBuild.toOption.get.successfulOpt.get

      val nativeConfig  = build.options.scalaNativeOptions.config
      val nativeWorkDir = build.options.scalaNativeOptions.nativeWorkDir(root, "native-test")
      val destPath      = nativeWorkDir / s"main${if (Properties.isWin) ".exe" else ""}"
      // generate dummy output
      os.write(destPath, Random.alphanumeric.take(10).mkString(""), createFolders = true)

      val changed =
        NativeBuilderHelper.shouldBuildIfChanged(build, nativeConfig, destPath, nativeWorkDir)
      NativeBuilderHelper.updateOutputSha(destPath, nativeWorkDir)
      expect(changed)

      os.write.over(destPath, Random.alphanumeric.take(10).mkString(""))
      val changedAfterFileUpdate =
        NativeBuilderHelper.shouldBuildIfChanged(build, nativeConfig, destPath, nativeWorkDir)
      expect(changedAfterFileUpdate)
    }
  }

  test("should build native if input file was changed") {
    inputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (root, _, maybeBuild) =>
      val build = maybeBuild.toOption.get.successfulOpt.get

      val nativeConfig  = build.options.scalaNativeOptions.config
      val nativeWorkDir = build.options.scalaNativeOptions.nativeWorkDir(root, "native-test")
      val destPath      = nativeWorkDir / s"main${if (Properties.isWin) ".exe" else ""}"
      os.write(destPath, Random.alphanumeric.take(10).mkString(""), createFolders = true)

      val changed =
        NativeBuilderHelper.shouldBuildIfChanged(build, nativeConfig, destPath, nativeWorkDir)
      NativeBuilderHelper.updateOutputSha(destPath, nativeWorkDir)
      expect(changed)

      os.write.append(root / helloFileName, Random.alphanumeric.take(10).mkString(""))
      val changedAfterFileUpdate =
        NativeBuilderHelper.shouldBuildIfChanged(build, nativeConfig, destPath, nativeWorkDir)
      expect(changedAfterFileUpdate)
    }
  }

  test("should build native if native config was changed") {
    inputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (root, _, maybeBuild) =>
      val build = maybeBuild.toOption.get.successfulOpt.get

      val nativeConfig  = build.options.scalaNativeOptions.config
      val nativeWorkDir = build.options.scalaNativeOptions.nativeWorkDir(root, "native-test")
      val destPath      = nativeWorkDir / s"main${if (Properties.isWin) ".exe" else ""}"
      os.write(destPath, Random.alphanumeric.take(10).mkString(""), createFolders = true)

      val changed =
        NativeBuilderHelper.shouldBuildIfChanged(build, nativeConfig, destPath, nativeWorkDir)
      NativeBuilderHelper.updateOutputSha(destPath, nativeWorkDir)
      expect(changed)

      val updatedBuild = build.copy(
        options = build.options.copy(
          scalaNativeOptions = build.options.scalaNativeOptions.copy(
            clang = Some(Random.alphanumeric.take(10).mkString(""))
          )
        )
      )
      val updatedNativeConfig = updatedBuild.options.scalaNativeOptions.config

      val changedAfterConfigUpdate =
        NativeBuilderHelper.shouldBuildIfChanged(
          updatedBuild,
          updatedNativeConfig,
          destPath,
          nativeWorkDir
        )
      expect(changedAfterConfigUpdate)
    }
  }

}
