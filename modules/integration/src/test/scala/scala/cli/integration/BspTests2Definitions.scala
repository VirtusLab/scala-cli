package scala.cli.integration

import scala.concurrent.ExecutionContext.Implicits.global

trait BspTests2Definitions { _: BspTestDefinitions =>
  for {
    useDirectives <- Seq(true, false)
    (directive, options) <- Seq(
      (s"//> using scala $actualScalaVersion", Seq("--scala", actualScalaVersion))
    )
    extraOptionsOverride =
      if (useDirectives) TestUtil.extraOptions else TestUtil.extraOptions ++ options
    testNameSuffix = if (useDirectives) directive else options.mkString(" ")
  } test(s"BSP App object wrapper forced with $testNameSuffix") {
    val (script1, script2) = "script1.sc" -> "script2.sc"
    val directiveString    = if (useDirectives) directive else ""
    val inputs = TestInputs(
      os.rel / script1 ->
        s"""//> using platform js
           |$directiveString
           |
           |def main(args: String*): Unit = println("Hello")
           |main()
           |""".stripMargin,
      os.rel / script2 ->
        """println("Hello")
          |""".stripMargin
    )
    testScriptWrappers(inputs, extraOptionsOverride = extraOptionsOverride)(expectAppWrapper)
  }
}
