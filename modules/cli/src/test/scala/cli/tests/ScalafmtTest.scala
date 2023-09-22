package cli.tests
import com.eed3si9n.expecty.Expecty.expect
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import scala.build.Ops.*
import scala.build.internal.Constants
import scala.build.tests.{TestInputs, TestLogger}
import scala.cli.commands.fmt.{FmtOptions, FmtUtil}
import scala.cli.commands.update.Update.Release

class ScalafmtTests extends munit.FunSuite {

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

  test("check native launcher availability for scalafmt") {
    final case class Asset(name: String)
    final case class Release(tag_name: String, assets: List[Asset])
    lazy val releaseCodec: JsonValueCodec[Release] = JsonCodecMaker.make

    val scalaFmtVersion = Constants.defaultScalafmtVersion
    val url =
      s"https://api.github.com/repos/virtuslab/scalafmt-native-image/releases/tags/v$scalaFmtVersion"

    try {
      val resp    = TestUtil.downloadFile(url).orThrow
      val release = readFromArray(resp)(releaseCodec)
      val assets  = release.assets.map(_.name)

      val expectedAssets = Seq(
        "scalafmt-x86_64-apple-darwin.gz",
        "scalafmt-x86_64-pc-linux-mostly-static.gz",
        "scalafmt-x86_64-pc-linux-static.gz",
        "scalafmt-x86_64-pc-linux.gz",
        "scalafmt-x86_64-pc-win32.zip"
      )
      // if this test fails, you should do a release of https://github.com/VirtusLab/scalafmt-native-image with the same version as scalafmt
      expect(assets.containsSlice(expectedAssets))
    }
    catch {
      case e: JsonReaderException => throw new Exception(s"Error reading $url", e)
      case e: Throwable => throw new Exception(
          s"Failed to check for the ScalaFmt native launcher assets: ${e.getMessage}",
          e
        )
    }
  }
}
