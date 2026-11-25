package cli.tests
import com.eed3si9n.expecty.Expecty.expect

import scala.build.internal.Constants
import scala.build.tests.{TestInputs, TestLogger}
import scala.cli.commands.fmt.{FmtOptions, FmtUtil}

class ScalafmtTests extends TestUtil.ScalaCliSuite {
  private lazy val defaultScalafmtVersion = Constants.defaultScalafmtVersion

  test("readVersionFromFile with non-default scalafmt version") {
    val confFile = """runner.dialect = scala213
                     |version = "3.1.2"
                     |""".stripMargin

    TestInputs.withTmpDir("temp-dir") { dirPath =>
      val confFilePath = dirPath / ".scalafmt.conf"
      os.write(confFilePath, confFile)

      val readVersionAndDialect =
        FmtUtil.readVersionAndDialect(workspace = dirPath, FmtOptions(), TestLogger())
      expect(readVersionAndDialect == (Some("3.1.2"), Some("scala213"), Some(confFilePath)))
    }
  }

  test("readVersionFromFile with missing .scalafmt.conf file") {
    TestInputs.withTmpDir("temp-dir") { dirPath =>
      val readVersionAndDialect =
        FmtUtil.readVersionAndDialect(workspace = dirPath, FmtOptions(), TestLogger())
      expect(readVersionAndDialect == (None, None, None))
    }
  }

  test(s"check native launcher availability for scalafmt $defaultScalafmtVersion") {
    // checks the download URLs Scala CLI uses, rather than the release from the GitHub API, as
    // the latter is rate-limited (60 requests per hour per IP address without authentication),
    // which made this test fail often on the CI
    val platforms = Seq(
      "x86_64-apple-darwin",
      "x86_64-pc-linux",
      "x86_64-pc-win32",
      "aarch64-apple-darwin",
      "aarch64-pc-linux"
    )
    val urls = platforms.map { platform =>
      FmtOptions(osArchSuffix = Some(platform)).binaryUrl(defaultScalafmtVersion)._1
    }
    val missing = urls.filterNot(TestUtil.remoteFileExists(_))
    assert(
      missing.isEmpty,
      clue =
        s"""scalafmt native images missing for v$defaultScalafmtVersion:
           |  ${missing.mkString(System.lineSeparator() + "  ")}
           |Ensure that all the native launchers are available in the scalafmt release.""".stripMargin
    )
  }
}
