package scala.build.internal

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import coursier.cache.FileCache
import coursier.util.{Artifact, Task}
import dependency.ScalaVersion

import scala.build.EitherCps.{either, value}
import scala.build.Os
import scala.build.errors.{BuildException, ScalaVersionError}
import scala.concurrent.duration._
import scala.util.control.NonFatal

object ScalaNightlyVersion {

  private object Scala2Repo {
    final case class ScalaVersion(name: String, lastModified: Long)
    final case class ScalaVersionsMetaData(repo: String, children: List[ScalaVersion])

    val codec: JsonValueCodec[ScalaVersionsMetaData] = JsonCodecMaker.make
  }

  private def downloadScala2RepoPage(cache: FileCache[Task]): Either[BuildException, Array[Byte]] =
    either {
      val scala2NightlyRepo =
        "https://scala-ci.typesafe.com/ui/api/v1/ui/nativeBrowser/scala-integration/org/scala-lang/scala-compiler"
      val artifact = Artifact(scala2NightlyRepo).withChanging(true)
      val res = cache.logger.use {
        try cache.withTtl(1.hours).file(artifact).run.unsafeRun()(cache.ec)
        catch {
          case NonFatal(e) => throw new Exception(e)
        }
      }.left.map { err =>
        val msg =
          """|Unable to compute the latest Scala 2 nightly version.
             |Throws error during downloading web page repository for Scala 2.""".stripMargin
        new ScalaVersionError(msg, cause = err)
      }

      val res0    = value(res)
      val content = os.read.bytes(os.Path(res0, Os.pwd))
      content
    }

  def computeLatestScalaNightlyVersions(
    versionPrefix: String,
    cache: FileCache[Task]
  ): Either[BuildException, (String, String)] = either {
    val webPageScala2Repo = value(downloadScala2RepoPage(cache))
    val scala2Repo        = readFromArray(webPageScala2Repo)(Scala2Repo.codec)
    val versions          = scala2Repo.children
    val sortedVersion =
      versions
        .filter(_.name.startsWith(versionPrefix))
        .filterNot(_.name.contains("pre"))
        .sortBy(_.lastModified)

    val latestNightly = sortedVersion.lastOption.map(_.name)
    latestNightly match {
      case Some(nightlyVersion) =>
        val scalaBinaryVersion = ScalaVersion.binary(nightlyVersion)
        (nightlyVersion, scalaBinaryVersion)
      case None =>
        val msg = s"""|Unable to compute the latest Scala $versionPrefix nightly version.
                      |Pass explicitly full Scala 2 nightly version.""".stripMargin
        throw new ScalaVersionError(msg)
    }

  }

}
