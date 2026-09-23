package scala.cli.commands.fix

import scala.build.input.ScalaCliInvokeData
import scala.build.options.BuildOptions
import scala.build.tests.{TestInputs, TestLogger}

class BuiltInRulesTests extends munit.FunSuite {

  private def fix(inputs: TestInputs)(f: (os.Path, Boolean => Boolean) => Unit): Unit =
    inputs.withCustomInputs(viaDirectory = true, forcedWorkspaceOpt = None) { (root, in) =>
      def runRules(check: Boolean): Boolean =
        BuiltInRules.runRules(
          inputs = in,
          buildOptions = BuildOptions(),
          check = check,
          logger = TestLogger()
        )(using ScalaCliInvokeData.dummy)

      runRules(check = false)
      f(root, runRules)
    }

  test("directive key aliases are retained") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """//> using dep com.lihaoyi::os-lib:0.9.1
          |
          |object Main extends App { println(os.pwd) }
          |""".stripMargin,
      os.rel / "Other.scala" ->
        """//> using dep com.lihaoyi::pprint:0.6.6
          |
          |object Other
          |""".stripMargin
    )
    fix(inputs) { (root, _) =>
      assertNoDiff(
        os.read(root / "project.scala"),
        """// Main
          |//> using dep com.lihaoyi::os-lib:0.9.1 com.lihaoyi::pprint:0.6.6
          |""".stripMargin
      )
    }
  }

  test("aliases of test directives are retained") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """//> using deps com.lihaoyi::os-lib:0.9.1
          |
          |object Main extends App { println(os.pwd) }
          |""".stripMargin,
      os.rel / "Tests.test.scala" ->
        """//> using test.deps org.scalameta::munit:0.7.29
          |
          |class Tests extends munit.FunSuite
          |""".stripMargin
    )
    fix(inputs) { (root, _) =>
      assertNoDiff(
        os.read(root / "project.scala"),
        """// Main
          |//> using deps com.lihaoyi::os-lib:0.9.1
          |
          |// Test
          |//> using test.deps org.scalameta::munit:0.7.29
          |""".stripMargin
      )
    }
  }

  test("the most often used alias of a key wins") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """//> using dependency com.lihaoyi::os-lib:0.9.1
          |
          |object Main extends App { println(os.pwd) }
          |""".stripMargin,
      os.rel / "Other.scala" ->
        """//> using dep com.lihaoyi::pprint:0.6.6
          |
          |object Other
          |""".stripMargin,
      os.rel / "YetAnother.scala" ->
        """//> using dep com.lihaoyi::upickle:3.1.2
          |
          |object YetAnother
          |""".stripMargin
    )
    fix(inputs) { (root, _) =>
      assertNoDiff(
        os.read(root / "project.scala"),
        """// Main
          |//> using dep com.lihaoyi::os-lib:0.9.1 com.lihaoyi::pprint:0.6.6 com.lihaoyi::upickle:3.1.2
          |""".stripMargin
      )
    }
  }

  test("a tie between aliases is won by the one written first") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """//> using deps com.lihaoyi::os-lib:0.9.1
          |//> using dep com.lihaoyi::pprint:0.6.6
          |
          |object Main extends App { println(os.pwd) }
          |""".stripMargin,
      os.rel / "Other.scala" ->
        """object Other
          |""".stripMargin
    )
    fix(inputs) { (root, _) =>
      assertNoDiff(
        os.read(root / "project.scala"),
        """// Main
          |//> using deps com.lihaoyi::os-lib:0.9.1 com.lihaoyi::pprint:0.6.6
          |""".stripMargin
      )
    }
  }

  test("test scoped aliases carrying no 'test.' prefix are normalized") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """//> using testDependency com.lihaoyi::os-lib:0.9.1
          |
          |object Main extends App { println(os.pwd) }
          |""".stripMargin,
      os.rel / "Tests.test.scala" ->
        """//> using testDependency org.scalameta::munit:0.7.29
          |
          |class Tests extends munit.FunSuite
          |""".stripMargin
    )
    fix(inputs) { (root, _) =>
      assertNoDiff(
        os.read(root / "project.scala"),
        """// Test
          |//> using test.dependencies com.lihaoyi::os-lib:0.9.1 org.scalameta::munit:0.7.29
          |""".stripMargin
      )
      assertNoDiff(os.read(root / "Tests.test.scala"), "class Tests extends munit.FunSuite\n")
    }
  }

  test("legacy aliases with no 'test.' counterpart fall back to the default spelling") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """//> using lib com.lihaoyi::pprint:0.6.6
          |
          |object Main extends App { println(os.pwd) }
          |""".stripMargin,
      os.rel / "Tests.test.scala" ->
        """//> using lib com.lihaoyi::os-lib:0.9.1
          |
          |class Tests extends munit.FunSuite
          |""".stripMargin
    )
    fix(inputs) { (root, _) =>
      assertNoDiff(
        os.read(root / "project.scala"),
        """// Main
          |//> using dependency com.lihaoyi::pprint:0.6.6
          |
          |// Test
          |//> using test.dependency com.lihaoyi::os-lib:0.9.1
          |""".stripMargin
      )
      assertNoDiff(os.read(root / "Tests.test.scala"), "class Tests extends munit.FunSuite\n")
    }
  }

  test("sources that are only read don't sway the pick, so that fix stays idempotent") {
    val externalSource = os.temp.dir(prefix = "scala-cli-tests-external-") / "External.scala"
    os.write(
      externalSource,
      """//> using dependency a:b:1
        |//> using dependency c:d:1
        |//> using option -Werror
        |
        |object External
        |""".stripMargin
    )
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        s"""//> using dep e:f:1
           |//> using file $externalSource
           |
           |object Main
           |""".stripMargin,
      os.rel / "Other.scala" ->
        """//> using dep g:h:1
          |
          |object Other
          |""".stripMargin
    )
    fix(inputs) { (root, runRules) =>
      val expectedContents =
        s"""// Main
           |//> using options -Werror
           |//> using file $externalSource
           |//> using dep a:b:1 c:d:1 e:f:1 g:h:1
           |""".stripMargin
      assertNoDiff(os.read(root / "project.scala"), expectedContents)

      assert(!runRules(true), "a second run would still change something")
      runRules(false)
      assertNoDiff(os.read(root / "project.scala"), expectedContents)
    }
  }
}
