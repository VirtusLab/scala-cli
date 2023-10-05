package scala.build.options.publish

import com.eed3si9n.expecty.Expecty.expect
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants

import scala.build.options.ComputeVersion
import scala.build.tests.{TestInputs, TestUtil}

class ComputeVersionTests extends munit.FunSuite {
  test("git tag") {
    TestInputs().fromRoot { root =>
      val ghRepo = "scala-cli/compute-version-test"
      val repo =
        if (TestUtil.isCI) s"https://git@github.com/$ghRepo.git"
        else s"https://github.com/$ghRepo.git"
      os.proc("git", "clone", repo)
        .call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)
      val dir = root / "compute-version-test"
      val cv  = ComputeVersion.GitTag(os.rel, true, Nil, "0.0.1-SNAPSHOT")

      val commitExpectedVersions = Seq(
        "8ea4e87f202fbcc369bec9615e7ddf2c14b39e9d" -> "0.2.0-1-g8ea4e87-SNAPSHOT",
        "v0.2.0"                                   -> "0.2.0",
        "698893f0a4cb1e758cbc8f748827daaf6c7b36d0" -> "0.0.1-SNAPSHOT"
      )

      for ((commit, expectedVersion) <- commitExpectedVersions) {
        os.proc("git", "checkout", commit)
          .call(cwd = dir, stdin = os.Inherit, stdout = os.Inherit)
        val version = cv.get(dir)
          .fold(ex => throw new Exception(ex), identity)
        expect(version == expectedVersion)
      }
    }
  }

  test("git tag on empty repo") {
    TestInputs().fromRoot { root =>
      val git     = Git.init().setDirectory(root.toIO).call()
      val hasHead = git.getRepository.resolve(Constants.HEAD) != null
      expect(!hasHead)

      val defaultVersion = "0.0.2-SNAPSHOT"
      val cv             = ComputeVersion.GitTag(os.rel, true, Nil, defaultVersion)

      val version = cv.get(root)
        .fold(ex => throw new Exception(ex), identity)
      expect(version == defaultVersion)
    }
  }

}
