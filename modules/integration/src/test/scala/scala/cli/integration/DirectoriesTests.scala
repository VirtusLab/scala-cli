package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

class DirectoriesTests extends ScalaCliSuite {
  test("running directories with args should fail") {
    TestInputs(os.rel / "s.sc" -> """println("Hello")""").fromRoot { root =>
      val r = os.proc(TestUtil.cli, "--power", "directories", "s.sc")
        .call(cwd = root, stderr = os.Pipe, check = false)
      expect(r.exitCode == 1)
      expect(r.err.trim() == "The directories command doesn't accept arguments.")
    }
  }

  if (Properties.isMac)
    test("running directories on Mac with no args should give valid results") {
      TestInputs.empty.fromRoot { root =>
        val r              = os.proc(TestUtil.cli, "--power", "directories").call(cwd = root)
        val cachesPath     = os.home / "Library" / "Caches" / "ScalaCli"
        val appSupportPath = os.home / "Library" / "Application Support" / "ScalaCli"
        val expectedOutput =
          s"""Local repository: ${cachesPath / "local-repo"}
             |Completions: ${appSupportPath / "completions"}
             |Virtual projects: ${cachesPath / "virtual-projects"}
             |BSP sockets: ${cachesPath / "bsp-sockets"}
             |Bloop daemon directory: ${cachesPath / "bloop" / "daemon"}
             |Secrets directory: ${appSupportPath / "secrets"}""".stripMargin
        expect(r.out.trim() == expectedOutput)
      }
    }
}
