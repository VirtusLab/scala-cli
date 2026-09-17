package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.jdk.CollectionConverters.*

class ScalaOrganizationTests extends ScalaCliSuite {
  override def group: ScalaCliSuite.TestGroup = ScalaCliSuite.TestGroup.First

  private val forkOrganization = "ch.epfl.lara"
  private val forkVersion      = "3.10.1-RC1-bin-20260903-e1f9361-NIGHTLY"

  test("run with a Scala organization passed from the command line") {
    TestInputs(
      os.rel / "Hello.scala" ->
        """object Hello extends App {
          |  println("Hello")
          |}
          |""".stripMargin
    ).fromRoot { root =>
      val res = os.proc(
        TestUtil.cli,
        "--power",
        "run",
        ".",
        "--scala-organization",
        forkOrganization,
        "-S",
        forkVersion
      ).call(cwd = root, stderr = os.Pipe)
      expect(res.out.trim() == "Hello")
    }
  }

  test("run with a Scala organization set by a using directive") {
    TestInputs(
      os.rel / "Hello.scala" ->
        s"""//> using scalaOrganization $forkOrganization
           |//> using scala $forkVersion
           |object Hello extends App {
           |  println("Hello")
           |}
           |""".stripMargin
    ).fromRoot { root =>
      val res = os.proc(TestUtil.cli, "--power", "run", ".").call(cwd = root, stderr = os.Pipe)
      expect(res.out.trim() == "Hello")
    }
  }

  test("transitive Scala dependencies are redirected to the Scala organization") {
    TestInputs(
      os.rel / "Hello.scala" ->
        s"""//> using scalaOrganization $forkOrganization
           |//> using scala $forkVersion
           |//> using dep com.lihaoyi::os-lib:0.11.5
           |object Hello extends App {
           |  println(os.pwd.last.nonEmpty)
           |}
           |""".stripMargin
    ).fromRoot { root =>
      val res = os.proc(TestUtil.cli, "--power", "compile", ".", "--print-class-path")
        .call(cwd = root, stderr = os.Pipe)
      val classPathEntries                         = classPathEntriesOf(res)
      def scalaLibraries(organizationPath: String) = classPathEntries
        .filter(_.contains(s"/$organizationPath/scala3-library_3/"))
      expect(scalaLibraries(forkOrganization.replace('.', '/')).nonEmpty)
      expect(scalaLibraries("org/scala-lang").isEmpty)
    }
  }

  test("a required toolchain dependency is redirected to the Scala organization") {
    TestInputs(
      os.rel / "Hello.scala" ->
        s"""//> using scalaOrganization $forkOrganization
           |//> using scala $forkVersion
           |//> using dep org.scala-lang::scala3-staging:3.3.8
           |object Hello extends App {
           |  println("Hello")
           |}
           |""".stripMargin
    ).fromRoot { root =>
      val res = os.proc(TestUtil.cli, "--power", "compile", ".", "--print-class-path")
        .call(cwd = root, stderr = os.Pipe)
      val classPathEntries = classPathEntriesOf(res)
      expect(classPathEntries.exists(_.contains("/ch/epfl/lara/scala3-compiler_3/")))
      expect(!classPathEntries.exists(_.contains("/org/scala-lang/scala3-compiler_3/")))
    }
  }

  test("a Scala 2 dependency does not drag in a second standard library") {
    TestInputs(
      os.rel / "Hello.scala" ->
        s"""//> using scalaOrganization $forkOrganization
           |//> using scala $forkVersion
           |//> using dep org.scala-lang.modules:scala-java8-compat_2.13:1.0.2
           |object Hello extends App {
           |  println("Hello")
           |}
           |""".stripMargin
    ).fromRoot { root =>
      val res = os.proc(TestUtil.cli, "--power", "compile", ".", "--print-class-path")
        .call(cwd = root, stderr = os.Pipe)
      val classPathEntries = classPathEntriesOf(res)
      expect(!classPathEntries.exists(_.contains("/org/scala-lang/scala-library/")))
    }
  }

  test("an explicit upstream toolchain dependency is redirected as well") {
    TestInputs(
      os.rel / "Hello.scala" ->
        s"""//> using scalaOrganization $forkOrganization
           |//> using scala $forkVersion
           |//> using dep org.scala-lang:scala-library:2.13.16
           |object Hello extends App {
           |  println("Hello")
           |}
           |""".stripMargin
    ).fromRoot { root =>
      val res = os.proc(TestUtil.cli, "--power", "compile", ".", "--print-class-path")
        .call(cwd = root, stderr = os.Pipe)
      val classPathEntries = classPathEntriesOf(res)
      expect(!classPathEntries.exists(_.contains("/org/scala-lang/scala-library/")))
    }
  }

  test("an explicit upstream auxiliary compiler module is redirected as well") {
    TestInputs(
      os.rel / "Hello.scala" ->
        s"""//> using scalaOrganization $forkOrganization
           |//> using scala $forkVersion
           |//> using dep org.scala-lang::tasty-core:3.3.8
           |object Hello extends App {
           |  println("Hello")
           |}
           |""".stripMargin
    ).fromRoot { root =>
      val res = os.proc(TestUtil.cli, "--power", "compile", ".", "--print-class-path")
        .call(cwd = root, stderr = os.Pipe)
      val classPathEntries = classPathEntriesOf(res)
      expect(classPathEntries.exists(_.contains("/ch/epfl/lara/tasty-core_3/")))
      expect(!classPathEntries.exists(_.contains("/org/scala-lang/tasty-core_3/")))
    }
  }

  test("published metadata carries the Scala organization") {
    TestInputs(
      os.rel / "Hello.scala" ->
        s"""//> using scalaOrganization $forkOrganization
           |//> using scala $forkVersion
           |//> using dep com.lihaoyi::os-lib:0.11.5
           |//> using dep org.scala-lang:scala-library:2.13.16
           |//> using dep org.scala-lang::scala3-staging:3.3.8
           |//> using publish.organization test.org
           |//> using publish.name hello
           |//> using publish.version 0.1.0
           |object Hello extends App {
           |  println(os.pwd.last)
           |}
           |""".stripMargin
    ).fromRoot { root =>
      os.proc(TestUtil.cli, "--power", "publish", ".", "-R", "test-repo")
        .call(cwd = root, stdin = os.Inherit, stderr = os.Pipe)
      val pomFile = os.walk(root / "test-repo").filter(_.last.endsWith(".pom")).head
      val pom     = os.read(pomFile).replaceAll("\\s+", "")
      expect(!pom.contains(
        "<groupId>org.scala-lang</groupId><artifactId>scala-library</artifactId><version>"
      ))
      expect(pom.contains(
        s"<groupId>$forkOrganization</groupId><artifactId>scala-library</artifactId>"
      ))
      expect(pom.contains(
        "<exclusion><groupId>org.scala-lang</groupId><artifactId>scala3-library_3</artifactId></exclusion>"
      ))
      expect(pom.contains(
        s"<groupId>$forkOrganization</groupId><artifactId>scala3-compiler_3</artifactId>"
      ))
    }
  }

  test("a test-scope toolchain dependency is resolved against its own scope") {
    TestInputs(
      os.rel / "Hello.scala" ->
        s"""//> using scalaOrganization $forkOrganization
           |//> using scala $forkVersion
           |//> using publish.organization test.org
           |//> using publish.name hello
           |//> using publish.version 0.1.0
           |object Hello extends App {
           |  println("Hello")
           |}
           |""".stripMargin,
      os.rel / "HelloTest.test.scala" ->
        """//> using dep org.scala-lang::scala3-staging:3.3.8
          |object HelloTest
          |""".stripMargin
    ).fromRoot { root =>
      os.proc(TestUtil.cli, "--power", "publish", ".", "--test", "-R", "test-repo")
        .call(cwd = root, stdin = os.Inherit, stderr = os.Pipe)
      val pomFile = os.walk(root / "test-repo").filter(_.last.endsWith(".pom")).head
      val pom     = os.read(pomFile).replaceAll("\\s+", "")
      expect(pom.contains(
        s"<groupId>$forkOrganization</groupId><artifactId>scala3-compiler_3</artifactId>"
      ))
    }
  }

  test("a compile-only dependency does not publish the toolchain it needed") {
    TestInputs(
      os.rel / "Hello.scala" ->
        s"""//> using scalaOrganization $forkOrganization
           |//> using scala $forkVersion
           |//> using compileOnly.dep org.scala-lang::scala3-staging:3.3.8
           |//> using publish.organization test.org
           |//> using publish.name hello
           |//> using publish.version 0.1.0
           |object Hello extends App {
           |  println("Hello")
           |}
           |""".stripMargin
    ).fromRoot { root =>
      os.proc(TestUtil.cli, "--power", "publish", ".", "-R", "test-repo")
        .call(cwd = root, stdin = os.Inherit, stderr = os.Pipe)
      val pomFile = os.walk(root / "test-repo").filter(_.last.endsWith(".pom")).head
      val pom     = os.read(pomFile).replaceAll("\\s+", "")
      expect(!pom.contains(
        s"<groupId>$forkOrganization</groupId><artifactId>scala3-compiler_3</artifactId>"
      ))
    }
  }

  test("a module the fork does not publish keeps its own version") {
    TestInputs(
      os.rel / "Hello.scala" ->
        s"""//> using scalaOrganization $forkOrganization
           |//> using scala $forkVersion
           |//> using dep org.scala-lang::scala3-tasty-inspector:3.3.8
           |object Hello extends App {
           |  println("Hello")
           |}
           |""".stripMargin
    ).fromRoot { root =>
      val res = os.proc(TestUtil.cli, "--power", "compile", ".", "--print-class-path")
        .call(cwd = root, stderr = os.Pipe)
      val classPathEntries = classPathEntriesOf(res)
      expect(classPathEntries.exists(_.contains("/org/scala-lang/scala3-tasty-inspector_3/3.3.8/")))
    }
  }

  test("a toolchain module provided by both organizations is reported") {
    TestInputs(
      os.rel / "Hello.scala" ->
        s"""//> using scalaOrganization $forkOrganization
           |//> using scala $forkVersion
           |//> using dep org.scala-lang::scala3-tasty-inspector:3.3.8
           |object Hello extends App {
           |  println("Hello")
           |}
           |""".stripMargin
    ).fromRoot { root =>
      val res = os.proc(TestUtil.cli, "--power", "compile", ".", "--with-compiler")
        .call(cwd = root, stderr = os.Pipe, mergeErrIntoOut = true)
      expect(res.out.text().contains(
        s"Both $forkOrganization and org.scala-lang provide scala3-tasty-inspector_3"
      ))
    }
  }

  test("a provided module is matched against the redirected coordinates") {
    forkInputs.fromRoot { root =>
      val assembly = root / "app.jar"
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        ".",
        "--assembly",
        "--provided",
        "org.scala-lang:scala3-library_3",
        "-o",
        assembly.toString
      ).call(cwd = root, stderr = os.Pipe)
      val zipFile = new java.util.zip.ZipFile(assembly.toIO)
      val entries =
        try zipFile.entries().asScala.map(_.getName).toVector
        finally zipFile.close()
      expect(!entries.exists(_.startsWith("scala/collection/")))
    }
  }

  test("the REPL resolves dependencies within the Scala organization") {
    TestInputs(
      os.rel / "Hello.scala" ->
        s"""//> using scalaOrganization $forkOrganization
           |//> using scala $forkVersion
           |//> using dep com.lihaoyi::os-lib:0.11.5
           |object Hello
           |""".stripMargin
    ).fromRoot { root =>
      os.proc(TestUtil.cli, "--power", "repl", ".", "--repl-dry-run")
        .call(cwd = root, stderr = os.Pipe)
    }
  }

  /** Class path entries with separators normalised, so the checks below hold on Windows too. */
  private def classPathEntriesOf(res: os.CommandResult) =
    res.out.trim().split(java.io.File.pathSeparator).toSeq.map(_.replace('\\', '/'))

  private val forkInputs = TestInputs(
    os.rel / "Hello.scala" ->
      s"""//> using scalaOrganization $forkOrganization
         |//> using scala $forkVersion
         |object Hello extends App {
         |  println("Hello")
         |}
         |""".stripMargin
  )

  for (
    (buildTool, exportFlag, buildFileNames) <- Seq(
      ("sbt", "--sbt", Set("build.sbt")),
      ("Maven", "--mvn", Set("pom.xml"))
    )
  )
    test(s"the Scala organization is carried over to a $buildTool export") {
      forkInputs.fromRoot { root =>
        val outputDir = root / "exported"
        os.proc(TestUtil.cli, "--power", "export", ".", exportFlag, "-o", outputDir.toString)
          .call(cwd = root, stderr = os.Pipe)
        val buildFiles = os.walk(outputDir).filter(os.isFile).filter(p => buildFileNames(p.last))
        expect(buildFiles.nonEmpty)
        expect(buildFiles.exists(os.read(_).contains(forkOrganization)))
        if buildTool == "Maven" then
          expect(buildFiles.exists(
            os.read(_).contains(s"<scalaOrganization>$forkOrganization</scalaOrganization>")
          ))
      }
    }

  test("a Mill export keeps the Scala organization for a Mill version supporting it") {
    forkInputs.fromRoot { root =>
      val outputDir = root / "exported"
      os.proc(
        TestUtil.cli,
        "--power",
        "export",
        ".",
        "--mill",
        "--mill-version",
        "0.12.17",
        "-o",
        outputDir.toString
      ).call(cwd = root, stderr = os.Pipe)
      val buildFiles = os.walk(outputDir).filter(os.isFile)
        .filter(p => p.last == "build.mill" || p.last == "build.sc")
      expect(buildFiles.nonEmpty)
      expect(
        buildFiles.exists(os.read(_).contains(s"""def scalaOrganization = "$forkOrganization""""))
      )
    }
  }

  for (
    (description, mainDirective, expectedMessage) <- Seq(
      (
        "differing between scopes",
        s"//> using scalaOrganization org.other\n",
        "different Scala organizations"
      ),
      ("set in the test scope only", "", "only set in the test scope")
    )
  )
    test(s"an export rejects a Scala organization $description") {
      TestInputs(
        os.rel / "Hello.scala" ->
          s"""$mainDirective//> using scala $forkVersion
             |object Hello
             |""".stripMargin,
        os.rel / "HelloTest.test.scala" ->
          s"""//> using scalaOrganization $forkOrganization
             |object HelloTest
             |""".stripMargin
      ).fromRoot { root =>
        val res = os.proc(
          TestUtil.cli,
          "--power",
          "export",
          ".",
          "--sbt",
          "-o",
          (root / "exported").toString
        ).call(cwd = root, check = false, mergeErrIntoOut = true)
        expect(res.exitCode != 0)
        expect(res.out.text().contains(expectedMessage))
      }
    }

  test("a Mill export rejects a custom Scala organization") {
    forkInputs.fromRoot { root =>
      val res = os.proc(
        TestUtil.cli,
        "--power",
        "export",
        ".",
        "--mill",
        "-o",
        (root / "exported").toString
      ).call(cwd = root, stderr = os.Pipe, check = false, mergeErrIntoOut = true)
      expect(res.exitCode != 0)
      expect(res.out.text().contains("scalaOrganization"))
    }
  }

  test("a Maven export excludes the upstream toolchain from project dependencies") {
    TestInputs(
      os.rel / "Hello.scala" ->
        s"""//> using scalaOrganization $forkOrganization
           |//> using scala $forkVersion
           |//> using dep com.lihaoyi::os-lib:0.11.5
           |object Hello extends App {
           |  println(os.pwd.last.nonEmpty)
           |}
           |""".stripMargin
    ).fromRoot { root =>
      val outputDir = root / "exported"
      os.proc(TestUtil.cli, "--power", "export", ".", "--mvn", "-o", outputDir.toString)
        .call(cwd = root, stderr = os.Pipe)
      val pom = os.read(outputDir / "pom.xml").replaceAll("\\s+", "")
      expect(pom.contains(
        "<exclusion><groupId>org.scala-lang</groupId><artifactId>scala3-library_3</artifactId></exclusion>"
      ))
      expect(pom.contains(
        s"<groupId>$forkOrganization</groupId><artifactId>scala3-library_3</artifactId>"
      ))
    }
  }

  test("a Maven export avoids the compiler bridge hardcoded to the official organization") {
    forkInputs.fromRoot { root =>
      val outputDir = root / "exported"
      os.proc(TestUtil.cli, "--power", "export", ".", "--mvn", "-o", outputDir.toString)
        .call(cwd = root, stderr = os.Pipe)
      val pom = os.read(outputDir / "pom.xml").replaceAll("\\s+", "")
      expect(pom.contains("<recompileMode>all</recompileMode>"))
    }
  }

  test("a Maven export rewrites explicit upstream toolchain dependencies") {
    TestInputs(
      os.rel / "Hello.scala" ->
        s"""//> using scalaOrganization $forkOrganization
           |//> using scala $forkVersion
           |//> using dep org.scala-lang:scala-library:2.13.16
           |object Hello extends App {
           |  println("Hello")
           |}
           |""".stripMargin
    ).fromRoot { root =>
      val outputDir = root / "exported"
      os.proc(TestUtil.cli, "--power", "export", ".", "--mvn", "-o", outputDir.toString)
        .call(cwd = root, stderr = os.Pipe)
      val pom = os.read(outputDir / "pom.xml").replaceAll("\\s+", "")
      expect(
        !pom.contains("<groupId>org.scala-lang</groupId><artifactId>scala-library</artifactId>")
      )
      expect(pom.contains(
        s"<groupId>$forkOrganization</groupId><artifactId>scala-library</artifactId><version>$forkVersion</version>"
      ))
    }
  }
}
