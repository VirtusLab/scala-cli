package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class ConfigTests extends munit.FunSuite {

  test("simple") {
    val homeDir    = os.rel / "home"
    val dirOptions = Seq[os.Shellable]("--home-directory", homeDir)
    val name       = "Alex"
    TestInputs(Nil).fromRoot { root =>
      val before = os.proc(TestUtil.cli, "config", dirOptions, "user.name").call(cwd = root)
      expect(before.out.text().trim.isEmpty)

      os.proc(TestUtil.cli, "config", dirOptions, "user.name", name).call(cwd = root)
      val res = os.proc(TestUtil.cli, "config", dirOptions, "user.name").call(cwd = root)
      expect(res.out.text().trim == name)

      os.proc(TestUtil.cli, "config", dirOptions, "user.name", "--unset").call(cwd = root)
      val after = os.proc(TestUtil.cli, "config", dirOptions, "user.name").call(cwd = root)
      expect(after.out.text().trim.isEmpty)
    }
  }

  test("password") {
    val homeDir    = os.rel / "home"
    val dirOptions = Seq[os.Shellable]("--home-directory", homeDir)
    val password   = "1234"
    TestInputs(Nil).fromRoot { root =>

      def emptyCheck(): Unit = {
        val value = os.proc(TestUtil.cli, "config", dirOptions, "sonatype.password")
          .call(cwd = root)
        expect(value.out.text().trim.isEmpty)
      }

      def unset(): Unit =
        os.proc(TestUtil.cli, "config", dirOptions, "sonatype.password", "--unset")
          .call(cwd = root)

      def read(): String = {
        val res = os.proc(TestUtil.cli, "config", dirOptions, "sonatype.password")
          .call(cwd = root)
        res.out.text().trim
      }
      def readDecoded(): String = {
        val res = os.proc(TestUtil.cli, "config", dirOptions, "sonatype.password", "--password")
          .call(cwd = root)
        res.out.text().trim
      }

      emptyCheck()

      os.proc(TestUtil.cli, "config", dirOptions, "sonatype.password", s"value:$password")
        .call(cwd = root)
      expect(read() == s"value:$password")
      expect(readDecoded() == password)
      unset()
      emptyCheck()

    }
  }

}
