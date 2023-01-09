package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import scala.build.options.{BuildOptions, InternalOptions, PackageType}
import scala.build.tests.util.BloopServer
import scala.build.{BuildThreads, Directories, LocalRepo}

class PackagingUsingDirectiveTests extends munit.FunSuite {

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

  test("package type") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using packaging.packageType "graalvm"
          |def foo() = println("hello foo")
          |""".stripMargin
    )
    inputs.withLoadedBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      val foundPackageTypeOpt = maybeBuild.options.notForBloopOptions.packageOptions.packageTypeOpt
      expect(foundPackageTypeOpt.contains(PackageType.GraalVMNativeImage))
    }
  }

  test("output") {
    val output = "foo"
    val inputs = TestInputs(
      os.rel / "Bar.scala" ->
        s"""//> using packaging.output "$output"
           |def hello() = println("hello")
           |""".stripMargin
    )
    inputs.withLoadedBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      val maybePackageOutput  = maybeBuild.options.notForBloopOptions.packageOptions.output
      val packageOutputString = maybePackageOutput.getOrElse("None")
      val index               = packageOutputString.lastIndexOf('/')
      val packageName         = packageOutputString.drop(index + 1)
      expect(packageName == output)
    }
  }

  test("docker options") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using packaging.dockerFrom "openjdk:11"
          |//> using packaging.dockerImageTag "1.0.0"
          |//> using packaging.dockerImageRegistry "virtuslab"
          |//> using packaging.dockerImageRepository "scala-cli"
          |
          |def foo() = println("hello foo")
          |""".stripMargin
    )
    inputs.withLoadedBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      val dockerOpt = maybeBuild.options.notForBloopOptions.packageOptions.dockerOptions
      expect(dockerOpt.from == Some("openjdk:11"))
      expect(dockerOpt.imageTag == Some("1.0.0"))
      expect(dockerOpt.imageRegistry == Some("virtuslab"))
      expect(dockerOpt.imageRepository == Some("scala-cli"))
    }
  }

}
