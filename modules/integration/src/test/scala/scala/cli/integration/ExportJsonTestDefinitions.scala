package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

abstract class ExportJsonTestDefinitions(val scalaVersionOpt: Option[String])
    extends ScalaCliSuite with TestScalaVersionArgs {
  private def readJson(path: os.ReadablePath): String =
    os.read(path)
      .replaceAll("\\s", "")
      .replaceAll(
        "ivy:file:[^\"]*(scalacli|ScalaCli)[^\"]*/local-repo[^\"]*",
        "ivy:file:.../scalacli/local-repo/..."
      )
      .replaceAll(
        "ivy:file:[^\"]*\\.ivy2/local[^\"]*",
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
      os.proc(TestUtil.cli, "--power", "export", "--json", ".", "--jvm", "adopt:11")
        .call(cwd = root)

      val fileContents = readJson(root / "dest" / "export.json")

      expect(fileContents ==
        """{
          |"scalaVersion":"3.2.2",
          |"platform":"JVM",
          |"jvmVersion":"adopt:11",
          |"scopes": {
          | "main": {
          |   "sources": ["Main.scala"],
          |   "dependencies": [
          |     {
          |       "groupId":"com.lihaoyi",
          |       "artifactId": {
          |         "name":"os-lib",
          |         "fullName": "os-lib_3"
          |       },
          |       "version":"0.7.8"
          |     }
          |   ],
          |   "resolvers": [
          |     "https://repo1.maven.org/maven2",
          |     "ivy:file:.../scalacli/local-repo/...",
          |     "ivy:file:.../.ivy2/local/"
          |   ]
          | }
          |}
          |}
          |""".replaceAll("\\s|\\|", ""))
    }
  }

  test("export json with test scope") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """//> using lib "com.lihaoyi::os-lib:0.7.8"
          |//> using option "-Xasync"
          |//> using plugin "org.wartremover:::wartremover:3.0.9"
          |
          |object Main {
          |  def main(args: Array[String]): Unit =
          |    println("Hello")
          |}
          |""".stripMargin,
      os.rel / "unit.test.scala" ->
        """//> using repository "sonatype:snapshots"
          |//> using resourceDir "./resources"
          |//> using jar "TEST.jar"
          |""".stripMargin
    )

    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "--power", "export", "--json", ".", "--native")
        .call(cwd = root)

      val fileContents = readJson(root / "dest" / "export.json")
        .replaceAll(
          "\"resourcesDirs\":\\[\"[^\"]*resources\"\\]",
          "\"resourcesDirs\":[\"./resources\"]"
        )
        .replaceAll(
          "\"customJarsDecls\":\\[\"[^\"]*TEST.jar\"\\]",
          "\"customJarsDecls\":[\"./TEST.jar\"]"
        )

      expect(fileContents ==
        """{
          |"scalaVersion":"3.2.2",
          |"platform":"Native",
          |"scalaNativeVersion":"0.4.9",
          |"scopes": {
          | "main": {
          |   "sources": ["Main.scala"],
          |   "scalacOptions":["-Xasync"],
          |   "scalaCompilerPlugins": [
          |     {
          |       "groupId": "org.wartremover",
          |       "artifactId": {
          |         "name": "wartremover",
          |         "fullName": "wartremover_3.2.2"
          |       },
          |       "version": "3.0.9"
          |     }
          |   ],
          |   "dependencies": [
          |     {
          |       "groupId":"com.lihaoyi",
          |       "artifactId": {
          |         "name":"os-lib",
          |         "fullName": "os-lib_3"
          |       },
          |       "version":"0.7.8"
          |     }
          |   ],
          |   "resolvers": [
          |     "https://repo1.maven.org/maven2",
          |     "ivy:file:.../scalacli/local-repo/...",
          |     "ivy:file:.../.ivy2/local/"
          |   ]
          | },
          | "test": {
          |   "sources":["unit.test.scala"],
          |   "scalacOptions":["-Xasync"],
          |   "scalaCompilerPlugins": [
          |     {
          |       "groupId": "org.wartremover",
          |       "artifactId": {
          |         "name": "wartremover",
          |         "fullName": "wartremover_3.2.2"
          |       },
          |       "version": "3.0.9"
          |     }
          |   ],
          |   "dependencies": [
          |     {
          |       "groupId": "com.lihaoyi",
          |       "artifactId": {
          |         "name":"os-lib",
          |         "fullName": "os-lib_3"
          |       },
          |       "version": "0.7.8"
          |     }
          |   ],
          |   "resolvers": [
          |     "https://oss.sonatype.org/content/repositories/snapshots",
          |     "https://repo1.maven.org/maven2",
          |     "ivy:file:.../scalacli/local-repo/...",
          |     "ivy:file:.../.ivy2/local/"
          |   ],
          |   "resourcesDirs":["./resources"],
          |   "customJarsDecls":["./TEST.jar"]
          | }
          |}
          |}
          |""".replaceAll("\\s|\\|", ""))
    }
  }

  test("export json with js") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """//> using scala "3.1.3"
          |//> using platform "scala-js"
          |//> using lib "com.lihaoyi::os-lib:0.7.8"
          |//> using option "-Xasync"
          |//> using plugin "org.wartremover:::wartremover:3.0.9"
          |
          |object Main {
          |  def main(args: Array[String]): Unit =
          |    println("Hello")
          |}
          |""".stripMargin
    )

    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "--power", "export", "--json", ".", "--js-es-version", "es2015")
        .call(cwd = root)

      val fileContents = readJson(root / "dest" / "export.json")

      expect(fileContents ==
        s"""{
          |"scalaVersion": "3.1.3",
          |"platform": "JS",
          |"scalaJsVersion": "${Constants.scalaJsVersion}",
          |"jsEsVersion":"es2015",
          |"scopes": {
          | "main": {
          |   "sources": ["Main.scala"],
          |   "scalacOptions": ["-Xasync"],
          |   "scalaCompilerPlugins": [
          |     {
          |       "groupId": "org.wartremover",
          |       "artifactId": {
          |         "name": "wartremover",
          |         "fullName": "wartremover_3.1.3"
          |       },
          |       "version": "3.0.9"
          |     }
          |   ],
          |   "dependencies": [
          |     {
          |       "groupId": "com.lihaoyi",
          |       "artifactId": {
          |         "name": "os-lib",
          |         "fullName": "os-lib_3"
          |       },
          |       "version": "0.7.8"
          |     }
          |   ],
          |   "resolvers": [
          |     "https://repo1.maven.org/maven2",
          |     "ivy:file:.../scalacli/local-repo/...",
          |     "ivy:file:.../.ivy2/local/"
          |   ]
          | }
          |}
          |}
          |""".replaceAll("\\s|\\|", ""))
    }
  }

}
