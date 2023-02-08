package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

abstract class ExportJsonTestDefinitions(val scalaVersionOpt: Option[String])
    extends ScalaCliSuite with TestScalaVersionArgs {
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

      val fileContents = os.read(root / "dest" / "export.json")
        .replaceAll("\\s", "")
        .replaceAll(
          "\"location\":\"file:[^}]*(scalacli|ScalaCli)[^}]*/local-repo[^}]*\"",
          "\"location\":\"file:.../scalacli/local-repo/...\""
        )
        .replaceAll(
          "\"location\":\"file:[^}]*\\.ivy2/local[^}]*\"",
          "\"location\":\"file:.../.ivy2/local/\""
        )

      expect(fileContents ==
        """{
          |"scalaVersion":"3.2.2",
          |"platform":"JVM",
          |"mainDeps":[
          | {
          |   "groupId":"com.lihaoyi",
          |   "artifactId":{"name":"os-lib","fullName":"os-lib_3"},
          |   "version":"0.7.8"
          | }
          |],
          |"testDeps":[
          | {
          |   "groupId":"com.lihaoyi",
          |   "artifactId":{"name":"os-lib","fullName":"os-lib_3"},
          |   "version":"0.7.8"
          | }
          |],
          |"mainSources":["Main.scala"],
          |"resolvers":[
          | {
          |   "name":"IvyRepository",
          |   "location":"file:.../scalacli/local-repo/..."
          | },
          | {
          |   "name":"IvyRepository",
          |   "location":"file:.../.ivy2/local/"
          | },
          | {
          |   "name":"MavenRepository",
          |   "location":"https://repo1.maven.org/maven2"
          | }
          |]
          |}
          |""".replaceAll("\\s|\\|", ""))
    }
  }
}
