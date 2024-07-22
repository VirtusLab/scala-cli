package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

trait ReplTests3StableDefinitions { _: ReplTestDefinitions =>
  if (!actualScalaVersion.equals(actualMaxAmmoniteScalaVersion)) {
    lazy val defaultScalaVersionString =
      s" with Scala $actualScalaVersion (the default version, may downgrade)"
    test(s"ammonite$defaultScalaVersionString") {
      ammoniteTest(useMaxAmmoniteScalaVersion = false)
    }

    test(s"ammonite scalapy$defaultScalaVersionString") {
      ammoniteScalapyTest(useMaxAmmoniteScalaVersion = false)
    }

    test(s"ammonite with test scope sources$defaultScalaVersionString") {
      ammoniteTestScope(useMaxAmmoniteScalaVersion = false)
    }
  }

  test("https://github.com/scala/scala3/issues/21229") {
    TestInputs(
      os.rel / "Pprint.scala" ->
        """//> using dep "com.lihaoyi::pprint::0.9.0"
          |package stuff
          |import scala.quoted.*
          |def foo = pprint(1)
          |inline def bar = pprint(1)
          |inline def baz = ${ bazImpl }
          |def bazImpl(using Quotes) = '{ pprint(1) }
          |""".stripMargin
    ).fromRoot { root =>
      val ammArgs = Seq("-c", "println(stuff.baz)")
        .map {
          if (Properties.isWin)
            a => if (a.contains(" ")) "\"" + a.replace("\"", "\\\"") + "\"" else a
          else
            identity
        }
        .flatMap(arg => Seq("--ammonite-arg", arg))
      // FIXME: test this on standard Scala 3 REPL, rather than just Ammonite
      val res = os.proc(TestUtil.cli, "repl", ".", "--power", "--amm", ammArgs, extraOptions)
        .call(cwd = root)
      expect(res.out.trim().nonEmpty)
    }
  }
}
