package scala.build.tests

import com.eed3si9n.expecty.Expecty.{assert => expect}

import scala.build.internal.NativeBuilderHelper
import scala.build.options.{BuildOptions, InternalOptions}
import scala.build.tests.util.BloopServer
import scala.build.{BuildThreads, Directories, LocalRepo}
import scala.util.{Properties, Random}

class NativeBuilderHelperTests extends munit.FunSuite {

  val buildThreads = BuildThreads.create()
  def bloopConfig  = BloopServer.bloopConfig

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

      val config        = build.options.scalaNativeOptions.configCliOptions()
      val nativeWorkDir = build.options.scalaNativeOptions.nativeWorkDir(root, "native-test")
      val destPath      = nativeWorkDir / s"main${if (Properties.isWin) ".exe" else ""}"
      // generate dummy output
      os.write(destPath, Random.alphanumeric.take(10).mkString(""), createFolders = true)

      val cacheData =
        NativeBuilderHelper.getCacheData(build, config, destPath, nativeWorkDir)
      expect(cacheData.changed)
    }
  }

  test("should not rebuild the second time") {
    inputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (root, _, maybeBuild) =>
      val build = maybeBuild.toOption.get.successfulOpt.get

      val config        = build.options.scalaNativeOptions.configCliOptions()
      val nativeWorkDir = build.options.scalaNativeOptions.nativeWorkDir(root, "native-test")
      val destPath      = nativeWorkDir / s"main${if (Properties.isWin) ".exe" else ""}"
      // generate dummy output
      os.write(destPath, Random.alphanumeric.take(10).mkString(""), createFolders = true)

      val cacheData =
        NativeBuilderHelper.getCacheData(build, config, destPath, nativeWorkDir)
      NativeBuilderHelper.updateProjectAndOutputSha(destPath, nativeWorkDir, cacheData.projectSha)
      expect(cacheData.changed)

      val sameBuildCache =
        NativeBuilderHelper.getCacheData(build, config, destPath, nativeWorkDir)
      expect(!sameBuildCache.changed)
    }
  }

  test("should build native if output file was deleted") {
    inputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (root, _, maybeBuild) =>
      val build = maybeBuild.toOption.get.successfulOpt.get

      val config        = build.options.scalaNativeOptions.configCliOptions()
      val nativeWorkDir = build.options.scalaNativeOptions.nativeWorkDir(root, "native-test")
      val destPath      = nativeWorkDir / s"main${if (Properties.isWin) ".exe" else ""}"
      // generate dummy output
      os.write(destPath, Random.alphanumeric.take(10).mkString(""), createFolders = true)

      val cacheData =
        NativeBuilderHelper.getCacheData(build, config, destPath, nativeWorkDir)
      NativeBuilderHelper.updateProjectAndOutputSha(destPath, nativeWorkDir, cacheData.projectSha)
      expect(cacheData.changed)

      os.remove(destPath)
      val afterDeleteCache =
        NativeBuilderHelper.getCacheData(build, config, destPath, nativeWorkDir)
      expect(afterDeleteCache.changed)
    }
  }

  test("should build native if output file was changed") {
    inputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (root, _, maybeBuild) =>
      val build = maybeBuild.toOption.get.successfulOpt.get

      val config        = build.options.scalaNativeOptions.configCliOptions()
      val nativeWorkDir = build.options.scalaNativeOptions.nativeWorkDir(root, "native-test")
      val destPath      = nativeWorkDir / s"main${if (Properties.isWin) ".exe" else ""}"
      // generate dummy output
      os.write(destPath, Random.alphanumeric.take(10).mkString(""), createFolders = true)

      val cacheData =
        NativeBuilderHelper.getCacheData(build, config, destPath, nativeWorkDir)
      NativeBuilderHelper.updateProjectAndOutputSha(destPath, nativeWorkDir, cacheData.projectSha)
      expect(cacheData.changed)

      os.write.over(destPath, Random.alphanumeric.take(10).mkString(""))
      val cacheAfterFileUpdate =
        NativeBuilderHelper.getCacheData(build, config, destPath, nativeWorkDir)
      expect(cacheAfterFileUpdate.changed)
    }
  }

  test("should build native if input file was changed") {
    inputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (root, _, maybeBuild) =>
      val build = maybeBuild.toOption.get.successfulOpt.get

      val config        = build.options.scalaNativeOptions.configCliOptions()
      val nativeWorkDir = build.options.scalaNativeOptions.nativeWorkDir(root, "native-test")
      val destPath      = nativeWorkDir / s"main${if (Properties.isWin) ".exe" else ""}"
      os.write(destPath, Random.alphanumeric.take(10).mkString(""), createFolders = true)

      val cacheData =
        NativeBuilderHelper.getCacheData(build, config, destPath, nativeWorkDir)
      NativeBuilderHelper.updateProjectAndOutputSha(destPath, nativeWorkDir, cacheData.projectSha)
      expect(cacheData.changed)

      os.write.append(root / helloFileName, Random.alphanumeric.take(10).mkString(""))
      val cacheAfterFileUpdate =
        NativeBuilderHelper.getCacheData(build, config, destPath, nativeWorkDir)
      expect(cacheAfterFileUpdate.changed)
    }
  }

  test("should build native if native config was changed") {
    inputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (root, _, maybeBuild) =>
      val build = maybeBuild.toOption.get.successfulOpt.get

      val config        = build.options.scalaNativeOptions.configCliOptions()
      val nativeWorkDir = build.options.scalaNativeOptions.nativeWorkDir(root, "native-test")
      val destPath      = nativeWorkDir / s"main${if (Properties.isWin) ".exe" else ""}"
      os.write(destPath, Random.alphanumeric.take(10).mkString(""), createFolders = true)

      val cacheData =
        NativeBuilderHelper.getCacheData(build, config, destPath, nativeWorkDir)
      NativeBuilderHelper.updateProjectAndOutputSha(destPath, nativeWorkDir, cacheData.projectSha)
      expect(cacheData.changed)

      val updatedBuild = build.copy(
        options = build.options.copy(
          scalaNativeOptions = build.options.scalaNativeOptions.copy(
            clang = Some(Random.alphanumeric.take(10).mkString(""))
          )
        )
      )
      val updatedConfig = updatedBuild.options.scalaNativeOptions.configCliOptions()

      val cacheAfterConfigUpdate =
        NativeBuilderHelper.getCacheData(
          updatedBuild,
          updatedConfig,
          destPath,
          nativeWorkDir
        )
      expect(cacheAfterConfigUpdate.changed)
    }
  }

}
