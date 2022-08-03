package scala.cli.tests

import com.eed3si9n.expecty.Expecty.{assert => expect}

import scala.build.options.{BuildOptions, InternalOptions}
import scala.build.tests.TestInputs
import scala.build.tests.util.BloopServer
import scala.build.{BuildThreads, Directories, LocalRepo}
import scala.cli.internal.CachedBinary
import scala.util.{Properties, Random}

class CachedBinaryTests extends munit.FunSuite {

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

  private def helperTests(fromDirectory: Boolean) = {
    val additionalMessage =
      if (fromDirectory) "built from a directory" else "built from a set of files"

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
            CachedBinary.getCacheData(build, config, destPath, nativeWorkDir)
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
            CachedBinary.getCacheData(build, config, destPath, nativeWorkDir)
          CachedBinary.updateProjectAndOutputSha(
            destPath,
            nativeWorkDir,
            cacheData.projectSha
          )
          expect(cacheData.changed)

          val sameBuildCache =
            CachedBinary.getCacheData(build, config, destPath, nativeWorkDir)
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
            CachedBinary.getCacheData(build, config, destPath, nativeWorkDir)
          CachedBinary.updateProjectAndOutputSha(
            destPath,
            nativeWorkDir,
            cacheData.projectSha
          )
          expect(cacheData.changed)

          os.remove(destPath)
          val afterDeleteCache =
            CachedBinary.getCacheData(build, config, destPath, nativeWorkDir)
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
            CachedBinary.getCacheData(build, config, destPath, nativeWorkDir)
          CachedBinary.updateProjectAndOutputSha(
            destPath,
            nativeWorkDir,
            cacheData.projectSha
          )
          expect(cacheData.changed)

          os.write.over(destPath, Random.alphanumeric.take(10).mkString(""))
          val cacheAfterFileUpdate =
            CachedBinary.getCacheData(build, config, destPath, nativeWorkDir)
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
            CachedBinary.getCacheData(build, config, destPath, nativeWorkDir)
          CachedBinary.updateProjectAndOutputSha(
            destPath,
            nativeWorkDir,
            cacheData.projectSha
          )
          expect(cacheData.changed)

          os.write.append(root / helloFileName, Random.alphanumeric.take(10).mkString(""))
          val cacheAfterFileUpdate =
            CachedBinary.getCacheData(build, config, destPath, nativeWorkDir)
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
            CachedBinary.getCacheData(build, config, destPath, nativeWorkDir)
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
              updatedBuild,
              updatedConfig,
              destPath,
              nativeWorkDir
            )
          expect(cacheAfterConfigUpdate.changed)
      }
    }
  }

  helperTests(fromDirectory = false)
  helperTests(fromDirectory = true)

}
