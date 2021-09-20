package scala.cli.integration

// format: off
class CompileTests212 extends CompileTestDefinitions(
  scalaVersionOpt = Some(Constants.scala212)
) {
// format: on 

  val pluginInputs = TestInputs(
    Seq(
      /** Copyright (c) Erik Osheim, 2011-2021
        *
        * Licensed under the MIT license.
        *
        * Permission is hereby granted, free of charge, to any person obtaining a copy of this
        * software and associated documentation files (the "Software"), to deal in the Software
        * without restriction, including without limitation the rights to use, copy, modify, merge,
        * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons
        * to whom the Software is furnished to do so, subject to the following conditions:
        *
        * The above copyright notice and this permission notice shall be included in all copies or
        * substantial portions of the Software.
        *
        * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
        * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
        * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
        * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
        * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
        * DEALINGS IN THE SOFTWARE.
        *
        * Originally copied from
        * (https://github.com/typelevel/kind-projector/blob/main/src/test/scala/polylambda.scala)
        */
      os.rel / "Plugin.scala" ->
        """object Plugin {
          |  trait ~>[-F[_], +G[_]] {
          |    def apply[A](x: F[A]): G[A]
          |  }
          |  type ToSelf[F[_]] = F ~> F
          |  val kf5 = λ[Map[*, Int] ~> Map[*, Long]](_.map { case (k, v) => (k, v.toLong) }.toMap)
          |  val kf6 = λ[ToSelf[Map[*, Int]]](_.map { case (k, v) => (k, v * 2) }.toMap)
          |}
          |""".stripMargin,
      os.rel / "scala.conf" -> ""
    )
  )

  val kindProjectPlugin = "org.typelevel:::kind-projector:0.13.2"

  test("should compile with compiler plugin") {
    pluginInputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "compile",
        extraOptions,
        "--compiler-plugin",
        kindProjectPlugin
      ).call(cwd = root).out.text
    }
  }
}
