package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class PublishTestsDefault extends PublishTestDefinitions(scalaVersionOpt = None) {

  test("publish local") {
    val testOrg     = "test-local-org.sth"
    val testName    = "my-proj"
    val testVersion = "1.5.6"
    val inputs = TestInputs(
      os.rel / "Project.scala" ->
        s"""//> using publish.organization "$testOrg"
           |//> using publish.name "$testName"
           |//> using publish.version "$testVersion"
           |
           |//> using scala "2.13"
           |//> using lib "com.lihaoyi::os-lib:0.8.1"
           |
           |object Project {
           |  def message = "Hello"
           |
           |  def main(args: Array[String]): Unit =
           |    println(message)
           |}
           |""".stripMargin
    )

    val expectedFiles = {
      val modName = s"${testName}_2.13"
      val base    = os.rel / testOrg / modName / testVersion
      val baseFiles = Seq(
        base / "jars" / s"$modName.jar",
        base / "docs" / s"$modName-javadoc.jar",
        base / "srcs" / s"$modName-sources.jar",
        base / "poms" / s"$modName.pom",
        base / "ivys" / "ivy.xml"
      )
      baseFiles
        .flatMap { f =>
          val md5  = f / os.up / s"${f.last}.md5"
          val sha1 = f / os.up / s"${f.last}.sha1"
          Seq(f, md5, sha1)
        }
        .toSet
    }

    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "publish", "local", "Project.scala", "--ivy2-home", os.rel / "ivy2")
        .call(cwd = root)
      val ivy2Local = root / "ivy2" / "local"
      val foundFiles = os.walk(ivy2Local)
        .filter(os.isFile(_))
        .map(_.relativeTo(ivy2Local))
        .toSet
      val missingFiles    = expectedFiles -- foundFiles
      val unexpectedFiles = foundFiles -- expectedFiles
      if (missingFiles.nonEmpty)
        pprint.err.log(missingFiles)
      if (unexpectedFiles.nonEmpty)
        pprint.err.log(unexpectedFiles)
      expect(missingFiles.isEmpty)
      expect(unexpectedFiles.isEmpty)
    }
  }

  test("Pure Java") {
    val testOrg     = "test-org.foo"
    val testName    = "foo"
    val testVersion = "0.3.1"
    val inputs = TestInputs(
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
      val output = res.out.trim()
      expect(output == "Hello from foo")
    }
  }
}
