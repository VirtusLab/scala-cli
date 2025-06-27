package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.ProcOps
import scala.util.{Properties, Try}

trait RunJdkTestDefinitions { _: RunTestDefinitions =>
  def javaIndex(javaVersion: Int): String =
    // TODO just passing the version number on arm64 should be enough, needs a fix in cs
    if (Properties.isMac && TestUtil.isM1 && (javaVersion < 11 || javaVersion == 16))
      s"zulu:$javaVersion"
    else javaVersion.toString

  def canUseScalaInstallationWrapper: Boolean =
    actualScalaVersion.startsWith("3") && actualScalaVersion.split('.').drop(1).head.toInt >= 5

  for {
    javaVersion <- Constants.allJavaVersions
    index = javaIndex(javaVersion)
    useScalaInstallationWrapper <-
      if (canUseScalaInstallationWrapper) Seq(false, true) else Seq(false)
    launcherString = if (useScalaInstallationWrapper) "coursier scala installation" else "Scala CLI"
    scalaRunnerWrapperVersion = actualScalaVersion match {
      case v if v == Constants.scala3NextRc => Constants.scala3NextRcAnnounced
      case v if v == Constants.scala3Next   => Constants.scala3NextAnnounced
      case v                                => v
    }
    withLauncher = (root: os.Path) =>
      (f: Seq[os.Shellable] => Unit) =>
        if (useScalaInstallationWrapper)
          withScalaRunnerWrapper(
            root = root,
            localBin = root / "local-bin",
            scalaVersion = scalaRunnerWrapperVersion,
            shouldCleanUp = false
          )(launcher => f(Seq(launcher)))
        else
          f(Seq(TestUtil.cli))
  } {
    test(s"correct JVM is picked up by $launcherString when JAVA_HOME set to $index") {
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
          withLauncher(root) { launcher =>
            val res = os.proc(launcher, "run", ".", extraOptions)
              .call(cwd = root, env = Map("JAVA_HOME" -> javaHome.toString))
            expect(res.out.trim().contains(javaHome.toString))
          }
        }
      }
    }

    test(s"hello world with $launcherString and --jvm $index") {
      TestUtil.retryOnCi() {
        val expectedMessage = "Hello, world!"
        TestInputs(
          os.rel / "hello_world.sc" -> s"println(\"$expectedMessage\")"
        ).fromRoot { root =>
          withLauncher(root) { launcher =>
            val res = os.proc(launcher, "run", ".", extraOptions, "--jvm", javaVersion)
              .call(cwd = root)
            expect(res.out.trim() == expectedMessage)
          }
        }
      }
    }

    if (!Properties.isWin || !useScalaInstallationWrapper) // TODO make this pass on Windows
      test(
        s"correct JVM is picked up by $launcherString when Java $index is passed with --java-home"
      ) {
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
            withLauncher(root) { launcher =>
              val res =
                os.proc(launcher, "run", ".", extraOptions, "--java-home", javaHome.toString)
                  .call(cwd = root)
              expect(res.out.trim().contains(javaHome.toString))
            }
          }
        }
      }

    if (javaVersion >= Constants.bloopMinimumJvmVersion)
      test(s"Bloop runs correctly with $launcherString on JVM $index") {
        TestUtil.retryOnCi() {
          val expectedMessage = "Hello, world!"
          TestInputs(os.rel / "check_java_home.sc" -> s"""println("$expectedMessage")""")
            .fromRoot { root =>
              os.proc(TestUtil.cli, "bloop", "exit", "--power").call(cwd = root)
              withLauncher(root) { launcher =>
                val res = os.proc(
                  launcher,
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

    // the warnings were introduced in JDK 24, so we only test this for JDKs >= 24
    // the issue never affected Scala 2.12, so we skip it for that version
    if (
      !actualScalaVersion.startsWith("2.12") &&
      !useScalaInstallationWrapper &&
      Try(index.toInt).map(_ >= 24).getOrElse(false)
    )
      // TODO: test with Scala installation wrapper when the fix gets propagated there
      test(s"REPL does not warn about restricted java.lang.System API called on JDK $index") {
        TestInputs.empty.fromRoot { root =>
          TestUtil.withProcessWatching(
            proc = os.proc(TestUtil.cli, "repl", extraOptions, "--jvm", index)
              .spawn(cwd = root, stderr = os.Pipe)
          ) { (proc, _, ec) =>
            proc.printStderrUntilJlineRevertsToDumbTerminal(proc) { s =>
              expect(!s.contains("A restricted method in java.lang.System has been called"))
            }(ec)
          }
        }
      }
  }
}
