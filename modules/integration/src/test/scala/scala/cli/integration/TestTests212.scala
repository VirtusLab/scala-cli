package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.StringOps
import scala.util.Properties

trait TestTests212 { this: TestTestDefinitions & Test212 & TestBuildServer =>
  if !usesBloop then
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

class TestTests212WithBloop
    extends TestTestDefinitions with Test212 with TestWithBloop with TestTests212

class TestTests212WithoutBloop
    extends TestTestDefinitions with Test212 with TestWithoutBloop with TestTests212 {
  override def munitIgnore: Boolean = super.munitIgnore || Properties.isWin
}
