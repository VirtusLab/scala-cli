package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

class ConfigTests extends ScalaCliSuite {

  override def group: ScalaCliSuite.TestGroup = ScalaCliSuite.TestGroup.First

  test("simple") {
    val homeDir = os.rel / "home"
    val homeEnv = Map("SCALA_CLI_HOME" -> homeDir.toString())
    val name    = "Alex"
    TestInputs.empty.fromRoot { root =>
      val before =
        os.proc(TestUtil.cli, "config", "publish.user.name").call(cwd = root, env = homeEnv)
      expect(before.out.trim().isEmpty)

      os.proc(TestUtil.cli, "config", "publish.user.name", name).call(cwd = root, env = homeEnv)
      val res = os.proc(TestUtil.cli, "config", "publish.user.name").call(cwd = root, env = homeEnv)
      expect(res.out.trim() == name)

      os.proc(TestUtil.cli, "config", "publish.user.name", "--unset").call(
        cwd = root,
        env = homeEnv
      )
      val after =
        os.proc(TestUtil.cli, "config", "publish.user.name").call(cwd = root, env = homeEnv)
      expect(after.out.trim().isEmpty)
    }
  }

  test("password") {
    val homeDir  = os.rel / "home"
    val homeEnv  = Map("SCALA_CLI_HOME" -> homeDir.toString)
    val password = "1234"
    val key      = "httpProxy.password"
    TestInputs.empty.fromRoot { root =>

      def emptyCheck(): Unit = {
        val value = os.proc(TestUtil.cli, "config", key)
          .call(cwd = root, env = homeEnv)
        expect(value.out.trim().isEmpty)
      }

      def unset(): Unit =
        os.proc(TestUtil.cli, "config", key, "--unset")
          .call(cwd = root, env = homeEnv)

      def read(): String = {
        val res = os.proc(TestUtil.cli, "config", key)
          .call(cwd = root, env = homeEnv)
        res.out.trim()
      }
      def readDecoded(env: Map[String, String] = Map.empty): String = {
        val res = os.proc(TestUtil.cli, "config", key, "--password")
          .call(cwd = root, env = homeEnv ++ env)
        res.out.trim()
      }

      emptyCheck()

      os.proc(TestUtil.cli, "config", key, s"value:$password")
        .call(cwd = root, env = homeEnv)
      expect(read() == s"value:$password")
      expect(readDecoded() == password)
      unset()
      emptyCheck()

      os.proc(TestUtil.cli, "config", key, "env:MY_PASSWORD")
        .call(cwd = root, env = homeEnv)
      expect(read() == "env:MY_PASSWORD")
      expect(readDecoded(env = Map("MY_PASSWORD" -> password)) == password)
      unset()
      emptyCheck()

      os.proc(
        TestUtil.cli,
        "config",
        key,
        "env:MY_PASSWORD",
        "--password-value"
      )
        .call(cwd = root, env = Map("MY_PASSWORD" -> password) ++ homeEnv)
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

      val extraEnv =
        Map("SCALA_CLI_CONFIG" -> confFile.toString) ++
          TestUtil.putCsInPathViaEnv(root / "bin")

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

  if (!Properties.isWin)
    test("Exit with non-zero error code if saving failed") {
      nonZeroErrorCodeOnFailedSaveTest()
    }
  def nonZeroErrorCodeOnFailedSaveTest(): Unit = {
    val proxyAddr = "https://foo.bar.com"
    TestInputs().fromRoot { root =>
      val confDir = root / "config"
      os.makeDir.all(confDir) // not adjusting perms - should make things fail below

      val confFile = confDir / "test-config.json"
      val extraEnv = Map("SCALA_CLI_CONFIG" -> confFile.toString)

      val res = os.proc(TestUtil.cli, "config", "httpProxy.address", proxyAddr)
        .call(cwd = root, env = extraEnv, check = false, mergeErrIntoOut = true)
      val output = res.out.trim()
      expect(output.contains(" has wrong permissions"))
    }
  }

  if (!TestUtil.isCI || !Properties.isWin)
    test("Create a default PGP key") {
      createDefaultPgpKeyTest()
    }
  def createDefaultPgpKeyTest(): Unit = {
    TestInputs().fromRoot { root =>
      val configFile = {
        val dir = root / "config"
        os.makeDir.all(dir, perms = if (Properties.isWin) null else "rwx------")
        dir / "config.json"
      }
      val extraEnv = Map(
        "SCALA_CLI_CONFIG" -> configFile.toString
      )
      val checkRes = os.proc(TestUtil.cli, "config", "--create-pgp-key")
        .call(cwd = root, env = extraEnv, check = false, mergeErrIntoOut = true)
      expect(checkRes.exitCode != 0)
      expect(checkRes.out.text().contains("--email"))
      os.proc(TestUtil.cli, "config", "--create-pgp-key", "--email", "alex@alex.me")
        .call(cwd = root, env = extraEnv, stdin = os.Inherit, stdout = os.Inherit)

      val password = os.proc(TestUtil.cli, "config", "pgp.secret-key-password")
        .call(cwd = root, env = extraEnv)
        .out.trim()
      val secretKey = os.proc(TestUtil.cli, "config", "pgp.secret-key")
        .call(cwd = root, env = extraEnv)
        .out.trim()
      val rawPublicKey = os.proc(TestUtil.cli, "config", "pgp.public-key", "--password")
        .call(cwd = root, env = extraEnv)
        .out.trim()

      val tmpFile    = root / "test-file"
      val tmpFileAsc = root / "test-file.asc"
      os.write(tmpFile, "Hello")

      val q = "\""
      def maybeEscape(arg: String): String =
        if (Properties.isWin) q + arg + q
        else arg
      os.proc(
        TestUtil.cli,
        "pgp",
        "sign",
        "--password",
        maybeEscape(password),
        "--secret-key",
        maybeEscape(secretKey),
        tmpFile
      )
        .call(cwd = root, stdin = os.Inherit, stdout = os.Inherit, env = extraEnv)

      val pubKeyFile = root / "key.pub"
      os.write(pubKeyFile, rawPublicKey)
      os.proc(TestUtil.cli, "pgp", "verify", "--key", pubKeyFile, tmpFileAsc)
        .call(cwd = root, stdin = os.Inherit, stdout = os.Inherit, env = extraEnv)
    }
  }

  test("repository credentials") {
    val testOrg     = "test-org"
    val testName    = "the-messages"
    val testVersion = "0.1.2"
    val user        = "alex"
    val password    = "1234"
    val realm       = "LeTestRealm"
    val inputs = TestInputs(
      os.rel / "messages" / "Messages.scala" ->
        """package messages
          |
          |object Messages {
          |  def hello(name: String): String =
          |    s"Hello $name"
          |}
          |""".stripMargin,
      os.rel / "hello" / "Hello.scala" ->
        s"""//> using lib "$testOrg::$testName:$testVersion"
           |import messages.Messages
           |object Hello {
           |  def main(args: Array[String]): Unit =
           |    println(Messages.hello(args.headOption.getOrElse("Unknown")))
           |}
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val configFile = {
        val dir = root / "conf"
        os.makeDir.all(dir, if (Properties.isWin) null else "rwx------")
        dir / "config.json"
      }
      val extraEnv = Map(
        "SCALA_CLI_CONFIG" -> configFile.toString
      )
      val repoPath = root / "the-repo"
      os.proc(
        TestUtil.cli,
        "publish",
        "--publish-repo",
        repoPath.toNIO.toUri.toASCIIString,
        "messages",
        "--organization",
        testOrg,
        "--name",
        testName,
        "--version",
        testVersion
      )
        .call(cwd = root, stdin = os.Inherit, stdout = os.Inherit, env = extraEnv)

      TestUtil.serveFilesInHttpServer(repoPath, user, password, realm) { (host, port) =>
        os.proc(
          TestUtil.cli,
          "config",
          "repositories.credentials",
          host,
          s"value:$user",
          s"value:$password",
          realm
        )
          .call(cwd = root, stdin = os.Inherit, stdout = os.Inherit, env = extraEnv)
        val res = os.proc(
          TestUtil.cli,
          "run",
          "--repository",
          s"http://$host:$port",
          "hello",
          "--",
          "TestUser"
        )
          .call(cwd = root, env = extraEnv)
        val output = res.out.trim()
        expect(output == "Hello TestUser")
      }
    }
  }

}
