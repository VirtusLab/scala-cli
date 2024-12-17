package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

trait RunJdkTestDefinitions { _: RunTestDefinitions =>
  def javaIndex(javaVersion: Int): String =
    // TODO just passing the version number on arm64 should be enough, needs a fix in cs
    if (Properties.isMac && TestUtil.isM1 && (javaVersion < 11 || javaVersion == 16))
      s"zulu:$javaVersion"
    else javaVersion.toString

  for {
    javaVersion <- Constants.allJavaVersions
    index = javaIndex(javaVersion)
  } {
    test(s"correct JVM is picked up when JAVA_HOME set to $index") {
      TestUtil.retryOnCi() {
        TestInputs(
          os.rel / "check_java_home.sc" ->
            s"""assert(
               |  System.getProperty("java.version").startsWith("$javaVersion") ||
               |  System.getProperty("java.version").startsWith("1.$javaVersion")
               |)
               |println(System.getProperty("java.home"))""".stripMargin
        ).fromRoot { root =>
          val javaHome =
            os.Path(
              os.proc(TestUtil.cs, "java-home", "--jvm", index).call().out.trim(),
              os.pwd
            )
            val res = os.proc(TestUtil.cli, "run", ".", extraOptions)
              .call(cwd = root, env = Map("JAVA_HOME" -> javaHome.toString))
            expect(res.out.trim().contains(javaHome.toString))
        }
      }
    }

    test(s"hello world with --jvm $index") {
      val expectedMessage = "Hello, world!"
      TestInputs(
        os.rel / "hello_world.sc" -> s"println(\"$expectedMessage\")"
      ).fromRoot { root =>
        val res = os.proc(TestUtil.cli, "run", ".", extraOptions, "--jvm", javaVersion)
          .call(cwd = root)
        expect(res.out.trim() == expectedMessage)
      }
    }

    test(s"correct JVM is picked up when Java $index is passed with --java-home") {
      TestUtil.retryOnCi() {
        TestInputs(
          os.rel / "check_java_home.sc" ->
            s"""assert(
               |  System.getProperty("java.version").startsWith("$javaVersion") ||
               |  System.getProperty("java.version").startsWith("1.$javaVersion")
               |)
               |println(System.getProperty("java.home"))""".stripMargin
        ).fromRoot { root =>
          val javaHome =
            os.Path(
              os.proc(TestUtil.cs, "java-home", "--jvm", index).call().out.trim(),
              os.pwd
            )
          val res =
            os.proc(TestUtil.cli, "run", ".", extraOptions, "--java-home", javaHome.toString)
              .call(cwd = root)
          expect(res.out.trim().contains(javaHome.toString))
        }
      }
    }

    if (javaVersion >= Constants.bloopMinimumJvmVersion)
      test(s"Bloop runs correctly on JVM $index") {
        TestUtil.retryOnCi() {
          val expectedMessage = "Hello, world!"
          TestInputs(os.rel / "check_java_home.sc" -> s"""println("$expectedMessage")""")
            .fromRoot { root =>
              os.proc(TestUtil.cli, "bloop", "exit", "--power").call(cwd = root)
              val res = os.proc(
                TestUtil.cli,
                "run",
                ".",
                extraOptions,
                "--bloop-jvm",
                index,
                "--jvm",
                index
              )
                .call(cwd = root, stderr = os.Pipe)
              expect(res.err.trim().contains(javaVersion.toString))
              expect(res.out.trim() == expectedMessage)
            }
        }
      }
  }
}
