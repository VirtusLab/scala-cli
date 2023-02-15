package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

abstract class ExportJsonTestDefinitions(val scalaVersionOpt: Option[String])
    extends ScalaCliSuite with TestScalaVersionArgs {
  private def readJson(path: os.ReadablePath): String =
    os.read(path)
      .replaceAll("\\s", "")
      .replaceAll(
        "ivy:Pattern[^\"]*file:[^\"]*(scalacli|ScalaCli)[^\"]*/local-repo[^\"]*",
        "ivy:file:.../scalacli/local-repo/..."
      )
      .replaceAll(
        "ivy:Pattern[^\"]*file:[^\"]*\\.ivy2/local[^\"]*",
        "ivy:file:.../.ivy2/local/"
      )

  test("export json") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """//> using lib "com.lihaoyi::os-lib:0.7.8"
          |
          |object Main {
          |  def main(args: Array[String]): Unit =
          |    println("Hello")
          |}
          |""".stripMargin
    )

    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "--power", "export", "--json", ".")
        .call(cwd = root)

      val fileContents = readJson(root / "dest" / "export.json")

      expect(fileContents ==
        """{
          |"scalaVersion":"3.2.2",
          |"platform":"JVM",
          |"scopes": [
          | {
          |   "scopeName": "main",
          |   "sources": ["Main.scala"],
          |   "dependencies": ["com.lihaoyi::os-lib:0.7.8"],
          |   "resolvers": [
          |     "https://repo1.maven.org/maven2",
          |     "ivy:file:.../scalacli/local-repo/...",
          |     "ivy:file:.../.ivy2/local/"
          |   ]
          | },
          | {
          |   "scopeName": "test",
          |   "dependencies": ["com.lihaoyi::os-lib:0.7.8"],
          |   "resolvers": [
          |     "https://repo1.maven.org/maven2",
          |     "ivy:file:.../scalacli/local-repo/...",
          |     "ivy:file:.../.ivy2/local/"
          |   ]
          | }
          |]
          |}
          |""".replaceAll("\\s|\\|", ""))
    }
  }

  test("export json with version options") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """//> using option "-Xasync"
          |//> using lib "com.lihaoyi::os-lib:0.7.8"
          |//> using plugin "org.typelevel:::kind-projector:0.13.2"
          |
          |object Main {
          |  def main(args: Array[String]): Unit =
          |    println("Hello")
          |}
          |""".stripMargin,
      os.rel / "unit.test.scala" ->
        """//> using repository "sonatype:snapshots"
          |//> using resourceDir "./resources"
          |
          |""".stripMargin
    )

    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "--power", "export", "--json", ".", "-S", "2.13", "--native")
        .call(cwd = root)

      val fileContents = readJson(root / "dest" / "export.json")
        .replaceAll(
          "\"resourcesDirs\":\\[\"[^\"]*resources\"\\]",
          "\"resourcesDirs\":[\"./resources\"]"
        )

      expect(fileContents ==
        """{
          |"scalaVersion":"2.13",
          |"platform":"Native",
          |"scalacOptions":["-Xasync"],
          |"scalaCompilerPlugins":["org.typelevel:::kind-projector:0.13.2"],
          |"scalaNativeVersion":"0.4.9",
          |"scopes": [
          | {
          |   "scopeName": "main",
          |   "sources": ["Main.scala"],
          |   "dependencies": [
          |     "com.lihaoyi::os-lib:0.7.8"
          |   ],
          |   "resolvers": [
          |     "https://repo1.maven.org/maven2",
          |     "ivy:file:.../scalacli/local-repo/...",
          |     "ivy:file:.../.ivy2/local/"
          |   ]
          | },
          | {
          |   "scopeName": "test",
          |   "sources":["unit.test.scala"],
          |   "dependencies": [
          |     "com.lihaoyi::os-lib:0.7.8"
          |   ],
          |   "resolvers": [
          |     "https://oss.sonatype.org/content/repositories/snapshots",
          |     "https://repo1.maven.org/maven2",
          |     "ivy:file:.../scalacli/local-repo/...",
          |     "ivy:file:.../.ivy2/local/"
          |   ],
          |   "resourcesDirs":["./resources"]
          | }
          |]
          |}
          |""".replaceAll("\\s|\\|", ""))
    }
  }

}
