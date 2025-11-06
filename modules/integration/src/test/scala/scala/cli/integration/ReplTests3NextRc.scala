package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class ReplTests3NextRc extends ReplTestDefinitions with Test3NextRc {
  test("run hello world from the 3.8 REPL") {
    TestInputs.empty.fromRoot { root =>
      val expectedMessage = "1337"
      val code            = s"""println($expectedMessage)"""
      val r               = os.proc(
        TestUtil.cli,
        "repl",
        "--repl-quit-after-init",
        "--repl-init-script",
        code,
        "-S",
        // TODO: switch this test to 3.8.0-RC1 once it's out
        "3.8.0-RC1-bin-20251104-b83b3d9-NIGHTLY",
        TestUtil.extraOptions
      )
        .call(cwd = root)
      expect(r.out.trim() == expectedMessage)
    }
  }
}
