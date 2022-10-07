package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

class ConfigTests extends ScalaCliSuite {

  override def group: ScalaCliSuite.TestGroup = ScalaCliSuite.TestGroup.First

  test("simple") {
    val homeDir    = os.rel / "home"
    val dirOptions = Seq[os.Shellable]("--home-directory", homeDir)
    val name       = "Alex"
    TestInputs.empty.fromRoot { root =>
      val before = os.proc(TestUtil.cli, "config", dirOptions, "user.name").call(cwd = root)
      expect(before.out.trim().isEmpty)

      os.proc(TestUtil.cli, "config", dirOptions, "user.name", name).call(cwd = root)
      val res = os.proc(TestUtil.cli, "config", dirOptions, "user.name").call(cwd = root)
      expect(res.out.trim() == name)

      os.proc(TestUtil.cli, "config", dirOptions, "user.name", "--unset").call(cwd = root)
      val after = os.proc(TestUtil.cli, "config", dirOptions, "user.name").call(cwd = root)
      expect(after.out.trim().isEmpty)
    }
  }

  test("password") {
    val homeDir    = os.rel / "home"
    val dirOptions = Seq[os.Shellable]("--home-directory", homeDir)
    val password   = "1234"
    TestInputs.empty.fromRoot { root =>

      def emptyCheck(): Unit = {
        val value = os.proc(TestUtil.cli, "config", dirOptions, "sonatype.password")
          .call(cwd = root)
        expect(value.out.trim().isEmpty)
      }

      def unset(): Unit =
        os.proc(TestUtil.cli, "config", dirOptions, "sonatype.password", "--unset")
          .call(cwd = root)

      def read(): String = {
        val res = os.proc(TestUtil.cli, "config", dirOptions, "sonatype.password")
          .call(cwd = root)
        res.out.trim()
      }
      def readDecoded(env: Map[String, String] = null): String = {
        val res = os.proc(TestUtil.cli, "config", dirOptions, "sonatype.password", "--password")
          .call(cwd = root, env = env)
        res.out.trim()
      }

      emptyCheck()

      os.proc(TestUtil.cli, "config", dirOptions, "sonatype.password", s"value:$password")
        .call(cwd = root)
      expect(read() == s"value:$password")
      expect(readDecoded() == password)
      unset()
      emptyCheck()

      os.proc(TestUtil.cli, "config", dirOptions, "sonatype.password", "env:MY_PASSWORD")
        .call(cwd = root)
      expect(read() == "env:MY_PASSWORD")
      expect(readDecoded(env = Map("MY_PASSWORD" -> password)) == password)
      unset()
      emptyCheck()

      os.proc(
        TestUtil.cli,
        "config",
        dirOptions,
        "sonatype.password",
        "env:MY_PASSWORD",
        "--password-value"
      )
        .call(cwd = root, env = Map("MY_PASSWORD" -> password))
      expect(read() == s"value:$password")
      expect(readDecoded() == password)
      unset()
      emptyCheck()
    }
  }

  test("Respect SCALA_CLI_CONFIG and format on write") {
    val proxyAddr = "https://foo.bar.com"
    TestInputs().fromRoot { root =>
      val confDir  = root / "config"
      val confFile = confDir / "test-config.json"
      val content =
        // non-formatted on purpose
        s"""{
           |  "httpProxy": {  "address" :      "$proxyAddr"     } }
           |""".stripMargin
      os.write(confFile, content, createFolders = true)

      if (!Properties.isWin)
        os.perms.set(confDir, "rwx------")

      val extraEnv = Map("SCALA_CLI_CONFIG" -> confFile.toString)

      val res = os.proc(TestUtil.cli, "config", "httpProxy.address")
        .call(cwd = root, env = extraEnv)
      val value = res.out.trim()
      expect(value == proxyAddr)

      os.proc(TestUtil.cli, "config", "interactive", "false")
        .call(cwd = root, env = extraEnv)

      val expectedUpdatedContent =
        // too many spaces after some ':' (jsoniter-scala bug?)
        s"""{
           |  "httpProxy": {
           |    "address":       "https://foo.bar.com"
           |  },
           |  "interactive": false
           |}
           |""".stripMargin.replace("\r\n", "\n")
      val updatedContent = os.read(confFile)
      expect(updatedContent == expectedUpdatedContent)
    }
  }

}
