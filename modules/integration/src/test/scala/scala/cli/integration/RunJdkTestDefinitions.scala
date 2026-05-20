package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.ProcOps
import scala.util.{Properties, Try}

trait RunJdkTestDefinitions { this: RunTestDefinitions =>
  def canUseScalaInstallationWrapper: Boolean =
    actualScalaVersion.startsWith("3") && actualScalaVersion.split('.').drop(1).head.toInt >= 5

  for {
    javaVersion <- isScala38OrNewer -> TestUtil.isJvmCli match {
      case (false, true) =>
        Constants.allJavaVersions.filter(_ >= Constants.minimumLauncherJavaVersion)
      case (true, _) => Constants.allJavaVersions.filter(_ >= Constants.defaultJvmVersion)
      case _         => Constants.allJavaVersions
    }
    index = javaVersion
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
            val res = os.proc(launcher, "run", ".", extraOptions, "--jvm", index)
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

  if (isScala38OrNewer)
    for (oldJvm <- Seq(11, 8).filter(Constants.allJavaVersions.contains)) {
      test(
        s"auto-falls back from JAVA_HOME $oldJvm when Scala $actualScalaVersion requires a newer JDK"
      ) {
        TestUtil.retryOnCi() {
          TestInputs(
            os.rel / "check_java_version.sc" ->
              """println(System.getProperty("java.version"))""".stripMargin
          ).fromRoot { root =>
            val javaHome =
              os.Path(
                os.proc(TestUtil.cs, "java-home", "--jvm", oldJvm).call().out.trim(),
                os.pwd
              )
            val res = os
              .proc(TestUtil.cli, "run", ".", extraOptions)
              .call(cwd = root, env = Map("JAVA_HOME" -> javaHome.toString), stderr = os.Pipe)
            val reportedVersion = res.out.trim()
            expect(
              reportedVersion.startsWith("17") ||
              reportedVersion.startsWith("21") ||
              reportedVersion.startsWith("23") ||
              reportedVersion.startsWith("24") ||
              reportedVersion.startsWith("25") ||
              reportedVersion.startsWith("26")
            )
            expect(
              res.err.text().contains(s"requires at least Java ${Constants.scala38MinJavaVersion}")
            )
          }
        }
      }

      test(s"errors on explicit --jvm $oldJvm when Scala $actualScalaVersion requires a newer JDK") {
        TestUtil.retryOnCi() {
          TestInputs(
            os.rel / "hello.sc" -> """println("ok")"""
          ).fromRoot { root =>
            val res = os
              .proc(TestUtil.cli, "run", "hello.sc", extraOptions, "--jvm", oldJvm)
              .call(cwd = root, check = false, stderr = os.Pipe)
            expect(res.exitCode != 0)
            expect(
              res.err.text().contains(s"requires at least Java ${Constants.scala38MinJavaVersion}")
            )
          }
        }
      }
    }

  {
    val newJavaVersion = Constants.allJavaVersions.max
    if newJavaVersion > 17 && actualScalaVersion == Constants.defaultScala then
      test(s"warns when JVM $newJavaVersion is newer than Scala 3.0.2 supports") {
        TestUtil.retryOnCi() {
          TestInputs(
            os.rel / "hello.sc" -> """println("ok")"""
          ).fromRoot { root =>
            val res = os
              .proc(
                TestUtil.cli,
                "run",
                "hello.sc",
                TestUtil.extraOptions,
                "--jvm",
                newJavaVersion,
                "-S",
                "3.0.2"
              )
              .call(cwd = root, check = false, stderr = os.Pipe)
            expect(res.err.text().contains("only tested up to JDK 17"))
          }
        }
      }
  }
}
