package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

abstract class ExportJsonTestDefinitions extends ScalaCliSuite with TestScalaVersionArgs {
  this: TestScalaVersion =>
  private def readJson(path: os.ReadablePath): String =
    readJson(os.read(path))

  private def readJson(json: String): String =
    json
      .replaceAll("\\s", "")
      .replaceAll(
        "ivy:file:[^\"]*/local-repo/[^\"]*",
        "ivy:file:.../local-repo/..."
      )
      .replaceAll(
        "ivy:file:[^\"]*\\.ivy2/local[^\"]*",
        "ivy:file:.../.ivy2/local/"
      )
      .replaceAll(
        "\"scalaCliVersion\":(\"[^\"]*\")",
        "\"scalaCliVersion\":\"1.1.1-SNAPSHOT\""
      )

  private def withEscapedBackslashes(s: os.Path): String =
    s.toString.replaceAll("\\\\", "\\\\\\\\")

  test("export json") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """//> using dep com.lihaoyi::os-lib:0.7.8
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
        os.proc(TestUtil.cli, "export", "--power", "--json", ".", "--jvm", "temurin:11")
          .call(cwd = root)

      val jsonContents = readJson(exportJsonProc.out.text())

      val expectedJsonContents =
        s"""{
           |"projectVersion":"1.1.2",
           |"scalaVersion":"${Constants.scala3Next}",
           |"platform":"JVM",
           |"jvmVersion":"temurin:11",
           |"scopes": {
           | "main": {
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
           |     "ivy:file:.../local-repo/...",
           |     "ivy:file:.../.ivy2/local/"
           |   ]
           | }
           |}
           |,"scalaCliVersion":"1.1.1-SNAPSHOT"
           |}
           |""".replaceAll("\\s|\\|", "")
      expect(jsonContents == expectedJsonContents)
    }
  }

  test("export json with test scope") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """//> using dep com.lihaoyi::os-lib:0.7.8
          |//> using option -Xasync
          |//> using plugin org.wartremover:::wartremover:3.0.9
          |//> using scala 3.2.2
          |
          |object Main {
          |  def main(args: Array[String]): Unit =
          |    println("Hello")
          |}
          |""".stripMargin,
      os.rel / "unit.test.scala" ->
        """//> using repository sonatype:snapshots
          |//> using resourceDir ./resources
          |//> using jar TEST.jar
          |""".stripMargin
    )

    inputs.fromRoot { root =>
      val exportJsonProc = os.proc(TestUtil.cli, "--power", "export", "--json", ".", "--native")
        .call(cwd = root)

      val jsonContents = readJson(exportJsonProc.out.text())

      val nativeVersion        = Constants.scalaNativeVersion
      val expectedJsonContents =
        s"""{
           |"scalaVersion":"3.2.2",
           |"platform":"Native",
           |"scalaNativeVersion":"$nativeVersion",
           |"nativeOptions": {
           |  "scalaNativeVersion":"$nativeVersion",
           |  "toolingDependencies": [
           |    {
           |      "groupId":"org.scala-native",
           |      "artifactId":{"name":"scala-native-cli","fullName":"scala-native-cli_2.12"},
           |      "version":"$nativeVersion"
           |    }
           |  ]
           |},
           |"scopes": {
           | "main": {
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
           |   "injectedDependencies": [
           |     {
           |       "groupId":"org.scala-native",
           |       "artifactId":{"name":"javalib_native0.5","fullName":"javalib_native0.5_3"},
           |       "version":"$nativeVersion"
           |     },
           |     {
           |       "groupId":"org.scala-native",
           |       "artifactId":{"name":"nscplugin","fullName":"nscplugin_3.2.2"},
           |       "version":"$nativeVersion"
           |     },
           |     {
           |       "groupId":"org.scala-native",
           |       "artifactId":{"name":"scala3lib_native0.5","fullName":"scala3lib_native0.5_3"},
           |       "version":"3.2.2+$nativeVersion"
           |     }
           |   ],
           |   "resolvers": [
           |     "https://repo1.maven.org/maven2",
           |     "ivy:file:.../local-repo/...",
           |     "ivy:file:.../.ivy2/local/"
           |   ]
           | },
           | "test": {
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
           |   "injectedDependencies": [
           |     {
           |       "groupId":"org.scala-native",
           |       "artifactId":{"name":"javalib_native0.5","fullName":"javalib_native0.5_3"},
           |       "version":"$nativeVersion"
           |     },
           |     {
           |       "groupId":"org.scala-native",
           |       "artifactId":{"name":"nscplugin","fullName":"nscplugin_3.2.2"},
           |       "version":"$nativeVersion"
           |     },
           |     {
           |       "groupId":"org.scala-native",
           |       "artifactId":{"name":"scala3lib_native0.5","fullName":"scala3lib_native0.5_3"},
           |       "version":"3.2.2+$nativeVersion"
           |     },
           |     {
           |       "groupId":"org.scala-native",
           |       "artifactId":{"name":"test-interface_native0.5","fullName":"test-interface_native0.5_3"},
           |       "version":"$nativeVersion"
           |     }
           |   ],
           |   "resolvers": [
           |     "https://oss.sonatype.org/content/repositories/snapshots",
           |     "https://repo1.maven.org/maven2",
           |     "ivy:file:.../local-repo/...",
           |     "ivy:file:.../.ivy2/local/"
           |   ],
           |   "resourceDirs":["${withEscapedBackslashes(root / "resources")}"],
           |   "customJarsDecls":["${withEscapedBackslashes(root / "TEST.jar")}"]
           |  }
           |}
           |,"scalaCliVersion":"1.1.1-SNAPSHOT"
           |}
           |""".replaceAll("\\s|\\|", "")
      expect(jsonContents == expectedJsonContents)
    }
  }

  test("export json injects JVM test-runner into test scope") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """object Main {
          |  def main(args: Array[String]): Unit = println("hi")
          |}
          |""".stripMargin,
      os.rel / "unit.test.scala" ->
        s"""//> using dep org.scalameta::munit::${Constants.munitVersion}
           |
           |class MyTest extends munit.FunSuite { test("ok") { assert(true) } }
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val exportJsonProc =
        os.proc(TestUtil.cli, "--power", "export", "--json", ".", "--jvm", "temurin:17")
          .call(cwd = root)
      val jsonContents     = readJson(exportJsonProc.out.text())
      val expectedFullName = s"test-runner_${Constants.scala3NextPrefix.split('.').head}"
      // The test scope should include both munit and the scala-cli test-runner.
      expect(jsonContents.contains("\"name\":\"test-runner\""))
      expect(jsonContents.contains(s"\"fullName\":\"$expectedFullName\""))
      expect(jsonContents.contains("\"groupId\":\"org.virtuslab.scala-cli\""))
      expect(jsonContents.contains("\"name\":\"munit\""))
    }
  }

  test("export json includes JVM test-runner even when no test framework dep is declared") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """object Main {
          |  def main(args: Array[String]): Unit = println("hi")
          |}
          |""".stripMargin,
      os.rel / "unit.test.scala" ->
        """class MyTest { def foo() = () }
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      val exportJsonProc =
        os.proc(TestUtil.cli, "--power", "export", "--json", ".", "--jvm", "temurin:17")
          .call(cwd = root)
      val jsonContents = readJson(exportJsonProc.out.text())
      expect(jsonContents.contains("\"name\":\"test-runner\""))
      expect(jsonContents.contains("\"groupId\":\"org.virtuslab.scala-cli\""))
    }
  }

  test("export json includes legacy JVM test-runner for Scala 2.12") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """object Main {
          |  def main(args: Array[String]): Unit = println("hi")
          |}
          |""".stripMargin,
      os.rel / "unit.test.scala" ->
        """class MyTest { def foo() = () }
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      val exportJsonProc =
        os.proc(
          TestUtil.cli,
          "--power",
          "export",
          "--json",
          ".",
          "--jvm",
          "temurin:17",
          "--scala",
          Constants.scala212
        )
          .call(cwd = root)
      val jsonContents = readJson(exportJsonProc.out.text())
      expect(jsonContents.contains("\"fullName\":\"test-runner_2.12\""))
      expect(jsonContents.contains("\"groupId\":\"org.virtuslab.scala-cli\""))
      expect(jsonContents.contains(s"\"version\":\"${Constants.runnerScala2LegacyVersion}\""))
    }
  }

  test("export json includes legacy JVM test-runner for Scala 2.13") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """object Main {
          |  def main(args: Array[String]): Unit = println("hi")
          |}
          |""".stripMargin,
      os.rel / "unit.test.scala" ->
        """class MyTest { def foo() = () }
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      val exportJsonProc =
        os.proc(
          TestUtil.cli,
          "--power",
          "export",
          "--json",
          ".",
          "--jvm",
          "temurin:17",
          "--scala",
          Constants.scala213
        )
          .call(cwd = root)
      val jsonContents = readJson(exportJsonProc.out.text())
      expect(jsonContents.contains("\"fullName\":\"test-runner_2.13\""))
      expect(jsonContents.contains("\"groupId\":\"org.virtuslab.scala-cli\""))
      expect(jsonContents.contains(s"\"version\":\"${Constants.runnerScala2LegacyVersion}\""))
    }
  }

  test("export json does not inject JVM test-runner for Native target") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """object Main {
          |  def main(args: Array[String]): Unit = println("hi")
          |}
          |""".stripMargin,
      os.rel / "unit.test.scala" ->
        """class MyTest { def foo() = () }
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      val exportJsonProc =
        os.proc(TestUtil.cli, "--power", "export", "--json", ".", "--native")
          .call(cwd = root)
      val jsonContents = readJson(exportJsonProc.out.text())
      // The JVM test-runner is JVM-only; Native targets get a Scala Native
      // test-interface dep instead (verified by a separate test below).
      expect(!jsonContents.contains("\"name\":\"test-runner\""))
      expect(!jsonContents.contains("org.virtuslab.scala-cli"))
    }
  }

  test("export json injects Scala Native test-interface into test scope of Native target") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """object Main {
          |  def main(args: Array[String]): Unit = println("hi")
          |}
          |""".stripMargin,
      os.rel / "unit.test.scala" ->
        """class MyTest { def foo() = () }
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      val exportJsonProc =
        os.proc(TestUtil.cli, "--power", "export", "--json", ".", "--native")
          .call(cwd = root)
      val jsonContents     = readJson(exportJsonProc.out.text())
      val snBinary         = Constants.scalaNativeVersion.split('.').take(2).mkString(".")
      val expectedFullName =
        s"test-interface_native${snBinary}_${Constants.scala3NextPrefix.split('.').head}"
      // The test scope's injectedDependencies should include the Scala Native test-interface
      // module pinned at scala-cli's bundled Scala Native version.
      expect(jsonContents.contains("\"name\":\"test-interface_native" + snBinary + "\""))
      expect(jsonContents.contains(s"\"fullName\":\"$expectedFullName\""))
      expect(jsonContents.contains(s"\"version\":\"${Constants.scalaNativeVersion}\""))
    }
  }

  test("export json with js") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """//> using scala 3.1.3
          |//> using platform scala-js
          |//> using lib com.lihaoyi::os-lib:0.7.8
          |//> using option -Xasync
          |//> using plugin org.wartremover:::wartremover:3.0.9
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

      val fileContents         = readJson(root / "json_dir" / "export.json")
      val expectedFileContents =
        s"""{
           |"scalaVersion": "3.1.3",
           |"platform": "JS",
           |"scalaJsVersion": "${Constants.scalaJsVersion}",
           |"jsEsVersion":"es2015",
           |"jsOptions": {
           | "scalaJsVersion": "${Constants.scalaJsVersion}",
           | "scalaJsCliVersion": "${Constants.scalaJsCliVersion}",
           | "toolingDependencies": [
           |   {
           |     "groupId": "org.virtuslab.scala-cli",
           |     "artifactId": {
           |       "name": "scalajscli_2.13",
           |       "fullName": "scalajscli_2.13"
           |     },
           |     "version": "${Constants.scalaJsCliVersion}"
           |   }
           | ]
           |},
           |"scopes": {
           | "main": {
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
           |   "injectedDependencies": [
           |     {
           |       "groupId":"org.scala-js",
           |       "artifactId":{"name":"scalajs-library","fullName":"scalajs-library_2.13"},
           |       "version":"${Constants.scalaJsVersion}"
           |     }
           |   ],
           |   "resolvers": [
           |     "https://repo1.maven.org/maven2",
           |     "ivy:file:.../local-repo/...",
           |     "ivy:file:.../.ivy2/local/"
           |   ]
           | }
           |},
           |"scalaCliVersion":"1.1.1-SNAPSHOT"
           |}
           |""".replaceAll("\\s|\\|", "")
      expect(fileContents == expectedFileContents)

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
      val jsonDirPath = root / "json_dir"
      expect(exportToExistingProc.out.text().contains(s"Error: $jsonDirPath already exists."))
    }
  }

}
