package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.StringOps

class TestTests212 extends TestTestDefinitions with Test212 {
  test(s"run a simple test with Scala $actualScalaVersion (legacy)") {
    val expectedMessage = "Hello, world!"
    TestInputs(os.rel / "example.test.scala" ->
      s"""//> using dep com.novocode:junit-interface:0.11
         |import org.junit.Test
         |
         |class MyTests {
         |  @Test
         |  def foo(): Unit = {
         |    assert(2 + 2 == 4)
         |    println("$expectedMessage")
         |  }
         |}
         |""".stripMargin).fromRoot { root =>
      val expectedWarning =
        s"Defaulting to a legacy test-runner module version: ${Constants.runnerScala2LegacyVersion}"
      val res =
        os.proc(TestUtil.cli, "test", ".", extraOptions)
          .call(cwd = root, stderr = os.Pipe)
      val out = res.out.trim()
      expect(out.contains(expectedMessage))
      val err = res.err.trim()
      expect(err.contains(expectedWarning))
      expect(err.countOccurrences(expectedWarning) == 1)
    }
  }
}
