package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

trait DocScalaJsTestDefinitions { this: DocTestDefinitions & TestScalaVersion =>

  // Scala 2 + Scala.js goes through the Scala 2 scaladoc, a different code path.
  if !actualScalaVersion.startsWith("2.") then
    test("generate scala doc for a Scala.js project") {
      val dest = os.rel / "doc-js"
      TestInputs(
        os.rel / "Hello.scala" ->
          """/** Greeter. */
            |object Hello {
            |  def greet: String = "hi"
            |}
            |""".stripMargin
      ).fromRoot { root =>
        os.proc(TestUtil.cli, "doc", extraOptions, "--js", ".", "-o", dest).call(
          cwd = root,
          stdin = os.Inherit,
          stdout = os.Inherit
        )
        expect(os.isDir(root / dest))
        expect(os.list(root / dest).exists(_.last.endsWith(".html")))
      }
    }

  // Unlike the smoke test above, this one calls a Scala.js API (`js.Promise.then`) whose erased
  // signature contains a pseudo-union (`js.UndefOr`). Such signatures only match what is recorded
  // in TASTy when `-scalajs` is passed on, so this is what actually detects the flag being dropped.
  // Scaladoc only honours `-scalajs` in its TASTy-reading run since 3.6.3, so on older versions
  // (including 3.3 LTS) this fails regardless of what Scala CLI passes.
  if !actualScalaVersion.startsWith("2.") && isScala363OrNewer then
    test("generate scala doc for a Scala.js project using a pseudo-union API") {
      val dest = os.rel / "doc-js-tasty"
      TestInputs(
        os.rel / "Promises.scala" ->
          """import scala.scalajs.js.Promise
            |
            |/** Promise helpers. */
            |object Promises:
            |  /** Prints the resolved value. */
            |  def printResolved(): Unit = Promise.resolve(25).`then`(println(_))
            |""".stripMargin
      ).fromRoot { root =>
        val res = os.proc(TestUtil.cli, "doc", extraOptions, "--js", ".", "-o", dest)
          .call(cwd = root, check = false, mergeErrIntoOut = true)
        val output = res.out.text()
        expect(!output.contains("at readTasty"))
        expect(!output.contains("undefined:"))
        expect(res.exitCode == 0)
        expect(os.isDir(root / dest))
        expect(os.list(root / dest).exists(_.last.endsWith(".html")))
      }
    }
}
