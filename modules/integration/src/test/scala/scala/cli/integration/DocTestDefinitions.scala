package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

abstract class DocTestDefinitions(val scalaVersionOpt: Option[String])
    extends munit.FunSuite with TestScalaVersionArgs {

  private lazy val extraOptions = scalaVersionArgs ++ TestUtil.extraOptions

  test("generate static scala doc") {
    val dest = os.rel / "doc-static"
    val inputs = TestInputs(
      Seq(
        os.rel / "lib" / "Messages.scala" ->
          """package lib
            |
            |object Messages {
            |  def msg = "Hello"
            |}
            |""".stripMargin,
        os.rel / "simple.sc" ->
          """val msg = lib.Messages.msg
            |println(msg)
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "doc", extraOptions, ".", "-o", dest).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val expectedDestDocPath = root / dest
      expect(os.isDir(expectedDestDocPath))
      val expectedEntries =
        if (actualScalaVersion.startsWith("2."))
          Seq(
            "index.html",
            "lib/Messages$.html",
            "simple$.html"
          )
        else
          Seq(
            "index.html",
            "inkuire-db.json",
            "_empty_/simple$.html",
            "lib/Messages$.html"
          )
      val entries =
        os.walk(root / dest).map(_.relativeTo(expectedDestDocPath)).map(_.toString()).toList
      expect(expectedEntries.forall(e => entries.contains(e)))
    }
  }
}
