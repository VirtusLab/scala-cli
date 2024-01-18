package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

trait SemanticDbTestDefinitions { _: CompileTestDefinitions =>
  test("Manual javac SemanticDB") {
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

  test("Javac SemanticDB") {
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
      os.proc(TestUtil.cli, "compile", extraOptions, "--semantic-db", ".")
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
}
