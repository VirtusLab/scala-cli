package scala.cli.integration

// format: off
class CompileTests212 extends CompileTestDefinitions(
  scalaVersionOpt = Some(Constants.scala212)
) {
// format: on

  val pluginInputs = TestInputs(
    Seq(
      os.rel / "Plugin.scala" ->
        // Copied from (https://github.com/typelevel/kind-projector/blob/00bf25cef1b7d01d61a3555cccb6cf38fe30e117/src/test/scala/polylambda.scala)
        """object Plugin {
          |  trait ~>[-F[_], +G[_]] {
          |    def apply[A](x: F[A]): G[A]
          |  }
          |  type ToSelf[F[_]] = F ~> F
          |  val kf5 = λ[Map[*, Int] ~> Map[*, Long]](_.map { case (k, v) => (k, v.toLong) }.toMap)
          |  val kf6 = λ[ToSelf[Map[*, Int]]](_.map { case (k, v) => (k, v * 2) }.toMap)
          |}
          |""".stripMargin
    )
  )

  val kindProjectPlugin = "org.typelevel:::kind-projector:0.13.2"

  test("should compile with compiler plugin") {
    pluginInputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "compile",
        extraOptions,
        ".",
        "--compiler-plugin",
        kindProjectPlugin
      ).call(cwd = root).out.text()
    }
  }
}
