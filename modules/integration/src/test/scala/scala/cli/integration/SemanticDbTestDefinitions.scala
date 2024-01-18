package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

trait SemanticDbTestDefinitions { _: CompileTestDefinitions =>
  test("Java SemanticDB (manual)") {
    val inputs = TestInputs(
      os.rel / "foo" / "Test.java" ->
        """package foo;
          |
          |public class Test {
          |  public static void main(String[] args) {
          |    System.err.println("Hello");
          |  }
          |}
          |""".stripMargin
    )
    inputs.fromRoot { root =>
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
    language <- Seq("Java", "Scala")
    sourceFileName = if (language == "Java") "Test.java" else "Test.scala"
    inputs =
      if (language == "Java")
        TestInputs(
          os.rel / "foo" / sourceFileName ->
            """package foo;
              |
              |public class Test {
              |  public static void main(String[] args) {
              |    System.err.println("Hello");
              |  }
              |}
              |""".stripMargin
        )
      else
        TestInputs(
          os.rel / "foo" / sourceFileName ->
            """package foo;
              |
              |object Test {
              |  def main(args: Array[String]): Unit = {
              |    println("Hello")
              |  }
              |}
              |""".stripMargin
        )
    semanticDbTargetDir <- Seq(None, Some("semanticdb-target"))
    targetDirString = semanticDbTargetDir.map(_ => "with forced target root").getOrElse("")
  }
    test(s"$language SemanticDB $targetDirString") {
      inputs.fromRoot { root =>
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
}
