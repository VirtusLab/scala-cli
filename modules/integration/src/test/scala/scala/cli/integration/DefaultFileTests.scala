package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class DefaultFileTests extends ScalaCliSuite {

  override def group = ScalaCliSuite.TestGroup.First

  test("Print .gitignore") {
    val res = os.proc(TestUtil.cli, "default-file", ".gitignore")
      .call()
    val output = res.out.text()
    expect(output.linesIterator.toVector.contains("/.scala-build/"))
  }

  test("Write .gitignore") {
    TestInputs.empty.fromRoot { root =>
      os.proc(TestUtil.cli, "default-file", ".gitignore", "--write")
        .call(cwd = root, stdout = os.Inherit)
      val dest = root / ".gitignore"
      expect(os.isFile(dest))
      val content = os.read(dest)
      expect(content.linesIterator.toVector.contains("/.scala-build/"))
    }
  }

}
