package cli.tests
import com.eed3si9n.expecty.Expecty.expect
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

import scala.build.Ops._
import scala.build.internal.Constants
import scala.build.tests.{TestInputs, TestLogger}
import scala.cli.commands.fmt.{FmtOptions, FmtUtil}
import scala.cli.commands.update.Update.Release

class ScalafmtTests extends munit.FunSuite {
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
    final case class Asset(name: String)
    final case class Release(tag_name: String, assets: List[Asset])
    lazy val releaseCodec: JsonValueCodec[Release] = JsonCodecMaker.make
    val url =
      s"https://api.github.com/repos/scalameta/scalafmt/releases/tags/v$defaultScalafmtVersion"

    val expectedAssets = Seq(
      "scalafmt-x86_64-apple-darwin.zip",
      "scalafmt-x86_64-pc-linux.zip",
      "scalafmt-x86_64-pc-win32.zip",
      "scalafmt-aarch64-apple-darwin.zip",
      "scalafmt-aarch64-pc-linux.zip"
    )
    val errorMsg =
      s"""scalafmt native images missing for v$defaultScalafmtVersion at https://github.com/scalameta/scalafmt
         |Ensure that all expected assets are available in the release:
         |  ${expectedAssets.mkString(", ")}
         |under tag v$defaultScalafmtVersion.""".stripMargin
    try {
      val resp    = TestUtil.downloadFile(url).orThrow
      val release = readFromArray(resp)(releaseCodec)
      val assets  = release.assets.map(_.name)

      assert(
        expectedAssets.forall(assets.contains),
        clue = errorMsg
      )
    }
    catch {
      case e: JsonReaderException => throw new Exception(s"Error reading $url", e)
      case e: Throwable => throw new Exception(
          s"""Failed to check for the ScalaFmt $defaultScalafmtVersion native launcher assets: ${e.getMessage}
             |
             |$errorMsg
             |""".stripMargin,
          e
        )
    }
  }
}
