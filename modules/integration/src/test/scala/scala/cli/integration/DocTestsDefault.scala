package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class DocTestsDefault extends DocTestDefinitions(scalaVersionOpt = None) {

  test("javadoc") {
    val inputs = TestInputs(
      os.rel / "Foo.java" ->
        """//> using dep "org.graalvm.nativeimage:svm:22.0.0.2"
          |
          |import com.oracle.svm.core.annotate.TargetClass;
          |import org.graalvm.nativeimage.Platform;
          |import org.graalvm.nativeimage.Platforms;
          |
          |/**
          | * Foo class
          | */
          |@TargetClass(className = "something")
          |@Platforms({Platform.LINUX.class, Platform.DARWIN.class})
          |public class Foo {
          |  /**
          |   * Gets the value
          |   *
          |   * @return the value
          |   */
          |  public int getValue() {
          |    return 2;
          |  }
          |}
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      val dest = root / "doc"
      os.proc(TestUtil.cli, "doc", extraOptions, ".", "-o", dest).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      expect(os.isDir(dest))
      val expectedEntries = Seq(
        "index.html",
        "overview-tree.html",
        "Foo.html"
      )
      val entries = os.walk(dest).map(_.relativeTo(dest)).map(_.toString).toSet
      expect(expectedEntries.forall(entries))
    }
  }
}
