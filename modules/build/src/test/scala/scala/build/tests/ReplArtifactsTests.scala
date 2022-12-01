package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect
import coursier.cache.FileCache
import dependency.ScalaParameters

import scala.build.{Directories, Logger, ReplArtifacts}

class ReplArtifactsTests extends munit.FunSuite {

  def scalaPyTest(version: String, usesFormerOrg: Boolean = false): Unit =
    TestInputs.withTmpDir("replartifactstests") { root =>
      val artifacts = ReplArtifacts.ammonite(
        ScalaParameters("2.13.8"),
        "2.5.4",
        Nil,
        Nil,
        Nil,
        Nil,
        Logger.nop,
        FileCache(),
        Directories.under(root),
        Some(version)
      ).fold(e => throw new Exception(e), identity)

      val urls           = artifacts.replArtifacts.map(_._1)
      val meShadajUrls   = urls.filter(_.startsWith("https://repo1.maven.org/maven2/me/shadaj/"))
      val devScalaPyUrls = urls.filter(_.startsWith("https://repo1.maven.org/maven2/dev/scalapy/"))

      if (usesFormerOrg) {
        expect(meShadajUrls.nonEmpty)
        expect(devScalaPyUrls.isEmpty)
      }
      else {
        expect(meShadajUrls.isEmpty)
        expect(devScalaPyUrls.nonEmpty)
      }
    }

  test("ScalaPy former organization") {
    scalaPyTest("0.5.2+5-83f1eb68", usesFormerOrg = true)
  }
  test("ScalaPy new organization") {
    scalaPyTest("0.5.2+9-623f0807")
  }

}
