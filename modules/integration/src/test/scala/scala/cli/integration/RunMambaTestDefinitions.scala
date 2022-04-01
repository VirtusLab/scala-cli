package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

abstract class RunMambaTestDefinitions(val scalaVersionOpt: Option[String])
    extends munit.FunSuite with TestScalaVersionArgs {

  private lazy val extraOptions = scalaVersionArgs ++ TestUtil.extraOptions

  private val ciOpt   = Option(System.getenv("CI")).map(v => Seq("-e", s"CI=$v")).getOrElse(Nil)
  private val termOpt = if (System.console() == null) Nil else Seq("-t")

  def dockerTest(): Unit = {
    val inputs = TestInputs(
      Seq(
        os.rel / "Hello.scala" ->
          """//> using platform "native"
            |object Hello {
            |  def main(args: Array[String]): Unit =
            |    println("Hello from native")
            |}
            |""".stripMargin,
        os.rel / "run.sh" ->
          s"""#!/usr/bin/env bash
             |set -e
             |
             |apt-get update
             |apt-get install -y curl
             |
             |curl -fLo cs.gz https://github.com/coursier/coursier/releases/download/v${Constants.csVersion}/cs-x86_64-pc-linux.gz
             |gzip -d cs.gz
             |chmod +x cs
             |
             |eval "$$(./cs java --env --jvm zulu:17)"
             |
             |./scala-cli ${extraOptions.map("\"" + _ + "\"").mkString(" ")} --server=false --put-platform-config-in-bloop-file=false --use-managed-clang . > output
             |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      os.copy(os.Path(TestUtil.cliPath, os.pwd), root / "scala-cli")
      os.perms.set(root / "run.sh", "rwxr-xr-x")

      // format: on
      val proc = os.proc(
        "docker",
        "run",
        "--rm",
        ciOpt,
        termOpt,
        "-v",
        s"$root:/data",
        "-w",
        "/data",
        "ubuntu:20.04",
        "/bin/bash",
        "./run.sh"
      )
      // format: off
      proc.call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)
      val output = os.read(root / "output")
      expect(output.trim() == "Hello from native")
    }
  }

  if (!TestUtil.isNativeCli)
    test("docker") {
      dockerTest()
    }

}