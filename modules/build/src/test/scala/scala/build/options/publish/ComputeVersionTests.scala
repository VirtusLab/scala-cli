package scala.build.options.publish

import com.eed3si9n.expecty.Expecty.expect

import scala.build.tests.{TestInputs, TestUtil}

class ComputeVersionTests extends munit.FunSuite {

  test("command") {
    val ver = "1.2.3"
    val inputs = TestInputs(
      os.rel / "version" -> ver
    )
    inputs.fromRoot { root =>
      val cv = ComputeVersion.Command(Seq("cat", "version"))
      val readVersion = cv.get(root)
        .fold(ex => throw new Exception(ex), identity)
      expect(readVersion == ver)
    }
  }

  test("git tag") {
    TestInputs().fromRoot { root =>
      val ghRepo = "scala-cli/compute-version-test"
      val repo =
        if (TestUtil.isCI) s"https://git@github.com/$ghRepo.git"
        else s"https://github.com/$ghRepo.git"
      os.proc("git", "clone", repo)
        .call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)
      val dir = root / "compute-version-test"
      val cv  = ComputeVersion.GitTag(os.rel, true, "0.0.1-SNAPSHOT")

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

}
