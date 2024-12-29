package scala.cli.integration

import scala.concurrent.ExecutionContext.Implicits.global

trait BspTests3Definitions { _: BspTestDefinitions =>
  test("BSP class wrapper for Scala 3") {
    val (script1, script2) = "script1.sc" -> "script2.sc"
    val inputs = TestInputs(
      os.rel / script1 ->
        s"""def main(args: String*): Unit = println("Hello")
           |main()
           |""".stripMargin,
      os.rel / script2 ->
        s"""//> using dep org.scalatest::scalatest:3.2.15
           |
           |import org.scalatest.*, flatspec.*, matchers.*
           |
           |class PiTest extends AnyFlatSpec with should.Matchers {
           |  "pi calculus" should "return a precise enough pi value" in {
           |    math.Pi shouldBe 3.14158d +- 0.001d
           |  }
           |}
           |org.scalatest.tools.Runner.main(Array("-oDF", "-s", classOf[PiTest].getName))""".stripMargin
    )
    testScriptWrappers(inputs)(expectClassWrapper)
  }

  for {
    useDirectives <- Seq(true, false)
    (directive, options) <- Seq(
      ("//> using object.wrapper", Seq("--object-wrapper")),
      ("//> using platform js", Seq("--js"))
    )
    wrapperOptions = if (useDirectives) Nil else options
    testNameSuffix = if (useDirectives) directive else options.mkString(" ")
  } test(s"BSP object wrapper forced with $testNameSuffix") {
    val (script1, script2) = "script1.sc" -> "script2.sc"
    val directiveString    = if (useDirectives) directive else ""
    val inputs = TestInputs(
      os.rel / script1 ->
        s"""$directiveString
           |
           |def main(args: String*): Unit = println("Hello")
           |main()
           |""".stripMargin,
      os.rel / script2 ->
        """println("Hello")
          |""".stripMargin
    )
    testScriptWrappers(inputs, bspOptions = wrapperOptions)(expectObjectWrapper)
  }
}
