package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.File

import scala.util.Properties

class ConfigTests extends ScalaCliSuite {

  override def group: ScalaCliSuite.TestGroup = ScalaCliSuite.TestGroup.First

  test("simple") {
    val configFile = os.rel / "config" / "config.json"
    val configEnv  = Map("SCALA_CLI_CONFIG" -> configFile.toString())
    val name       = "Alex"
    TestInputs.empty.fromRoot { root =>
      val before =
        os.proc(TestUtil.cli, "--power", "config", "publish.user.name").call(
          cwd = root,
          env = configEnv
        )
      expect(before.out.trim().isEmpty)

      os.proc(TestUtil.cli, "--power", "config", "publish.user.name", name).call(
        cwd = root,
        env = configEnv
      )
      val res =
        os.proc(TestUtil.cli, "--power", "config", "publish.user.name").call(
          cwd = root,
          env = configEnv
        )
      expect(res.out.trim() == name)

      os.proc(TestUtil.cli, "--power", "config", "publish.user.name", "--unset").call(
        cwd = root,
        env = configEnv
      )
      val after =
        os.proc(TestUtil.cli, "--power", "config", "publish.user.name").call(
          cwd = root,
          env = configEnv
        )
      expect(after.out.trim().isEmpty)
    }
  }

  test("password") {
    val configFile = os.rel / "config" / "config.json"
    val configEnv  = Map("SCALA_CLI_CONFIG" -> configFile.toString)
    val password   = "1234"
    val key        = "httpProxy.password"
    TestInputs.empty.fromRoot { root =>

      def emptyCheck(): Unit = {
        val value = os.proc(TestUtil.cli, "--power", "config", key)
          .call(cwd = root, env = configEnv)
        expect(value.out.trim().isEmpty)
      }

      def unset(): Unit =
        os.proc(TestUtil.cli, "--power", "config", key, "--unset")
          .call(cwd = root, env = configEnv)

      def read(): String = {
        val res = os.proc(TestUtil.cli, "--power", "config", key)
          .call(cwd = root, env = configEnv)
        res.out.trim()
      }
      def readDecoded(env: Map[String, String] = Map.empty): String = {
        val res = os.proc(TestUtil.cli, "--power", "config", key, "--password-value")
          .call(cwd = root, env = configEnv ++ env)
        res.out.trim()
      }

      emptyCheck()

      os.proc(TestUtil.cli, "--power", "config", key, s"value:$password")
        .call(cwd = root, env = configEnv)
      expect(read() == s"value:$password")
      expect(readDecoded() == password)
      unset()
      emptyCheck()

      os.proc(TestUtil.cli, "--power", "config", key, "env:MY_PASSWORD")
        .call(cwd = root, env = configEnv)
      expect(read() == "env:MY_PASSWORD")
      expect(readDecoded(env = Map("MY_PASSWORD" -> password)) == password)
      unset()
      emptyCheck()

      os.proc(
        TestUtil.cli,
        "--power",
        "config",
        key,
        "env:MY_PASSWORD",
        "--password-value"
      )
        .call(cwd = root, env = Map("MY_PASSWORD" -> password) ++ configEnv)
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

      val res = os.proc(TestUtil.cli, "--power", "config", "httpProxy.address")
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

      val res = os.proc(TestUtil.cli, "--power", "config", "httpProxy.address", proxyAddr)
        .call(cwd = root, env = extraEnv, check = false, mergeErrIntoOut = true)
      val output = res.out.trim()
      expect(output.contains(" has wrong permissions"))
    }
  }

  if (!TestUtil.isCI || !Properties.isWin)
    for (pgpPasswordOption <- List("none", "random", "MY_CHOSEN_PASSWORD"))
      test(s"Create a default PGP key, password: $pgpPasswordOption") {
        createDefaultPgpKeyTest(pgpPasswordOption)
      }

  if (TestUtil.isNativeCli)
    test(s"Create a PGP key with external JVM process, java version too low") {
      TestInputs().fromRoot { root =>
        val configFile = {
          val dir = root / "config"
          os.makeDir.all(dir, perms = if (Properties.isWin) null else "rwx------")
          dir / "config.json"
        }

        val java8Home =
          os.Path(os.proc(TestUtil.cs, "java-home", "--jvm", "zulu:8").call().out.trim(), os.pwd)

        val extraEnv = Map(
          "JAVA_HOME" -> java8Home.toString,
          "PATH" -> ((java8Home / "bin").toString + File.pathSeparator + System.getenv("PATH")),
          "SCALA_CLI_CONFIG" -> configFile.toString
        )

        val pgpCreated = os.proc(
          TestUtil.cli,
          "--power",
          "config",
          "--create-pgp-key",
          "--email",
          "alex@alex.me",
          "--pgp-password",
          "none",
          "--force-jvm-signing-cli",
          "-v",
          "-v",
          "-v"
        )
          .call(cwd = root, env = extraEnv, mergeErrIntoOut = true)

        val javaCommandLine = pgpCreated.out.text()
          .linesIterator
          .dropWhile(!_.equals("  Running")).slice(1, 2)
          .toSeq

        expect(javaCommandLine.nonEmpty)
        expect(javaCommandLine.head.contains("17"))

        val passwordInConfig = os.proc(TestUtil.cli, "--power", "config", "pgp.secret-key-password")
          .call(cwd = root, env = extraEnv, stderr = os.Pipe)
        expect(passwordInConfig.out.text().isEmpty())

        val secretKey = os.proc(TestUtil.cli, "--power", "config", "pgp.secret-key")
          .call(cwd = root, env = extraEnv, stderr = os.Pipe)
          .out.trim()
        val rawPublicKey =
          os.proc(TestUtil.cli, "--power", "config", "pgp.public-key", "--password-value")
            .call(cwd = root, env = extraEnv, stderr = os.Pipe)
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
          "--power",
          "pgp",
          "sign",
          "--secret-key",
          maybeEscape(secretKey),
          tmpFile
        ).call(cwd = root, stdin = os.Inherit, stdout = os.Inherit, env = extraEnv)

        val pubKeyFile = root / "key.pub"
        os.write(pubKeyFile, rawPublicKey)
        val verifyResult =
          os.proc(TestUtil.cli, "--power", "pgp", "verify", "--key", pubKeyFile, tmpFileAsc)
            .call(cwd = root, env = extraEnv, mergeErrIntoOut = true)

        expect(verifyResult.out.text().contains("valid signature"))
      }

    }

  def createDefaultPgpKeyTest(pgpPasswordOption: String): Unit = {
    TestInputs().fromRoot { root =>
      val configFile = {
        val dir = root / "config"
        os.makeDir.all(dir, perms = if (Properties.isWin) null else "rwx------")
        dir / "config.json"
      }
      val extraEnv = Map(
        "SCALA_CLI_CONFIG" -> configFile.toString
      )
      val checkPassword = os.proc(TestUtil.cli, "--power", "config", "--create-pgp-key")
        .call(cwd = root, env = extraEnv, check = false, mergeErrIntoOut = true)
      expect(checkPassword.exitCode != 0)
      expect(checkPassword.out.text().contains("--pgp-password"))

      val checkEmail = os.proc(
        TestUtil.cli,
        "--power",
        "config",
        "--create-pgp-key",
        "--pgp-password",
        pgpPasswordOption
      )
        .call(cwd = root, env = extraEnv, check = false, mergeErrIntoOut = true)
      expect(checkEmail.exitCode != 0)
      expect(checkEmail.out.text().contains("--email"))

      val pgpCreated = os.proc(
        TestUtil.cli,
        "--power",
        "config",
        "--create-pgp-key",
        "--email",
        "alex@alex.me",
        "--pgp-password",
        pgpPasswordOption
      )
        .call(cwd = root, env = extraEnv, mergeErrIntoOut = true)

      val pgpPasswordOpt: Option[String] = pgpCreated.out.text()
        .linesIterator
        .toSeq
        .find(_.startsWith("Password"))
        .map(_.stripPrefix("Password:").trim())

      if (pgpPasswordOption != "random")
        expect(pgpPasswordOpt.isEmpty)
      else
        expect(pgpPasswordOpt.isDefined)

      val passwordInConfig = os.proc(TestUtil.cli, "--power", "config", "pgp.secret-key-password")
        .call(cwd = root, env = extraEnv, stderr = os.Pipe)
      expect(passwordInConfig.out.text().isEmpty())

      val secretKey = os.proc(TestUtil.cli, "--power", "config", "pgp.secret-key")
        .call(cwd = root, env = extraEnv, stderr = os.Pipe)
        .out.trim()
      val rawPublicKey =
        os.proc(TestUtil.cli, "--power", "config", "pgp.public-key", "--password-value")
          .call(cwd = root, env = extraEnv, stderr = os.Pipe)
          .out.trim()

      val tmpFile    = root / "test-file"
      val tmpFileAsc = root / "test-file.asc"
      os.write(tmpFile, "Hello")

      val q = "\""
      def maybeEscape(arg: String): String =
        if (Properties.isWin) q + arg + q
        else arg
      val signProcess = if (pgpPasswordOption != "none")
        os.proc(
          TestUtil.cli,
          "--power",
          "pgp",
          "sign",
          "--password",
          s"value:${maybeEscape(pgpPasswordOpt.getOrElse("MY_CHOSEN_PASSWORD"))}",
          "--secret-key",
          maybeEscape(secretKey),
          tmpFile
        )
      else
        os.proc(
          TestUtil.cli,
          "--power",
          "pgp",
          "sign",
          "--secret-key",
          maybeEscape(secretKey),
          tmpFile
        )
      signProcess.call(cwd = root, stdin = os.Inherit, stdout = os.Inherit, env = extraEnv)

      val pubKeyFile = root / "key.pub"
      os.write(pubKeyFile, rawPublicKey)
      val verifyResult =
        os.proc(TestUtil.cli, "--power", "pgp", "verify", "--key", pubKeyFile, tmpFileAsc)
          .call(cwd = root, env = extraEnv, mergeErrIntoOut = true)

      expect(verifyResult.out.text().contains("valid signature"))
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
        s"""//> using dep "$testOrg::$testName:$testVersion"
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
        "--power",
        "publish",
        "--publish-repo",
        repoPath.toNIO.toUri.toASCIIString,
        "messages",
        "--organization",
        testOrg,
        "--name",
        testName,
        "--project-version",
        testVersion
      )
        .call(cwd = root, stdin = os.Inherit, stdout = os.Inherit, env = extraEnv)

      TestUtil.serveFilesInHttpServer(repoPath, user, password, realm) { (host, port) =>
        os.proc(
          TestUtil.cli,
          "--power",
          "config",
          "repositories.credentials",
          host,
          s"value:$user",
          s"value:$password",
          realm
        )
          .call(cwd = root, stdin = os.Inherit, stdout = os.Inherit, env = extraEnv)
        val credentialsAsStringRes = os.proc(
          TestUtil.cli,
          "--power",
          "config",
          "repositories.credentials"
        ).call(cwd = root, env = extraEnv)
        val linePrefix = "configRepo0"
        val expectedCredentialsAsString =
          s"""$linePrefix.host=$host
             |$linePrefix.username=value:$user
             |$linePrefix.password=value:$password
             |$linePrefix.realm=$realm
             |$linePrefix.auto=true""".stripMargin
        expect(credentialsAsStringRes.out.trim() == expectedCredentialsAsString)
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

  test("password-value in credentials") {
    val configFile         = os.rel / "config" / "config.json"
    val passwordEnvVarName = "REPO_PASSWORD"
    val userEnvVarName     = "REPO_USER"
    val password           = "1234"
    val user               = "user"
    val envVars = Map(
      userEnvVarName     -> user,
      passwordEnvVarName -> password
    )
    val configEnv = Map("SCALA_CLI_CONFIG" -> configFile.toString)

    val keys = List("repositories.credentials", "publish.credentials")
    TestInputs.empty.fromRoot { root =>
      for (key <- keys) {
        os.proc(
          TestUtil.cli,
          "--power",
          "config",
          key,
          "s1.oss.sonatype.org",
          s"env:$userEnvVarName",
          s"env:$passwordEnvVarName",
          "--password-value"
        )
          .call(cwd = root, env = configEnv ++ envVars)
        val credsFromConfig = os.proc(TestUtil.cli, "--power", "config", key)
          .call(cwd = root, env = configEnv)
          .out.trim()
        expect(credsFromConfig.contains(password))
        expect(credsFromConfig.contains(user))
      }
    }
  }

  for (
    (entryType, key, valuesPlural, invalidValue) <- Seq(
      ("boolean", "power", Seq("true", "false", "true"), "true."),
      ("string", "publish.user.name", Seq("abc", "def", "xyz"), ""),
      ("password", "httpProxy.password", Seq("value:pass1", "value:pass2", "value:pass3"), "pass")
    )
  ) {
    test(s"print a meaningful error when multiple values are passed for a $entryType key: $key") {
      val configFile = os.rel / "config" / "config.json"
      val env        = Map("SCALA_CLI_CONFIG" -> configFile.toString)
      TestInputs.empty.fromRoot { root =>
        val res = os.proc(TestUtil.cli, "--power", "config", key, valuesPlural)
          .call(cwd = root, env = env, stderr = os.Pipe, check = false)
        expect(res.exitCode == 1)
        expect(res.err.trim().contains(s"expected a single $entryType value"))
      }
    }

    if (entryType != "string")
      test(s"print a meaningful error when an invalid value is passed for a $entryType key: $key") {
        val configFile = os.rel / "config" / "config.json"
        val env        = Map("SCALA_CLI_CONFIG" -> configFile.toString)
        TestInputs.empty.fromRoot { root =>
          val res = os.proc(TestUtil.cli, "--power", "config", key, invalidValue)
            .call(cwd = root, env = env, stderr = os.Pipe, check = false)
          expect(res.exitCode == 1)
          expect(res.err.trim().contains("Malformed"))
          expect(res.err.trim().contains(invalidValue))
        }
      }
  }

  test("change value for key") {
    val configFile              = os.rel / "config" / "config.json"
    val configEnv               = Map("SCALA_CLI_CONFIG" -> configFile.toString)
    val (props, props2, props3) = ("props=test", "props2=test2", "props3=test3")
    val key                     = "java.properties"
    TestInputs.empty.fromRoot { root =>
      // set some values first time
      os.proc(TestUtil.cli, "--power", "config", key, props, props2).call(
        cwd = root,
        env = configEnv
      )

      // override some values should throw error without force flag
      val res = os.proc(TestUtil.cli, "--power", "config", key, props, props2, props3).call(
        cwd = root,
        env = configEnv,
        check = false,
        mergeErrIntoOut = true
      )

      expect(res.exitCode == 1)
      expect(res.out.trim().contains("pass -f or --force"))

      os.proc(TestUtil.cli, "--power", "config", key, props, props2, props3, "-f").call(
        cwd = root,
        env = configEnv,
        check = false
      )
      val propertiesFromConfig = os.proc(TestUtil.cli, "--power", "config", key)
        .call(cwd = root, env = configEnv)
        .out.trim()

      expect(propertiesFromConfig.contains(props))
      expect(propertiesFromConfig.contains(props2))
      expect(propertiesFromConfig.contains(props3))
    }
  }

}
