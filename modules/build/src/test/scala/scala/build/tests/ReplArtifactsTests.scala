package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect
import coursier.cache.FileCache
import dependency.ScalaParameters

import scala.build.{Logger, ReplArtifacts}

class ReplArtifactsTests extends TestUtil.ScalaCliBuildSuite {

  def scalaPyTest(version: String, usesFormerOrg: Boolean = false): Unit =
    TestInputs.withTmpDir("replartifactstests") { _ =>
      val artifacts = ReplArtifacts.ammonite(
        scalaParams = ScalaParameters("2.13.8"),
        ammoniteVersion = "2.5.4",
        dependencies = Nil,
        extraClassPath = Nil,
        extraSourceJars = Nil,
        extraRepositories = Nil,
        logger = Logger.nop,
        cache = FileCache(),
        addScalapy = Some(version)
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
