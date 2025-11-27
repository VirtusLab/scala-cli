package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

trait ReplAmmoniteTests3StableDefinitions {
  this: ReplTestDefinitions & ReplAmmoniteTestDefinitions =>
  test(s"$ammonitePrefix https://github.com/scala/scala3/issues/21229$ammoniteMaxVersionString") {
    // FIXME: test this on standard Scala 3 REPL, rather than just Ammonite
    runInAmmoniteRepl(
      codeToRunInRepl = "println(stuff.baz)",
      testInputs = TestInputs(
        os.rel / "Pprint.scala" ->
          """//> using dep com.lihaoyi::pprint::0.9.0
            |package stuff
            |import scala.quoted.*
            |def foo = pprint(1)
            |inline def bar = pprint(1)
            |inline def baz = ${ bazImpl }
            |def bazImpl(using Quotes) = '{ pprint(1) }
            |""".stripMargin
      )
    )(res => expect(res.out.trim().nonEmpty))
  }

  test(s"$ammonitePrefix as jar$ammoniteMaxVersionString") {
    val inputs = TestInputs(
      os.rel / "CheckCp.scala" ->
        """//> using dep com.lihaoyi::os-lib:0.9.1
          |package checkcp
          |object CheckCp {
          |  def hasDir: Boolean =
          |    sys.props("java.class.path")
          |      .split(java.io.File.pathSeparator)
          |      .toVector
          |      .map(os.Path(_, os.pwd))
          |      .exists(os.isDir(_))
          |}
          |""".stripMargin
    )
    val code = """println("hasDir=" + checkcp.CheckCp.hasDir)"""
    runInAmmoniteRepl(codeToRunInRepl = code, testInputs = inputs) {
      res => expect(res.out.trim() == "hasDir=true")
    }
    runInAmmoniteRepl(codeToRunInRepl = code, testInputs = inputs, cliOptions = Seq("--as-jar")) {
      res => expect(res.out.trim() == "hasDir=false")
    }
  }
}
