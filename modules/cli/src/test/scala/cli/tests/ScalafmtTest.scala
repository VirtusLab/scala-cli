package cli.tests
import com.eed3si9n.expecty.Expecty.expect

import scala.build.tests.{TestInputs, TestLogger}
import scala.cli.commands.util.FmtUtil

class ScalafmtTests extends munit.FunSuite {

  test("readVersionFromFile with non-default scalafmt version") {
    val confFile = """runner.dialect = scala213
                     |version = "3.1.2"
                     |""".stripMargin

    TestInputs.withTmpDir("temp-dir") { dirPath =>
      val confFilePath = dirPath / ".scalafmt.conf"
      os.write(confFilePath, confFile)

      val readVersionAndDialect =
        FmtUtil.readVersionAndDialectFromFile(workspace = dirPath, TestLogger())
      expect(readVersionAndDialect == (Some("3.1.2"), Some("scala213"), Some(confFilePath)))
    }
  }

  test("readVersionFromFile with missing .scalafmt.conf file") {
    TestInputs.withTmpDir("temp-dir") { dirPath =>
      val readVersionAndDialect =
        FmtUtil.readVersionAndDialectFromFile(workspace = dirPath, TestLogger())
      expect(readVersionAndDialect == (None, None, None))
    }
  }
}
