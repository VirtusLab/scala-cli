package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.File

class StandaloneLauncherTests extends ScalaCliSuite {

  if (TestUtil.isJvmCli)
    test(s"Standalone launcher should run with java 8") {
      // It should download Java 17 and use it to run itself
      val message = "Hello World"
      val inputs = TestInputs(
        os.rel / "hello.sc" -> s"""println("$message")"""
      )
      inputs.fromRoot { root =>
        val java8Home =
          os.Path(os.proc(TestUtil.cs, "java-home", "--jvm", "zulu:8").call().out.trim(), os.pwd)

        val extraEnv = Map(
          "JAVA_HOME" -> java8Home.toString,
          "PATH"      -> ((java8Home / "bin").toString + File.pathSeparator + System.getenv("PATH"))
        )

        val output =
          os.proc(TestUtil.cli, ".").call(cwd = root, env = extraEnv).out.trim()

        expect(output == message)
      }
    }
}
