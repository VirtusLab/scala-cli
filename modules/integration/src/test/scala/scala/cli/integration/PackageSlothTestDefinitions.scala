package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.nio.charset.StandardCharsets
import java.util

import scala.util.Properties

trait PackageSlothTestDefinitions extends LazyValTests:
  this: PackageTestDefinitions & TestScalaVersion =>

  private val latestJava             = Constants.allJavaVersions.max
  private val assemblyScalaVersions  = Seq("3.0.2", Constants.scala3Lts)
  private val ltsOnlyScalaVersion    = Constants.scala3Lts
  private val expectedMessage        = "Hello"
  private val slothAgentWarnFragment = "is not applicable to package"

  private def lazyValApp(scalaVersion: String): String =
    s"""//> using scala $scalaVersion
       |object Main {
       |  lazy val greeting: String = "$expectedMessage"
       |  def main(args: Array[String]): Unit = println(greeting)
       |}
       |""".stripMargin

  private def nativeApp(scalaVersion: String): String =
    s"""//> using scala $scalaVersion
       |//> using platform scala-native
       |object Main {
       |  def main(args: Array[String]): Unit = println("$expectedMessage")
       |}
       |""".stripMargin

  private def lazyValJsApp(scalaVersion: String): String =
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

  /** Assembly preambles are OS-specific: a `.bat` script on Windows, a shell script elsewhere. */
  private val assemblyPreambleStart: Array[Byte] =
    val marker = if Properties.isWin then "@echo off" else "#!"
    marker.getBytes(StandardCharsets.UTF_8)

  private def runAssemblyJar(
    root: os.Path,
    appJar: os.Path,
    mainClass: String,
    withPreamble: Boolean = false
  ): os.CommandResult =
    if withPreamble then
      val contentStart = os.read.bytes(appJar).take(assemblyPreambleStart.length)
      expect(util.Arrays.equals(contentStart, assemblyPreambleStart))
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

  private def runLibraryJar(root: os.Path, appJar: os.Path): os.CommandResult =
    os.proc(
      TestUtil.cli,
      "run",
      extraOptions,
      appJar,
      "--jvm",
      latestJava.toString
    ).call(cwd = root, stderr = os.Pipe)

  private def runBootstrapLauncher(root: os.Path, launcher: os.Path): os.CommandResult =
    val home = javaHome(latestJava)
    val env  = Map("JAVA_HOME" -> home.toString)
    val res  =
      os.proc(launcher.toString).call(cwd = root, stderr = os.Pipe, check = false, env = env)
    if Properties.isLinux && res.exitCode == 127 then
      os.proc("/bin/bash", launcher.toString).call(cwd = root, stderr = os.Pipe, env = env)
    else if res.exitCode != 0 then throw os.SubprocessException(res)
    else res

  private def packageSlothTest(
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

  for ver <- assemblyScalaVersions do
    packageSlothTest(
      "sloth-assembly-default-preamble",
      Seq("--assembly"),
      runAssemblyJar(_, _, "Main", withPreamble = true)
    )(ver)

  test(
    s"package bootstrap standalone $ltsOnlyScalaVersion --sloth patches lazy vals on JDK $latestJava"
  ) {
    TestInputs(
      os.rel / "Main.scala" -> lazyValApp(ltsOnlyScalaVersion)
    ).fromRoot { root =>
      val launcher = root / (if Properties.isWin then "app.bat" else "app")
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        extraOptions,
        "--sloth",
        "--standalone",
        ".",
        "-o",
        launcher
      ).call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)

      val r = runBootstrapLauncher(root, launcher)
      expect(r.out.trim().contains(expectedMessage))
      expect(!r.err.trim().contains("sun.misc.Unsafe"))
    }
  }

  test(
    s"package bootstrap --standalone --sloth patches external -cp class directory on JDK $latestJava"
  ) {
    TestInputs(
      externalLazyValsInput(),
      os.rel / "project" / "Main.scala" ->
        """object Main {
          |  def main(args: Array[String]): Unit = println(slothful)
          |}
          |""".stripMargin
    ).fromRoot { root =>
      val (classDir, expectedMsg) = compileExternalLazyValClassDir(root)
      val launcher                = root / (if Properties.isWin then "app.bat" else "app")
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        "--server=false",
        "--sloth",
        "--suppress-experimental-feature-warning",
        "--standalone",
        "-cp",
        classDir.toString,
        os.rel / "project" / "Main.scala",
        "-o",
        launcher,
        extraOptions
      ).call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)

      val r = runBootstrapLauncher(root, launcher)
      expect(r.out.trim().contains(expectedMsg))
      expect(!r.err.trim().contains("sun.misc.Unsafe"))
    }
  }

  test(
    s"package bootstrap --standalone jars external -cp class directory without --sloth on JDK $latestJava"
  ) {
    TestInputs(
      externalLazyValsInput(),
      os.rel / "project" / "Main.scala" ->
        """object Main {
          |  def main(args: Array[String]): Unit = println(slothful)
          |}
          |""".stripMargin
    ).fromRoot { root =>
      val (classDir, expectedMsg) = compileExternalLazyValClassDir(root)
      val launcher                = root / (if Properties.isWin then "app.bat" else "app")
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        "--server=false",
        "--standalone",
        "-cp",
        classDir.toString,
        os.rel / "project" / "Main.scala",
        "-o",
        launcher,
        extraOptions
      ).call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)

      // Without --sloth the bootstrap still succeeds (directory is jarred); Unsafe warning is OK.
      val r = runBootstrapLauncher(root, launcher)
      expect(r.out.trim().contains(expectedMsg))
    }
  }

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

  for {
    (label, packageExtraArgs) <- Seq(
      "library"  -> Seq("--library"),
      "assembly" -> Seq("--assembly", "--preamble=false")
    )
  }
    test(
      s"package $label reflects --sloth toggle for $ltsOnlyScalaVersion lazy vals on JDK $latestJava"
    ) {
      TestInputs(
        os.rel / "Main.scala" -> lazyValApp(ltsOnlyScalaVersion)
      ).fromRoot { root =>
        val appJar = root / "app.jar"

        os.proc(
          TestUtil.cli,
          "--power",
          "package",
          extraOptions,
          slothOptions,
          packageExtraArgs,
          ".",
          "-o",
          appJar
        ).call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)

        val withSloth =
          if label == "library" then runLibraryJar(root, appJar)
          else runAssemblyJar(root, appJar, "Main")
        expect(withSloth.out.trim().contains(expectedMessage))
        expect(!withSloth.err.trim().contains("sun.misc.Unsafe"))

        os.proc(
          TestUtil.cli,
          "--power",
          "package",
          extraOptions,
          "--force",
          packageExtraArgs,
          ".",
          "-o",
          appJar
        ).call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)

        val withoutSloth =
          if label == "library" then runLibraryJar(root, appJar)
          else runAssemblyJar(root, appJar, "Main")
        expect(withoutSloth.out.trim().contains(expectedMessage))
        expect(withoutSloth.err.trim().contains("sun.misc.Unsafe"))
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

  test(
    s"package native-image rebuilds when --sloth is toggled on ($ltsOnlyScalaVersion)"
  ) {
    TestUtil.retryOnCi() {
      val dest       = "hello"
      val actualDest = if Properties.isWin then "hello.exe" else "hello"
      val cachedMsg  = "Found cached native image binary."
      val sharedOpts = extraOptions ++ Seq("--suppress-experimental-feature-warning")
      TestInputs(
        os.rel / "Main.scala" -> lazyValApp(ltsOnlyScalaVersion)
      ).fromRoot { root =>
        os.proc(
          TestUtil.cli,
          "--power",
          "package",
          sharedOpts,
          ".",
          "--native-image",
          "-o",
          dest,
          "--",
          "--no-fallback"
        ).call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)

        expect(os.isFile(root / actualDest))
        val withoutSlothBytes = os.read.bytes(root / actualDest)

        val withSloth = os.proc(
          TestUtil.cli,
          "--power",
          "package",
          sharedOpts,
          "--sloth",
          "--force",
          ".",
          "--native-image",
          "-o",
          dest,
          "--",
          "--no-fallback"
        ).call(cwd = root, mergeErrIntoOut = true)

        expect(!withSloth.out.trim().contains(cachedMsg))
        expect(os.isFile(root / actualDest))
        val withSlothBytes = os.read.bytes(root / actualDest)
        expect(!util.Arrays.equals(withoutSlothBytes, withSlothBytes))
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

  test("package --doc --sloth patches the scaladoc classpath") {
    TestInputs(
      os.rel / "Main.scala" -> lazyValApp(ltsOnlyScalaVersion)
    ).fromRoot { root =>
      val dest = root / "doc.jar"
      val r    = os.proc(
        TestUtil.cli,
        "--power",
        "package",
        extraOptions,
        slothOptions,
        ".",
        "-o",
        dest,
        "--doc",
        "-v"
      ).call(cwd = root, mergeErrIntoOut = true)
      expect(r.exitCode == 0)
      expect(os.isFile(dest))
      expectScaladocClasspathContains(r.out.text(), slothCacheSegment)
      expect(!r.out.text().contains(slothNoOpWarnPrefix))
    }
  }

  test("package --doc --sloth-agent attaches the sloth java agent to scaladoc") {
    TestInputs(
      os.rel / "Main.scala" -> lazyValApp(ltsOnlyScalaVersion)
    ).fromRoot { root =>
      val dest = root / "doc.jar"
      val r    = os.proc(
        TestUtil.cli,
        "--power",
        "package",
        extraOptions,
        slothAgentOptions,
        ".",
        "-o",
        dest,
        "--doc",
        "-v"
      ).call(cwd = root, mergeErrIntoOut = true)
      expect(r.exitCode == 0)
      expect(os.isFile(dest))
      expect(r.out.text().contains("-javaagent"))
      expect(!r.out.text().contains(slothAgentWarnFragment))
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

  for {
    preambleOpts <- Seq(Seq("--preamble=false"), Nil)
    preambleString = preambleOpts.headOption.getOrElse("assembly with preamble")
    if actualScalaVersion.startsWith("3")
    if !Properties.isWin || preambleOpts.isEmpty
  }
    test(
      s"package assembly --sloth with signed dependency strips signatures (JDK $latestJava, $preambleString)"
    ) {
      TestInputs(
        os.rel / "Main.scala" ->
          """import signedlib.SignedLib
            |object Main {
            |  def main(args: Array[String]): Unit = println(SignedLib.greeting)
            |}
            |""".stripMargin
      ).fromRoot { root =>
        // Signed dep built at 3.3 LTS
        val signedJar = publishSignedLazyValsJar(Constants.scala3Lts, root, latestJava)
        val appJar    = root / "app.jar"

        os.proc(
          TestUtil.cli,
          "--power",
          "package",
          extraOptions,
          slothOptions,
          "--assembly",
          preambleOpts,
          "--classpath",
          signedJar,
          ".",
          "-o",
          appJar
        ).call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)

        // The assembly should not contain signature files
        expect(signatureEntriesIn(appJar).isEmpty)

        val r = os.proc(
          TestUtil.cli,
          "run",
          extraOptions,
          appJar,
          "-M",
          "Main",
          "--jvm",
          latestJava.toString
        ).call(cwd = root, stderr = os.Pipe)

        expect(r.out.trim().contains(signedLibMessage))
        expect(!r.err.trim().contains("sun.misc.Unsafe"))
      }
    }

  test(s"package --library --sloth preserves user META-INF/MANIFEST.MF ($ltsOnlyScalaVersion)") {
    TestInputs(
      os.rel / "Main.scala" ->
        s"""//> using scala $ltsOnlyScalaVersion
           |//> using resourceDir resources
           |object Main {
           |  lazy val greeting: String = "$expectedMessage"
           |  def main(args: Array[String]): Unit = println(greeting)
           |}
           |""".stripMargin,
      os.rel / "resources" / "META-INF" / "MANIFEST.MF" -> userManifestResourceContent
    ).fromRoot { root =>
      val appJar = root / "app.jar"
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        extraOptions,
        slothOptions,
        "--library",
        ".",
        "-o",
        appJar
      ).call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)

      val attrs = jarManifestMainAttributes(appJar)
      expect(attrs.get("X-Custom").contains("yes"))
      expect(attrs.get("Main-Class").contains("Main"))
      val r = runLibraryJar(root, appJar)
      expect(r.out.trim().contains(expectedMessage))
    }
  }

  test(s"package --assembly --sloth preserves user META-INF/MANIFEST.MF ($ltsOnlyScalaVersion)") {
    TestInputs(
      os.rel / "Main.scala" ->
        s"""//> using scala $ltsOnlyScalaVersion
           |//> using resourceDir resources
           |object Main {
           |  lazy val greeting: String = "$expectedMessage"
           |  def main(args: Array[String]): Unit = println(greeting)
           |}
           |""".stripMargin,
      os.rel / "resources" / "META-INF" / "MANIFEST.MF" -> userManifestResourceContent
    ).fromRoot { root =>
      val appJar = root / "app.jar"
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        extraOptions,
        slothOptions,
        "--assembly",
        "--preamble=false",
        ".",
        "-o",
        appJar
      ).call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)

      val attrs = jarManifestMainAttributes(appJar)
      expect(attrs.get("X-Custom").contains("yes"))
      expect(attrs.get("Main-Class").contains("Main"))
      val r = runAssemblyJar(root, appJar, "Main")
      expect(r.out.trim().contains(expectedMessage))
    }
  }

  for {
    preambleOpts <- Seq(Seq("--preamble=false"), Nil)
    preambleString = preambleOpts.headOption.getOrElse("assembly with preamble")
    if !Properties.isWin || preambleOpts.nonEmpty
  }
    test(
      s"package assembly --sloth extension-less output patches lazy vals ($ltsOnlyScalaVersion, $preambleString)"
    ) {
      TestInputs(
        os.rel / "Main.scala" -> lazyValApp(ltsOnlyScalaVersion)
      ).fromRoot { root =>
        val app = root / "app_no_jar_ext"
        os.proc(
          TestUtil.cli,
          "--power",
          "package",
          extraOptions,
          slothOptions,
          "--assembly",
          preambleOpts,
          ".",
          "-o",
          app
        ).call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)

        val r = os.proc(
          jdkTool(latestJava, "java").toString,
          "-jar",
          app.toString
        ).call(cwd = root, stderr = os.Pipe)
        expect(r.out.trim().contains(expectedMessage))
        expect(!r.err.trim().contains("sun.misc.Unsafe"))
      }
    }
