package cli.tests

import com.eed3si9n.expecty.Expecty.expect

import java.nio.file.FileSystems

import scala.build.Ops.*
import scala.build.options.{BuildOptions, InternalOptions, PackageType}
import scala.build.tests.util.BloopServer
import scala.build.tests.{TestInputs, TestLogger}
import scala.build.{BuildThreads, Directories, LocalRepo}
import scala.cli.commands.package0.Package
import scala.cli.packaging.Library

class PackageTests extends TestUtil.ScalaCliSuite {
  val buildThreads = BuildThreads.create()
  def bloopConfig  = BloopServer.bloopConfig

  val extraRepoTmpDir = os.temp.dir(prefix = "scala-cli-tests-extra-repo-")
  val directories     = Directories.under(extraRepoTmpDir)

  val defaultOptions = BuildOptions(
    internal = InternalOptions(
      localRepository = LocalRepo.localRepo(directories.localRepoDir, TestLogger())
    )
  )

  /** Fixes - https://github.com/VirtusLab/scala-cli/issues/2166 */
  test(s"should generate a correct jar library when the project was changed") {
    TestInputs().fromRoot { root =>
      val inputs = TestInputs(
        files = Seq(os.rel / "Hello.scala" ->
          """//> using platform scala-js
            |//> using options -Wvalue-discard -Wunused:all
            |
            |object Hello extends App {
            |  println("Hello, World World World")
            |}""".stripMargin),
        forceCwd = Some(root)
      )
      inputs.withBuild(defaultOptions, buildThreads, Some(bloopConfig)) {
        (_, _, maybeFirstBuild) =>
          val firstBuild      = maybeFirstBuild.orThrow.successfulOpt.get
          val firstLibraryJar = Library.libraryJar(Seq(firstBuild))
          expect(os.exists(firstLibraryJar)) // should create library jar

          // change Hello.scala and recompile
          os.write.over(
            root / "Hello.scala",
            """//> using platform scala-js
              |//> using options -Wvalue-discard -Wunused:all
              |
              |object Hello extends App {
              |  println("hello")
              |}""".stripMargin
          )

          inputs.withBuild(
            defaultOptions,
            buildThreads,
            Some(bloopConfig),
            skipCreatingSources = true
          ) {
            (_, _, maybeSecondBuild) =>
              val secondBuild = maybeSecondBuild.orThrow.successfulOpt.get
              val libraryJar  = Library.libraryJar(Seq(secondBuild))
              val fs = // should not throw "invalid CEN header (bad signature)" ZipException
                FileSystems.newFileSystem(libraryJar.toNIO, null: ClassLoader)
              expect(fs.isOpen)
              fs.close()
          }
      }
    }
  }

  /** Fixes - https://github.com/VirtusLab/scala-cli/issues/2303 */
  test("accept packageType-native when using native platform") {
    val inputs = TestInputs(
      files = Seq(os.rel / "Hello.scala" ->
        """//> using platform native
          |//> using packaging.packageType native
          |
          |object Hello extends App {
          |  println("Hello World")
          |}""".stripMargin)
    )
    inputs.withBuild(defaultOptions, buildThreads, Some(bloopConfig)) {
      (_, _, maybeFirstBuild) =>
        val build = maybeFirstBuild.orThrow.successfulOpt.get

        val packageType = Package.resolvePackageType(Seq(build), None).orThrow
        expect(packageType == PackageType.Native.Application)
    }
  }

}
