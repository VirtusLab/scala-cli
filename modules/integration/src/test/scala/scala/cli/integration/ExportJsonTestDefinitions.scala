package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

abstract class ExportJsonTestDefinitions extends ScalaCliSuite with TestScalaVersionArgs {
  _: TestScalaVersion =>
  private def readJson(path: os.ReadablePath): String =
    readJson(os.read(path))

  private def readJson(json: String): String =
    json
      .replaceAll("\\s", "")
      .replaceAll(
        "ivy:file:[^\"]*(scalacli|ScalaCli)[^\"]*/local-repo[^\"]*",
        "ivy:file:.../scalacli/local-repo/..."
      )
      .replaceAll(
        "ivy:file:[^\"]*\\.ivy2/local[^\"]*",
        "ivy:file:.../.ivy2/local/"
      )

  private def withEscapedBackslashes(s: os.Path): String =
    s.toString.replaceAll("\\\\", "\\\\\\\\")

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
      TestUtil.initializeGit(root, "v1.1.2")

      val exportJsonProc =
        // Test --power placed after subcommand name
        os.proc(TestUtil.cli, "export", "--power", "--json", ".", "--jvm", "adopt:11")
          .call(cwd = root)

      val jsonContents = readJson(exportJsonProc.out.text())

      expect(jsonContents ==
        s"""{
          |"projectVersion":"1.1.2",
          |"scalaVersion":"${Constants.scala3Next}",
          |"platform":"JVM",
          |"jvmVersion":"adopt:11",
          |"scopes": [[
          | "main",
          | {
          |   "sources": ["${withEscapedBackslashes(root / "Main.scala")}"],
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
          |]]
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
          |//> using scala 3.2.2
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
      val exportJsonProc = os.proc(TestUtil.cli, "--power", "export", "--json", ".", "--native")
        .call(cwd = root)

      val jsonContents = readJson(exportJsonProc.out.text())

      expect(jsonContents ==
        s"""{
          |"scalaVersion":"3.2.2",
          |"platform":"Native",
          |"scalaNativeVersion":"${Constants.scalaNativeVersion}",
          |"scopes": [[
          | "main",
          | {
          |   "sources": ["${withEscapedBackslashes(root / "Main.scala")}"],
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
          | }], [
          | "test",
          | {
          |   "sources":["${withEscapedBackslashes(root / "unit.test.scala")}"],
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
          |   "resourceDirs":["${withEscapedBackslashes(root / "resources")}"],
          |   "customJarsDecls":["${withEscapedBackslashes(root / "TEST.jar")}"]
          | }
          |]]
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
      val exportJsonProc = os.proc(
        TestUtil.cli,
        "--power",
        "export",
        "--json",
        "--output",
        "json_dir",
        ".",
        "--js-es-version",
        "es2015"
      )
        .call(cwd = root)

      expect(exportJsonProc.out.text().isEmpty)

      val fileContents = readJson(root / "json_dir" / "export.json")

      expect(fileContents ==
        s"""{
          |"scalaVersion": "3.1.3",
          |"platform": "JS",
          |"scalaJsVersion": "${Constants.scalaJsVersion}",
          |"jsEsVersion":"es2015",
          |"scopes": [[
          | "main",
          | {
          |   "sources": ["${withEscapedBackslashes(root / "Main.scala")}"],
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
          |]]
          |}
          |""".replaceAll("\\s|\\|", ""))

      val exportToExistingProc = os.proc(
        TestUtil.cli,
        "--power",
        "export",
        "--json",
        "--output",
        "json_dir",
        ".",
        "--js-es-version",
        "es2015"
      )
        .call(cwd = root, check = false, mergeErrIntoOut = true)

      expect(exportToExistingProc.exitCode != 0)
      expect(
        exportToExistingProc.out.text().contains(s"Error: ${root / "json_dir"} already exists.")
      )
    }
  }

}
