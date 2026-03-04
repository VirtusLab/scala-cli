package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class PackageTestsDefault extends PackageTestDefinitions with TestDefault {
  test("reuse run native binary") {
    TestUtil.retryOnCi() {
      val inputs = TestInputs(
        os.rel / "Hello.scala" ->
          """object Hello {
            |  def main(args: Array[String]): Unit =
            |    println("Hello")
            |}
            |""".stripMargin
      )
      inputs.fromRoot { root =>
        val runRes = os.proc(TestUtil.cli, "run", "--native", ".", extraOptions)
          .call(cwd = root)
        val runOutput = runRes.out.trim().linesIterator.filter(!_.startsWith("[info] ")).toVector
        expect(runOutput == Seq("Hello"))

        val packageRes =
          os.proc(TestUtil.cli, "--power", "package", "--native", ".", "-o", "hello", extraOptions)
            .call(cwd = root, mergeErrIntoOut = true)
        val packageOutput    = packageRes.out.trim()
        val topPackageOutput =
          packageOutput.linesIterator.takeWhile(!_.startsWith("Wrote ")).toVector
        // no compilation or Scala Native pipeline output, as this should just re-use what the run command wrote
        expect(topPackageOutput.forall(!_.startsWith("[info] ")))
      }
    }
  }

}
