package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

abstract class FixTestDefinitions
    extends ScalaCliSuite
    with TestScalaVersionArgs
    with ScalafixTestDefinitions { _: TestScalaVersion =>
  val projectFileName = "project.scala"
  val extraOptions: Seq[String] =
    scalaVersionArgs ++ TestUtil.extraOptions ++ Seq("--suppress-experimental-feature-warning")
  def enableRulesOptions(
    enableScalafix: Boolean = true,
    enableBuiltIn: Boolean = true
  ): Seq[String] =
    Seq(
      s"--enable-scalafix=${enableScalafix.toString}",
      s"--enable-built-in-rules=${enableBuiltIn.toString}"
    )

  test("basic built-in rules") {
    val mainFileName = "Main.scala"
    val inputs = TestInputs(
      os.rel / mainFileName ->
        s"""//> using objectWrapper
           |//> using dep com.lihaoyi::os-lib:0.9.1 com.lihaoyi::upickle:3.1.2
           |
           |package com.foo.main
           |
           |object Main extends App {
           |  println(os.pwd)
           |}
           |""".stripMargin,
      os.rel / projectFileName ->
        s"""//> using deps "com.lihaoyi::pprint:0.6.6"
           |""".stripMargin
    )

    inputs.fromRoot { root =>

      val fixOutput = os.proc(
        TestUtil.cli,
        "--power",
        "fix",
        ".",
        "-v",
        "-v",
        extraOptions,
        enableRulesOptions(enableScalafix = false)
      )
        .call(cwd = root, mergeErrIntoOut = true).out.trim()

      assertNoDiff(
        fixOutput,
        """Running built-in rules...
          |Extracting directives from Main.scala
          |Extracting directives from project.scala
          |Writing project.scala
          |Removing directives from Main.scala
          |Built-in rules completed.""".stripMargin
      )

      val projectFileContents = os.read(root / projectFileName)
      val mainFileContents    = os.read(root / mainFileName)

      assertNoDiff(
        projectFileContents,
        """// Main
          |//> using objectWrapper
          |
          |//> using dependency "com.lihaoyi::os-lib:0.9.1"
          |//> using dependency "com.lihaoyi::pprint:0.6.6"
          |//> using dependency "com.lihaoyi::upickle:3.1.2"
          |
          |""".stripMargin
      )

      assertNoDiff(
        mainFileContents,
        """package com.foo.main
          |
          |object Main extends App {
          |  println(os.pwd)
          |}
          |""".stripMargin
      )

      val runProc = os.proc(TestUtil.cli, "--power", "compile", ".", extraOptions)
        .call(cwd = root, stderr = os.Pipe)

      expect(!runProc.err.trim().contains("Using directives detected in multiple files"))
    }
  }

  test("built-in rules for script with shebang") {
    val mainFileName = "main.sc"
    val inputs = TestInputs(
      os.rel / mainFileName ->
        s"""#!/usr/bin/env -S scala-cli shebang
           |
           |//> using objectWrapper
           |//> using dep com.lihaoyi::os-lib:0.9.1 com.lihaoyi::upickle:3.1.2
           |
           |println(os.pwd)
           |""".stripMargin,
      os.rel / projectFileName ->
        s"""//> using deps "com.lihaoyi::pprint:0.6.6"
           |""".stripMargin
    )

    inputs.fromRoot { root =>

      val fixOutput =
        os.proc(
          TestUtil.cli,
          "--power",
          "fix",
          ".",
          "-v",
          "-v",
          extraOptions,
          enableRulesOptions(enableScalafix = false)
        )
          .call(cwd = root, mergeErrIntoOut = true).out.trim()

      assertNoDiff(
        fixOutput,
        """Running built-in rules...
          |Extracting directives from project.scala
          |Extracting directives from main.sc
          |Writing project.scala
          |Removing directives from main.sc
          |Built-in rules completed.""".stripMargin
      )

      val projectFileContents = os.read(root / projectFileName)
      val mainFileContents    = os.read(root / mainFileName)

      assertNoDiff(
        projectFileContents,
        """// Main
          |//> using objectWrapper
          |
          |//> using dependency "com.lihaoyi::os-lib:0.9.1"
          |//> using dependency "com.lihaoyi::pprint:0.6.6"
          |//> using dependency "com.lihaoyi::upickle:3.1.2"
          |
          |""".stripMargin
      )

      assertNoDiff(
        mainFileContents,
        """#!/usr/bin/env -S scala-cli shebang
          |
          |println(os.pwd)
          |""".stripMargin
      )

      val runProc = os.proc(TestUtil.cli, "--power", "compile", ".", extraOptions)
        .call(cwd = root, stderr = os.Pipe)

      expect(!runProc.err.trim().contains("Using directives detected in multiple files"))
    }
  }

  test("built-in rules with test scope") {
    val mainSubPath = os.rel / "src" / "Main.scala"
    val testSubPath = os.rel / "test" / "MyTests.scala"
    val inputs = TestInputs(
      mainSubPath ->
        s"""//> using objectWrapper
           |//> using dep com.lihaoyi::os-lib:0.9.1
           |
           |//> using test.dep org.typelevel::cats-core:2.9.0
           |
           |package com.foo.main
           |
           |object Main extends App {
           |  println(os.pwd)
           |}
           |""".stripMargin,
      testSubPath ->
        s"""//> using options -Xasync -Xfatal-warnings
           |//> using dep org.scalameta::munit::0.7.29
           |
           |package com.foo.test.bar
           |
           |class MyTests extends munit.FunSuite {
           |  test("bar") {
           |    assert(2 + 2 == 4)
           |    println("Hello from " + "tests")
           |  }
           |}
           |""".stripMargin,
      os.rel / projectFileName ->
        s"""//> using deps com.lihaoyi::pprint:0.6.6
           |""".stripMargin
    )

    inputs.fromRoot { root =>

      val fixOutput =
        os.proc(
          TestUtil.cli,
          "--power",
          "fix",
          ".",
          "-v",
          "-v",
          extraOptions,
          enableRulesOptions(enableScalafix = false)
        )
          .call(cwd = root, mergeErrIntoOut = true).out.trim()

      assertNoDiff(
        fixOutput,
        """Running built-in rules...
          |Extracting directives from project.scala
          |Extracting directives from src/Main.scala
          |Extracting directives from test/MyTests.scala
          |Writing project.scala
          |Removing directives from src/Main.scala
          |Removing directives from test/MyTests.scala
          |Built-in rules completed.""".stripMargin
      )

      val projectFileContents = os.read(root / projectFileName)
      val mainFileContents    = os.read(root / mainSubPath)
      val testFileContents    = os.read(root / testSubPath)

      assertNoDiff(
        projectFileContents,
        """// Main
          |//> using objectWrapper
          |//> using dependency "com.lihaoyi::os-lib:0.9.1" "com.lihaoyi::pprint:0.6.6"
          |
          |// Test
          |//> using test.options "-Xasync" "-Xfatal-warnings"
          |//> using test.dependency "org.scalameta::munit::0.7.29" "org.typelevel::cats-core:2.9.0"
          |""".stripMargin
      )

      assertNoDiff(
        mainFileContents,
        """package com.foo.main
          |
          |object Main extends App {
          |  println(os.pwd)
          |}
          |""".stripMargin
      )

      assertNoDiff(
        testFileContents,
        """package com.foo.test.bar
          |
          |class MyTests extends munit.FunSuite {
          |  test("bar") {
          |    assert(2 + 2 == 4)
          |    println("Hello from " + "tests")
          |  }
          |}
          |""".stripMargin
      )

      val runProc = os.proc(TestUtil.cli, "--power", "compile", ".", extraOptions)
        .call(cwd = root, stderr = os.Pipe)

      expect(!runProc.err.trim().contains("Using directives detected in multiple files"))
    }
  }

  test("built-in rules with complex inputs") {
    val mainSubPath = os.rel / "src" / "Main.scala"
    val testSubPath = os.rel / "test" / "MyTests.scala"

    val withUsedTargetSubPath = os.rel / "src" / "UsedTarget.scala"
    val withUsedTargetContents =
      s"""//> using target.scala 3.3.0
         |//> using dep com.lihaoyi::upickle:3.1.2
         |case class UsedTarget(x: Int)
         |""".stripMargin
    val withUnusedTargetSubPath = os.rel / "src" / "UnusedTarget.scala"
    val withUnusedTargetContents =
      s"""//> using target.scala 2.13
         |//> using dep com.lihaoyi::upickle:3.1.2
         |case class UnusedTarget(x: Int)
         |""".stripMargin

    val includedInputs = TestInputs(
      os.rel / "Included.scala" ->
        """//> using options -Werror
          |
          |case class Included(x: Int)
          |""".stripMargin
    )

    includedInputs.fromRoot { includeRoot =>
      val includePath = (includeRoot / "Included.scala").toString.replace("\\", "\\\\")

      val inputs = TestInputs(
        mainSubPath ->
          s"""//> using platforms jvm
             |//> using scala 3.3.0
             |//> using jvm 17
             |//> using objectWrapper
             |//> using dep com.lihaoyi::os-lib:0.9.1
             |//> using file $includePath
             |
             |//> using test.dep org.typelevel::cats-core:2.9.0
             |
             |package com.foo.main
             |
             |object Main extends App {
             |  println(os.pwd)
             |}
             |""".stripMargin,
        withUsedTargetSubPath   -> withUsedTargetContents,
        withUnusedTargetSubPath -> withUnusedTargetContents,
        testSubPath ->
          s"""//> using options -Xasync -Xfatal-warnings
             |//> using dep org.scalameta::munit::0.7.29
             |//> using scala 3.2.2
             |
             |package com.foo.test.bar
             |
             |class MyTests extends munit.FunSuite {
             |  test("bar") {
             |    assert(2 + 2 == 4)
             |    println("Hello from " + "tests")
             |  }
             |}
             |""".stripMargin,
        os.rel / projectFileName ->
          s"""//> using deps com.lihaoyi::pprint:0.6.6
             |
             |//> using publish.ci.password env:PUBLISH_PASSWORD
             |//> using publish.ci.secretKey env:PUBLISH_SECRET_KEY
             |//> using publish.ci.secretKeyPassword env:PUBLISH_SECRET_KEY_PASSWORD
             |//> using publish.ci.user env:PUBLISH_USER
             |""".stripMargin
      )

      inputs.fromRoot { root =>
        val res = os.proc(
          TestUtil.cli,
          "--power",
          "fix",
          ".",
          "--script-snippet",
          "//> using toolkit default",
          "-v",
          "-v",
          extraOptions,
          enableRulesOptions(enableScalafix = false)
        ).call(cwd = root, stderr = os.Pipe)

        assertNoDiff(
          res.err.trim(),
          s"""Running built-in rules...
             |Extracting directives from project.scala
             |Extracting directives from src/Main.scala
             |Extracting directives from src/UsedTarget.scala
             |Extracting directives from ${includeRoot / "Included.scala"}
             |Extracting directives from snippet
             |Extracting directives from test/MyTests.scala
             |Writing project.scala
             |Removing directives from src/Main.scala
             |Removing directives from test/MyTests.scala
             |  Keeping:
             |    //> using scala 3.2.2
             |Built-in rules completed.""".stripMargin
        )

        val projectFileContents          = os.read(root / projectFileName)
        val mainFileContents             = os.read(root / mainSubPath)
        val testFileContents             = os.read(root / testSubPath)
        val withUsedTargetContentsRead   = os.read(root / withUsedTargetSubPath)
        val withUnusedTargetContentsRead = os.read(root / withUnusedTargetSubPath)

        assertNoDiff(
          projectFileContents,
          s"""// Main
             |//> using scala "3.3.0"
             |//> using platforms "jvm"
             |//> using jvm "17"
             |//> using options "-Werror"
             |//> using files "$includePath"
             |//> using objectWrapper
             |//> using toolkit "default"
             |//> using dependency "com.lihaoyi::os-lib:0.9.1" "com.lihaoyi::pprint:0.6.6"
             |
             |//> using publish.ci.password "env:PUBLISH_PASSWORD"
             |//> using publish.ci.secretKey "env:PUBLISH_SECRET_KEY"
             |//> using publish.ci.secretKeyPassword "env:PUBLISH_SECRET_KEY_PASSWORD"
             |//> using publish.ci.user "env:PUBLISH_USER"
             |
             |// Test
             |//> using test.options "-Xasync" "-Xfatal-warnings"
             |//> using test.dependency "org.scalameta::munit::0.7.29" "org.typelevel::cats-core:2.9.0"
             |""".stripMargin
        )

        assertNoDiff(
          mainFileContents,
          """package com.foo.main
            |
            |object Main extends App {
            |  println(os.pwd)
            |}
            |""".stripMargin
        )

        // Directives with no 'test.' equivalent are retained
        assertNoDiff(
          testFileContents,
          """//> using scala 3.2.2
            |
            |package com.foo.test.bar
            |
            |class MyTests extends munit.FunSuite {
            |  test("bar") {
            |    assert(2 + 2 == 4)
            |    println("Hello from " + "tests")
            |  }
            |}
            |""".stripMargin
        )

        assertNoDiff(withUsedTargetContents, withUsedTargetContentsRead)
        assertNoDiff(withUnusedTargetContents, withUnusedTargetContentsRead)
      }

      assertNoDiff(
        os.read(includeRoot / "Included.scala"),
        """//> using options -Werror
          |
          |case class Included(x: Int)
          |""".stripMargin
      )
    }
  }
}
