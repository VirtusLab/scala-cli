package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class PublishTestsDefault extends PublishTestDefinitions(scalaVersionOpt = None) {

  test("Pure Java") {
    val testOrg     = "test-org.foo"
    val testName    = "foo"
    val testVersion = "0.3.1"
    val inputs = TestInputs(
      Seq(
        os.rel / "Foo.java" ->
          s"""//> using publish.organization "$testOrg"
             |//> using publish.name "$testName"
             |//> using publish.version "$testVersion"
             |
             |package foo;
             |
             |public class Foo {
             |  private static boolean checkClass(String clsName) {
             |    try {
             |      Thread.currentThread().getContextClassLoader().loadClass(clsName);
             |      return true;
             |    } catch (ClassNotFoundException ex) {
             |      return false;
             |    }
             |  }
             |
             |  public static void main(String[] args) {
             |    boolean hasJuList = checkClass("java.util.List");
             |    boolean hasScalaArray = checkClass("scala.Array");
             |    if (!hasJuList) {
             |      System.out.println("Error: java.util.List not found");
             |      System.exit(1);
             |    }
             |    if (hasScalaArray) {
             |      System.out.println("Error: unexpectedly found scala.Array");
             |      System.exit(1);
             |    }
             |    System.out.println("Hello from " + "foo");
             |  }
             |}
             |""".stripMargin
      )
    )

    val repoRelPath = os.rel / "test-repo"
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "publish", extraOptions, ".", "-R", repoRelPath)
        .call(stdin = os.Inherit, stdout = os.Inherit, cwd = root)
      val repoRoot = root / repoRelPath
      val baseDir  = repoRoot / testOrg.split('.').toSeq / testName / testVersion
      expect(os.isDir(baseDir))

      val res = os.proc(
        TestUtil.cs,
        "launch",
        s"$testOrg:$testName:$testVersion",
        "-r",
        repoRoot.toNIO.toUri.toASCIIString
      )
        .call(stdin = os.Inherit, cwd = root)
      val output = res.out.text().trim
      expect(output == "Hello from foo")
    }
  }
}
