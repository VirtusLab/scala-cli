package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.nio.file.Files

import scala.util.Properties

class JmhTests extends ScalaCliSuite with JmhSuite {
  override def group: ScalaCliSuite.TestGroup = ScalaCliSuite.TestGroup.First

  test("simple") {
    // TODO extract running benchmarks to a separate scope, or a separate sub-command
    simpleInputs.fromRoot { root =>
      val res =
        os.proc(TestUtil.cli, "--power", TestUtil.extraOptions, ".", "--jmh").call(cwd = root)
      val output = res.out.trim()
      expect(output.contains(expectedInOutput))
    }
  }

  test("compile") {
    simpleInputs.fromRoot { root =>
      os.proc(TestUtil.cli, "compile", "--power", TestUtil.extraOptions, ".", "--jmh")
        .call(cwd = root)
    }
  }

  test("doc") {
    simpleInputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "doc", "--power", TestUtil.extraOptions, ".", "--jmh")
        .call(cwd = root, stderr = os.Pipe)
      expect(!res.err.trim().contains("Error"))
    }
  }

  test("setup-ide") {
    // TODO fix setting jmh via a reload & add tests for it
    simpleInputs.fromRoot { root =>
      os.proc(TestUtil.cli, "setup-ide", "--power", TestUtil.extraOptions, ".", "--jmh")
        .call(cwd = root)
    }
  }

  test("package") {
    // TODO make package with --jmh build an artifact that actually runs benchmarks
    val expectedMessage = "Placeholder main method"
    simpleInputs
      .add(os.rel / "Main.scala" -> s"""@main def main: Unit = println("$expectedMessage")""")
      .fromRoot { root =>
        val launcherName = {
          val ext = if (Properties.isWin) ".bat" else ""
          "launcher" + ext
        }
        os.proc(
          TestUtil.cli,
          "package",
          "--power",
          TestUtil.extraOptions,
          ".",
          "--jmh",
          "-o",
          launcherName
        )
          .call(cwd = root)
        val launcher = root / launcherName
        expect(os.isFile(launcher))
        expect(Files.isExecutable(launcher.toNIO))
        val output = TestUtil.maybeUseBash(launcher)(cwd = root).out.trim()
        expect(output == expectedMessage)
      }
  }

  test("export") {
    simpleInputs.fromRoot { root =>
      // TODO add proper support for JMH export, we're checking if it doesn't fail the command for now
      os.proc(TestUtil.cli, "export", "--power", TestUtil.extraOptions, ".", "--jmh")
        .call(cwd = root)
    }
  }

}
