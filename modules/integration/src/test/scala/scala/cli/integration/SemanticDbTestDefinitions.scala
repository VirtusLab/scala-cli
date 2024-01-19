package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

trait SemanticDbTestDefinitions { _: CompileTestDefinitions =>
  def javaHelloWorld(packageName: String, mainClassName: String): String =
    s"""package $packageName;
       |
       |public class $mainClassName {
       |  public static void main(String[] args) {
       |    System.err.println("Hello");
       |  }
       |}
       |""".stripMargin

  def scalaHelloWorld(packageName: String, mainClassName: String): String =
    s"""package $packageName
       |
       |object $mainClassName {
       |  def main(args: Array[String]): Unit = {
       |    println("Hello")
       |  }
       |}
       |""".stripMargin

  test("Java SemanticDB (manual)") {
    TestInputs(os.rel / "foo" / "Test.java" -> javaHelloWorld("foo", "Test"))
      .fromRoot { root =>
        val compilerPackages = Seq(
          "com.sun.tools.javac.api",
          "com.sun.tools.javac.code",
          "com.sun.tools.javac.model",
          "com.sun.tools.javac.tree",
          "com.sun.tools.javac.util"
        )
        val exports = compilerPackages
          .flatMap { pkg =>
            Seq("-J--add-exports", s"-Jjdk.compiler/$pkg=ALL-UNNAMED")
          }
          .flatMap(opt => List("--javac-opt", opt))
        val javaSemDbOptions = Seq(
          "--javac-plugin",
          "com.sourcegraph:semanticdb-javac:0.7.4",
          "--javac-opt",
          s"-Xplugin:semanticdb -sourceroot:$root -targetroot:javac-classes-directory"
        ) ++ exports
        os.proc(TestUtil.cli, "compile", extraOptions, javaSemDbOptions, ".")
          .call(cwd = root)

        val files = os.walk(root / Constants.workspaceDirName)
        val semDbFiles = files
          .filter(_.last.endsWith(".semanticdb"))
          .filter(!_.segments.exists(_ == "bloop-internal-classes"))
        expect(semDbFiles.length == 1)
        val semDbFile = semDbFiles.head
        expect(
          semDbFile.endsWith(os.rel / "META-INF" / "semanticdb" / "foo" / "Test.java.semanticdb")
        )
      }
  }

  for {
    language            <- Seq("Java", "Scala")
    semanticDbTargetDir <- Seq(None, Some("semanticdb-target"))
  } {
    test(
      s"$language SemanticDB${semanticDbTargetDir.map(_ => " with forced target root").getOrElse("")}"
    ) {
      val sourceFileName = if (language == "Java") "Test.java" else "Test.scala"
      (if (language == "Java")
         TestInputs(os.rel / "foo" / sourceFileName -> javaHelloWorld("foo", "Test"))
       else
         TestInputs(os.rel / "foo" / sourceFileName -> scalaHelloWorld("foo", "Test"))).fromRoot {
        root =>
          val targetDirOptions =
            semanticDbTargetDir match {
              case Some(targetDir) => Seq("--semantic-db-target-root", targetDir)
              case None            => Nil
            }
          os.proc(TestUtil.cli, "compile", extraOptions, "--semantic-db", ".", targetDirOptions)
            .call(cwd = root)
          val files = os.walk(root)
          val semDbFiles = files
            .filter(_.last.endsWith(".semanticdb"))
            .filter(!_.segments.exists(_ == "bloop-internal-classes"))
          expect(semDbFiles.length == 1)
          val semDbFile = semDbFiles.head
          val expectedSemanticDbPath =
            if (semanticDbTargetDir.isDefined)
              os.rel / semanticDbTargetDir.get / "META-INF" / "semanticdb" / "foo" / s"$sourceFileName.semanticdb"
            else
              os.rel / "META-INF" / "semanticdb" / "foo" / s"$sourceFileName.semanticdb"
          expect(semDbFile.endsWith(expectedSemanticDbPath))
      }
    }

    test(
      s"$language SemanticDB with spread source dirs, forced source root ${semanticDbTargetDir.map(_ => "and target root").getOrElse("")}"
    ) {
      val (className1, className2) = s"Test1$language" -> s"Test2$language"
      val (sourceFileName1, sourceFileName2) =
        if (language == "Java") s"$className1.java" -> s"$className2.java"
        else s"$className1.scala"                   -> s"$className2.scala"
      val (package1, package2)     = "foo"                 -> "bar"
      val (sourceDir1, sourceDir2) = (os.rel / "sources1") -> (os.rel / "sources2")
      val (code1, code2) =
        if (language == "Java")
          javaHelloWorld(package1, className1)     -> javaHelloWorld(package2, className2)
        else scalaHelloWorld(package1, className1) -> scalaHelloWorld(package2, className2)
      TestInputs(
        os.rel / sourceDir1 / package1 / sourceFileName1 -> code1,
        os.rel / sourceDir2 / package2 / sourceFileName2 -> code2
      ).fromRoot { (root: os.Path) =>
        val targetDirOptions =
          semanticDbTargetDir match {
            case Some(targetDir) => Seq("--semanticdb-targetroot", targetDir)
            case None            => Nil
          }
        val semanticDbOptions: Seq[String] =
          targetDirOptions ++ Seq("--semanticdb", "--semanticdb-sourceroot", root.toString)
        os.proc(
          TestUtil.cli,
          "compile",
          extraOptions,
          semanticDbOptions,
          sourceDir1,
          sourceDir2
        )
          .call(cwd = root)
        val files = os.walk(semanticDbTargetDir.map(root / _)
          .getOrElse(root / sourceDir1 / Constants.workspaceDirName))
        val semDbFiles = files
          .filter(_.last.endsWith(".semanticdb"))
          .filter(!_.segments.exists(_ == "bloop-internal-classes"))
        expect(semDbFiles.length == 2)
        val semDbFile1 = semDbFiles.find(_.last == s"$sourceFileName1.semanticdb").get
        expect(
          semDbFile1.endsWith(
            os.rel / "META-INF" / "semanticdb" / sourceDir1 / package1 / s"$sourceFileName1.semanticdb"
          )
        )
        val semDbFile2 = semDbFiles.find(_.last == s"$sourceFileName2.semanticdb").get
        expect(
          semDbFile2.endsWith(
            os.rel / "META-INF" / "semanticdb" / sourceDir2 / package2 / s"$sourceFileName2.semanticdb"
          )
        )
      }
    }
  }
}
