package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.{Constants, PackageTestDefinitions, TestScalaVersion, TestUtil}
import scala.util.Properties

trait PackageSlothTestDefinitions { this: PackageTestDefinitions & TestScalaVersion =>
  if actualScalaVersion.startsWith("3.") then {
    val latestJava             = Constants.allJavaVersions.max
    val assemblyScalaVersions  = Seq("3.0.2", Constants.scala3Lts)
    val ltsOnlyScalaVersion    = Constants.scala3Lts
    val expectedMessage        = "Hello"
    val slothNoOpWarnPrefix    = "Sloth patching is not applicable to"
    val slothAgentWarnFragment = "is not applicable to package"

    def lazyValApp(scalaVersion: String): String =
      s"""//> using scala $scalaVersion
         |object Main {
         |  lazy val greeting: String = "$expectedMessage"
         |  def main(args: Array[String]): Unit = println(greeting)
         |}
         |""".stripMargin

    def nativeApp(scalaVersion: String): String =
      s"""//> using scala $scalaVersion
         |//> using platform scala-native
         |object Main {
         |  def main(args: Array[String]): Unit = println("$expectedMessage")
         |}
         |""".stripMargin

    def lazyValJsApp(scalaVersion: String): String =
      s"""//> using scala $scalaVersion
         |//> using platform scala-js
         |import scala.scalajs.js
         |
         |object Main {
         |  lazy val greeting: String = "$expectedMessage"
         |  def main(args: Array[String]): Unit =
         |    js.Dynamic.global.console.log(greeting)
         |}
         |""".stripMargin

    def runAssemblyJar(root: os.Path, appJar: os.Path, mainClass: String): os.CommandResult =
      os.proc(
        TestUtil.cli,
        "run",
        extraOptions,
        appJar,
        "-M",
        mainClass,
        "--jvm",
        latestJava.toString
      ).call(cwd = root, stderr = os.Pipe)

    def runLibraryJar(root: os.Path, appJar: os.Path): os.CommandResult =
      os.proc(
        TestUtil.cli,
        "run",
        extraOptions,
        appJar,
        "--jvm",
        latestJava.toString
      ).call(cwd = root, stderr = os.Pipe)

    def javaHome(jvm: Int): os.Path =
      os.Path(
        os.proc(TestUtil.cs, "java-home", "--jvm", jvm.toString).call().out.trim(),
        os.pwd
      )

    def runBootstrapLauncher(root: os.Path, launcher: os.Path): os.CommandResult =
      val home = javaHome(latestJava)
      val env  = Map("JAVA_HOME" -> home.toString)
      val res  =
        os.proc(launcher.toString).call(cwd = root, stderr = os.Pipe, check = false, env = env)
      if Properties.isLinux && res.exitCode == 127 then
        os.proc("/bin/bash", launcher.toString).call(cwd = root, stderr = os.Pipe, env = env)
      else if res.exitCode != 0 then throw os.SubprocessException(res)
      else res

    def packageSlothTest(
      label: String,
      packageExtraArgs: Seq[String],
      runArtifact: (os.Path, os.Path) => os.CommandResult
    )(packageScalaVersion: String): Unit =
      test(s"package $label $packageScalaVersion --sloth patches lazy vals on JDK $latestJava") {
        TestInputs(
          os.rel / "Main.scala" -> lazyValApp(packageScalaVersion)
        ).fromRoot { root =>
          val appJar = root / "app.jar"
          os.proc(
            TestUtil.cli,
            "--power",
            "package",
            extraOptions,
            "--sloth",
            packageExtraArgs,
            ".",
            "-o",
            appJar
          ).call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)

          val r = runArtifact(root, appJar)
          expect(r.out.trim().contains(expectedMessage))
          expect(!r.err.trim().contains("sun.misc.Unsafe"))
        }
      }

    for ver <- assemblyScalaVersions do
      packageSlothTest(
        "assembly",
        Seq("--assembly", "--preamble=false"),
        runAssemblyJar(_, _, "Main")
      )(ver)

    packageSlothTest(
      "bootstrap standalone",
      Seq("--standalone"),
      runBootstrapLauncher
    )(ltsOnlyScalaVersion)

    test(s"package library $ltsOnlyScalaVersion --sloth patches lazy vals on JDK $latestJava") {
      TestInputs(
        os.rel / "Main.scala" -> lazyValApp(ltsOnlyScalaVersion)
      ).fromRoot { root =>
        val appJar = root / "app.jar"
        os.proc(
          TestUtil.cli,
          "--power",
          "package",
          extraOptions,
          "--sloth",
          "--library",
          ".",
          "-o",
          appJar
        ).call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)

        val r = runLibraryJar(root, appJar)
        expect(r.out.trim().contains(expectedMessage))
        expect(!r.err.trim().contains("sun.misc.Unsafe"))
      }
    }

    test(s"package native-image $ltsOnlyScalaVersion --sloth patches classpath on JDK $latestJava") {
      TestUtil.retryOnCi() {
        val dest       = "hello"
        val actualDest = if Properties.isWin then "hello.exe" else "hello"
        TestInputs(
          os.rel / "Main.scala" -> lazyValApp(ltsOnlyScalaVersion)
        ).fromRoot { root =>
          os.proc(
            TestUtil.cli,
            "--power",
            "package",
            extraOptions,
            "--sloth",
            ".",
            "--native-image",
            "-o",
            dest,
            "--",
            "--no-fallback"
          ).call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)

          expect(os.isFile(root / actualDest))
          val res = os.proc(root / actualDest).call(cwd = root)
          expect(res.out.trim() == expectedMessage)
        }
      }
    }

    test("package native --sloth warns that sloth is not applicable") {
      TestUtil.retryOnCi() {
        TestInputs(
          os.rel / "Main.scala" -> nativeApp(ltsOnlyScalaVersion)
        ).fromRoot { root =>
          val dest = if Properties.isWin then root / "app.exe" else root / "app"
          val r    = os.proc(
            TestUtil.cli,
            "--power",
            "package",
            extraOptions,
            "--sloth",
            "--native",
            ".",
            "-o",
            dest
          ).call(cwd = root, mergeErrIntoOut = true)
          expect(r.out.trim().contains(slothNoOpWarnPrefix))
          expect(r.out.trim().contains("Scala Native"))
        }
      }
    }

    test("package js --sloth warns that sloth is not applicable") {
      TestInputs(
        os.rel / "Main.scala" -> lazyValJsApp(ltsOnlyScalaVersion)
      ).fromRoot { root =>
        val dest = root / "app.js"
        val r    = os.proc(
          TestUtil.cli,
          "--power",
          "package",
          extraOptions,
          "--sloth",
          "--js",
          ".",
          "-o",
          dest
        ).call(cwd = root, mergeErrIntoOut = true)
        expect(r.out.trim().contains(slothNoOpWarnPrefix))
        expect(r.out.trim().contains("Scala.js"))
      }
    }

    test("package source jar --sloth warns that sloth is not applicable") {
      TestInputs(
        os.rel / "Main.scala" -> lazyValApp(ltsOnlyScalaVersion)
      ).fromRoot { root =>
        val dest = root / "sources.jar"
        val r    = os.proc(
          TestUtil.cli,
          "--power",
          "package",
          extraOptions,
          "--sloth",
          ".",
          "-o",
          dest,
          "--with-sources"
        ).call(cwd = root, mergeErrIntoOut = true)
        expect(r.out.trim().contains(slothNoOpWarnPrefix))
        expect(r.out.trim().contains("source jars"))
      }
    }

    test("package doc jar --sloth warns that sloth is not applicable") {
      TestInputs(
        os.rel / "Main.scala" -> lazyValApp(ltsOnlyScalaVersion)
      ).fromRoot { root =>
        val dest = root / "doc.jar"
        val r    = os.proc(
          TestUtil.cli,
          "--power",
          "package",
          extraOptions,
          "--sloth",
          ".",
          "-o",
          dest,
          "--doc"
        ).call(cwd = root, mergeErrIntoOut = true)
        expect(r.out.trim().contains(slothNoOpWarnPrefix))
        expect(r.out.trim().contains("doc jars"))
      }
    }

    test("package --sloth-agent is rejected with a warning") {
      TestInputs(
        os.rel / "Main.scala" -> lazyValApp(ltsOnlyScalaVersion)
      ).fromRoot { root =>
        val appJar = root / "app.jar"
        val r      = os.proc(
          TestUtil.cli,
          "--power",
          "package",
          extraOptions,
          "--sloth-agent",
          "--assembly",
          ".",
          "-o",
          appJar
        ).call(cwd = root, mergeErrIntoOut = true)
        expect(os.isFile(appJar))
        expect(r.out.trim().contains(slothAgentWarnFragment))
      }
    }
  }
}
