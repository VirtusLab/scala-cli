package cli.tests
import com.eed3si9n.expecty.Expecty.expect

import scala.build.tests.{TestInputs, TestLogger}
import scala.cli.commands.Fmt

class ScalafmtTests extends munit.FunSuite {

  test("readVersionFromFile with non-default scalafmt version") {
    val confFile = """runner.dialect = scala213
                     |version = "3.1.2"
                     |""".stripMargin

    TestInputs.withTmpDir("temp-dir") { dirPath =>
      val confFilePath = dirPath / ".scalafmt.conf"
      os.write(confFilePath, confFile)

      val readVersionMaybe = Fmt.readVersionFromFile(workspace = dirPath, TestLogger())
      expect(readVersionMaybe == (Some("3.1.2"), true))
    }
  }

  test("readVersionFromFile with missing .scalafmt.conf file") {
    TestInputs.withTmpDir("temp-dir") { dirPath =>
      val readVersionMaybe = Fmt.readVersionFromFile(workspace = dirPath, TestLogger())
      expect(readVersionMaybe == (None, false))
    }
  }
}
