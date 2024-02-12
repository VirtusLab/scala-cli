package scala.build.options

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import coursier.Versions
import coursier.cache.{ArtifactError, FileCache}
import coursier.core.{Module, Repository, Version, Versions as CoreVersions}
import coursier.util.{Artifact, Task}

import java.io.File

import scala.build.CoursierUtils.*
import scala.build.EitherCps.{either, value}
import scala.build.errors.{
  BuildException,
  InvalidBinaryScalaVersionError,
  NoValidScalaVersionFoundError,
  ScalaVersionError,
  UnsupportedScalaVersionError
}
import scala.build.internal.Regexes.scala2NightlyRegex
import scala.build.internal.{Constants, Util}
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.Try
import scala.util.control.NonFatal

object ScalaVersionUtil {

  private def scala2Library = cmod"org.scala-lang:scala-library"
  private def scala3Library = cmod"org.scala-lang:scala3-library_3"
  def scala212Nightly       = "2.12.nightly"
  def scala213Nightly       = List("2.13.nightly", "2.nightly")
  def scala3Nightly         = "3.nightly"
  def scala3Lts             = List("3.lts", "lts")
  // not valid versions, defined only for informative error messages
  def scala2Lts = List("2.13.lts", "2.12.lts", "2.lts")
  extension (cache: FileCache[Task]) {
    def fileWithTtl0(artifact: Artifact): Either[ArtifactError, File] =
      cache.logger.use {
        try cache.withTtl(0.seconds).file(artifact).run.unsafeRun()(cache.ec)
        catch {
          case NonFatal(e) => throw new Exception(e)
        }
      }

    def versions(
      module: Module,
      repositories: Seq[Repository] = Seq.empty,
      ttl: Option[Duration] = None
    ): Versions.Result =
      val cacheWithTtl = ttl.map(cache.withTtl).getOrElse(cache)
      cacheWithTtl.logger.use {
        Versions(cacheWithTtl)
          .withModule(module)
          .addRepositories(repositories: _*)
          .result()
          .unsafeRun()(cacheWithTtl.ec)
      }
    def versionsWithTtl0(
      module: Module,
      repositories: Seq[Repository] = Seq.empty
    ): Versions.Result = versions(module, repositories, Some(0.seconds))
  }

  extension (versionsResult: Versions.Result) {
    def verify(versionString: String): Either[BuildException, Unit] =
      if versionsResult.versions.available.contains(versionString) then Right(())
      else Left(NoValidScalaVersionFoundError(versionString))
  }

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
        val res = cache.fileWithTtl0(artifact)
          .left.map { err =>
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
      cache: FileCache[Task]
    ): Either[BuildException, String] = {
      val res = cache.versionsWithTtl0(scala3Library)
        .versions.available.filter(_.endsWith("-NIGHTLY"))

      val threeXNightlies = res.filter(_.startsWith(s"3.$threeSubBinaryNum.")).map(Version(_))
      if threeXNightlies.nonEmpty then Right(threeXNightlies.max.repr)
      else Left(NoValidScalaVersionFoundError())
    }

    /** @return
      *   Either a BuildException or the calculated (ScalaVersion, ScalaBinaryVersion) tuple
      */
    def scala3(cache: FileCache[Task]): Either[BuildException, String] =
      latestScalaVersionFrom(
        cache.versionsWithTtl0(scala3Library).versions,
        "latest Scala 3 nightly build"
      )

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
      cache: FileCache[Task]
    ): Either[BuildException, Unit] =
      cache.versionsWithTtl0(scala2Library, Seq(coursier.Repositories.scalaIntegration))
        .verify(versionString)

    def scala3(
      versionString: String,
      cache: FileCache[Task]
    ): Either[BuildException, Unit] =
      cache.versionsWithTtl0(scala3Library).verify(versionString)
  }

  def validateNonStable(
    scalaVersionStringArg: String,
    cache: FileCache[Task],
    repositories: Seq[Repository]
  ): Either[ScalaVersionError, String] = {
    val versionPool =
      ScalaVersionUtil.allMatchingVersions(Some(scalaVersionStringArg), cache, repositories)

    if (versionPool.contains(scalaVersionStringArg))
      if (isSupportedVersion(scalaVersionStringArg))
        Right(scalaVersionStringArg)
      else
        Left(new UnsupportedScalaVersionError(scalaVersionStringArg))
    else
      Left(new InvalidBinaryScalaVersionError(scalaVersionStringArg))
  }

  def validateStable(
    scalaVersionStringArg: String,
    cache: FileCache[Task],
    repositories: Seq[Repository]
  ): Either[ScalaVersionError, String] = {
    val versionPool =
      ScalaVersionUtil.allMatchingVersions(Some(scalaVersionStringArg), cache, repositories)
        .filter(ScalaVersionUtil.isStable)

    val prefix =
      if (Util.isFullScalaVersion(scalaVersionStringArg)) scalaVersionStringArg
      else if (scalaVersionStringArg.endsWith(".")) scalaVersionStringArg
      else scalaVersionStringArg + "."
    val matchingStableVersions = versionPool.filter(_.startsWith(prefix)).map(Version(_))
    if (matchingStableVersions.isEmpty)
      Left(new InvalidBinaryScalaVersionError(scalaVersionStringArg))
    else {
      val supportedMatchingStableVersions =
        matchingStableVersions.filter(v => isSupportedVersion(v.repr))

      supportedMatchingStableVersions.find(_.repr == scalaVersionStringArg) match {
        case Some(v) => Right(v.repr)
        case None if supportedMatchingStableVersions.nonEmpty =>
          Right(supportedMatchingStableVersions.max.repr)
        case _ => Left(
            new UnsupportedScalaVersionError(scalaVersionStringArg)
          )
      }
    }
  }

  private def isSupportedVersion(version: String): Boolean =
    version.startsWith("2.12.") || version.startsWith("2.13.") || version.startsWith("3.")

  def isScala2Nightly(version: String): Boolean =
    scala2NightlyRegex.unapplySeq(version).isDefined
    || (scala212Nightly +: scala213Nightly).contains(version)

  def isScala3Nightly(version: String): Boolean =
    version.startsWith("3") && version.endsWith("-NIGHTLY")

  def isStable(version: String): Boolean =
    !version.exists(_.isLetter)

  def allMatchingVersions(
    maybeScalaVersionArg: Option[String],
    cache: FileCache[Task],
    repositories: Seq[Repository]
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
              .addRepositories(repositories: _*)
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
