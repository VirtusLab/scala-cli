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

}
