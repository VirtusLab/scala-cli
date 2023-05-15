package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

class CompleteTests extends ScalaCliSuite {

  test("simple") {
    TestInputs.empty.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "complete", shellFormat, "2", "com").call(cwd = root)
      expect(res.exitCode == 0)
      expect(res.out.trim().nonEmpty)
    }
  }

  test("zsh bug") {
    // guard against https://github.com/alexarchambault/case-app/issues/475
    TestInputs.empty.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "complete", "zsh-v1", "2", "scala-cli").call(cwd = root)
      expect(res.exitCode == 0)
      expect(!res.out.text().contains(raw"\'"))
    }
  }

  def shellFormat: String =
    if (Properties.isMac) "zsh-v1"
    else "bash-v1"
}
