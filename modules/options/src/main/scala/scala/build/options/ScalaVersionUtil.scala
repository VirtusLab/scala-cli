package scala.build.options

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import coursier.Versions
import coursier.cache.FileCache
import coursier.core.{Version, Versions => CoreVersions}
import coursier.util.{Artifact, Task}

import scala.build.CoursierUtils._
import scala.build.EitherCps.{either, value}
import scala.build.errors.{
  BuildException,
  InvalidBinaryScalaVersionError,
  NoValidScalaVersionFoundError,
  ScalaVersionError,
  UnsupportedScalaVersionError
}
import scala.build.internal.Regexes.scala2NightlyRegex
import scala.build.internal.Util
import scala.concurrent.duration.DurationInt
import scala.util.Try
import scala.util.control.NonFatal

object ScalaVersionUtil {

  private def scala2Library = cmod"org.scala-lang:scala-library"
  private def scala3Library = cmod"org.scala-lang:scala3-library_3"

  object GetNightly {

    private object Scala2Repo {
      final case class ScalaVersion(name: String, lastModified: Long)
      final case class ScalaVersionsMetaData(repo: String, children: List[ScalaVersion])

      val codec: JsonValueCodec[ScalaVersionsMetaData] = JsonCodecMaker.make
    }

    private def downloadScala2RepoPage(cache: FileCache[Task])
      : Either[BuildException, Array[Byte]] =
      either {
        val scala2NightlyRepo =
          "https://scala-ci.typesafe.com/ui/api/v1/ui/nativeBrowser/scala-integration/org/scala-lang/scala-compiler"
        val artifact = Artifact(scala2NightlyRepo).withChanging(true)
        val res = cache.logger.use {
          try cache.withTtl(0.seconds).file(artifact).run.unsafeRun()(cache.ec)
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
        val content = os.read.bytes(os.Path(res0, os.pwd))
        content
      }

    def scala2(
      versionPrefix: String,
      cache: FileCache[Task]
    ): Either[BuildException, String] = either {
      val webPageScala2Repo = value(downloadScala2RepoPage(cache))
      val scala2Repo        = readFromArray(webPageScala2Repo)(Scala2Repo.codec)
      val versions          = scala2Repo.children
      val sortedVersion =
        versions
          .filter(_.name.startsWith(versionPrefix))
          .filterNot(_.name.contains("pre"))
          .sortBy(_.lastModified)

      val latestNightly = sortedVersion.lastOption.map(_.name)
      latestNightly.getOrElse {
        val msg = s"""|Unable to compute the latest Scala $versionPrefix nightly version.
                      |Pass explicitly full Scala 2 nightly version.""".stripMargin
        throw new ScalaVersionError(msg)
      }
    }

    /** @return
      *   Either a BuildException or the calculated (ScalaVersion, ScalaBinaryVersion) tuple
      */
    def scala3X(
      threeSubBinaryNum: String,
      cache: FileCache[Task],
      latestSupportedStableVersions: Seq[String]
    ): Either[BuildException, String] = {
      val res = cache.withTtl(0.seconds).logger.use {
        Versions(cache)
          .withModule(scala3Library)
          .result()
          .unsafeRun()(cache.ec)
      }.versions.available.filter(_.endsWith("-NIGHTLY"))

      val threeXNightlies = res.filter(_.startsWith(s"3.$threeSubBinaryNum.")).map(Version(_))
      if (threeXNightlies.nonEmpty) Right(threeXNightlies.max.repr)
      else Left(
        new NoValidScalaVersionFoundError(res, latestSupportedStableVersions)
      )
    }

    /** @return
      *   Either a BuildException or the calculated (ScalaVersion, ScalaBinaryVersion) tuple
      */
    def scala3(cache: FileCache[Task]): Either[BuildException, String] = {
      val res = cache.withTtl(0.seconds).logger.use {
        Versions(cache)
          .withModule(scala3Library)
          .result()
          .unsafeRun()(cache.ec)
      }
      latestScalaVersionFrom(res.versions, "latest Scala 3 nightly build")
    }

    private def latestScalaVersionFrom(
      versions: CoreVersions,
      desc: String
    ): Either[scala.build.errors.ScalaVersionError, String] =
      versions.latest(coursier.core.Latest.Release) match {
        case Some(versionString) => Right(versionString)
        case None =>
          val msg =
            s"Unable to find matching version for $desc in available version: ${versions.available.mkString(", ")}. " +
              "This error may indicate a network or other problem accessing repository."
          Left(new ScalaVersionError(msg))
      }

  }

  object CheckNightly {

    def scala2(
      versionString: String,
      cache: FileCache[Task],
      latestSupportedStableVersions: Seq[String]
    ): Either[BuildException, Unit] = {
      val res = cache.withTtl(0.seconds).logger.use {
        Versions(cache)
          .withModule(scala2Library)
          .withRepositories(Seq(coursier.Repositories.scalaIntegration))
          .result()
          .unsafeRun()(cache.ec)
      }
      if (res.versions.available.contains(versionString))
        Right(())
      else
        Left(
          new NoValidScalaVersionFoundError(res.versions.available, latestSupportedStableVersions)
        )
    }

    def scala3(
      versionString: String,
      cache: FileCache[Task],
      latestSupportedStableVersions: Seq[String]
    ): Either[BuildException, Unit] = {
      val res = cache.withTtl(0.seconds).logger.use {
        Versions(cache)
          .withModule(scala3Library)
          .result()
          .unsafeRun()(cache.ec)
      }
      if (res.versions.available.contains(versionString))
        Right(())
      else
        Left(
          new NoValidScalaVersionFoundError(res.versions.available, latestSupportedStableVersions)
        )
    }
  }

  def validateNonStable(
    scalaVersionStringArg: String,
    cache: FileCache[Task],
    latestSupportedStableVersions: Seq[String]
  ): Either[ScalaVersionError, String] = {
    val versionPool =
      ScalaVersionUtil.allMatchingVersions(Some(scalaVersionStringArg), cache)

    if (versionPool.contains(scalaVersionStringArg))
      if (isSupportedVersion(scalaVersionStringArg))
        Right(scalaVersionStringArg)
      else
        Left(new UnsupportedScalaVersionError(
          scalaVersionStringArg,
          latestSupportedStableVersions
        ))
    else
      Left(new InvalidBinaryScalaVersionError(
        scalaVersionStringArg,
        latestSupportedStableVersions
      ))
  }

  def validateStable(
    scalaVersionStringArg: String,
    cache: FileCache[Task],
    latestSupportedStableVersions: Seq[String],
    maxSupportedStableScalaVersions: Seq[Version]
  ): Either[ScalaVersionError, String] = {
    val versionPool =
      ScalaVersionUtil.allMatchingVersions(Some(scalaVersionStringArg), cache)
        .filter(ScalaVersionUtil.isStable)
    val prefix =
      if (Util.isFullScalaVersion(scalaVersionStringArg)) scalaVersionStringArg
      else if (scalaVersionStringArg.endsWith(".")) scalaVersionStringArg
      else scalaVersionStringArg + "."
    val matchingStableVersions = versionPool.filter(_.startsWith(prefix)).map(Version(_))
    if (matchingStableVersions.isEmpty)
      Left(new InvalidBinaryScalaVersionError(
        scalaVersionStringArg,
        latestSupportedStableVersions
      ))
    else {
      val validMaxVersions = maxSupportedStableScalaVersions
        .filter(_.repr.startsWith(prefix))
      val validMatchingVersions = {
        val filtered = matchingStableVersions.filter(v => validMaxVersions.exists(v <= _))
        if (filtered.isEmpty) matchingStableVersions
        else filtered
      }.filter(v => isSupportedVersion(v.repr))

      validMatchingVersions.find(_.repr == scalaVersionStringArg) match {
        case Some(v)                                => Right(v.repr)
        case None if validMatchingVersions.nonEmpty => Right(validMatchingVersions.max.repr)
        case _ => Left(
            new UnsupportedScalaVersionError(scalaVersionStringArg, latestSupportedStableVersions)
          )
      }
    }
  }

  def default(
    versionPool: Seq[String],
    latestSupportedStableVersions: Seq[String],
    maxSupportedStableScalaVersions: Seq[Version]
  ): Either[ScalaVersionError, String] = {
    val validVersions = versionPool
      .map(Version(_))
      .filter(v => maxSupportedStableScalaVersions.exists(v <= _))
    if (validVersions.isEmpty)
      Left(new NoValidScalaVersionFoundError(
        versionPool,
        latestSupportedStableVersions
      ))
    else
      Right(validVersions.max.repr)
  }

  private def isSupportedVersion(version: String): Boolean =
    version.startsWith("2.12.") || version.startsWith("2.13.") || version.startsWith("3.")

  def isScala2Nightly(version: String): Boolean =
    scala2NightlyRegex.unapplySeq(version).isDefined
  def isScala3Nightly(version: String): Boolean =
    version.startsWith("3") && version.endsWith("-NIGHTLY")

  def isStable(version: String): Boolean =
    !version.exists(_.isLetter)

  def allMatchingVersions(
    maybeScalaVersionArg: Option[String],
    cache: FileCache[Task]
  ): Seq[String] = {

    val modules =
      if (maybeScalaVersionArg.contains("2") || maybeScalaVersionArg.exists(_.startsWith("2.")))
        Seq(scala2Library)
      else if (
        maybeScalaVersionArg.contains("3") || maybeScalaVersionArg.exists(_.startsWith("3."))
      )
        Seq(scala3Library)
      else
        Seq(scala2Library, scala3Library)

    modules
      .flatMap { mod =>
        val versions = cache.logger.use {
          try Versions(cache)
              .withModule(mod)
              .result()
              .unsafeRun()(cache.ec)
          catch {
            case NonFatal(e) => throw new Exception(e)
          }
        }
        versions.versions.available
      }
      .distinct
  }

  extension (sv: String) {
    def asVersion: Version = Version(sv)
  }
}
