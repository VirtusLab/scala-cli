package scala.cli.commands.github

import coursier.cache.{ArchiveCache, Cache, CacheLogger}
import coursier.core.Type
import coursier.util.Task

import java.nio.charset.StandardCharsets
import java.util.Locale

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.internal.{Constants, FetchExternalBinary}
import scala.build.internals.EnvVar
import scala.cli.internal.Constants as CliConstants
import scala.util.Properties
import scala.util.control.NonFatal

object LibSodiumJni {

  private def isGraalvmNativeImage: Boolean =
    sys.props.contains("org.graalvm.nativeimage.imagecode")

  private def launcherKindOpt: Option[String] =
    Option(
      Thread.currentThread.getContextClassLoader.getResource(CliConstants.launcherTypeResourcePath)
    ).map { resUrl =>
      val is = resUrl.openStream()
      try new String(is.readAllBytes(), StandardCharsets.UTF_8)
      finally is.close()
    }

  private def libsodiumVersion    = Constants.libsodiumVersion
  private def libsodiumjniVersion = Constants.libsodiumjniVersion

  final case class LibSodiumArchive(url: String, pathInArchive: os.RelPath)

  /** Where to get the libsodium shared library that the libsodiumjni native library needs, if any.
    *
    * @param platformSuffix
    *   the libsodiumjni artifact classifier, as computed by [[FetchExternalBinary.platformSuffix]]
    * @return
    *   `Right(None)` if no libsodium needs to be loaded, `Left(error)` on unsupported platforms
    */
  def libsodiumArchive(
    platformSuffix: String,
    libsodiumVersion: String
  ): Either[String, Option[LibSodiumArchive]] =
    platformSuffix match {
      case "x86_64-pc-linux" | "aarch64-pc-linux" | "x86_64-apple-darwin" |
          "aarch64-apple-darwin" =>
        Right(None)
      case "x86_64-pc-win32" | "aarch64-pc-win32" =>
        val msvcArch = if platformSuffix.startsWith("aarch64-") then "ARM64" else "x64"
        Right(Some(LibSodiumArchive(
          s"https://download.libsodium.org/libsodium/releases/libsodium-$libsodiumVersion-stable-msvc.zip",
          os.rel / "libsodium" / msvcArch / "Release" / "v143" / "dynamic" / "libsodium.dll"
        )))
      case other => Left(s"libsodium isn't supported on platform $other")
    }

  /** Extension of the libsodiumjni shared library artifact for a given platform. */
  def jniLibExtension(platformSuffix: String): Either[String, String] =
    platformSuffix match {
      case s"$_-pc-linux"     => Right("so")
      case s"$_-apple-darwin" => Right("dylib")
      case s"$_-pc-win32"     => Right("dll")
      case other              => Left(s"libsodiumjni isn't supported on platform $other")
    }

  private def jniLibArtifact(cache: Cache[Task], classifier: String) = {

    import dependency._
    import scala.build.internal.Util.DependencyOps

    val ext = jniLibExtension(classifier).fold(sys.error, identity)

    val dep =
      dep"org.virtuslab.scala-cli:libsodiumjni:$libsodiumjniVersion,intransitive,classifier=$classifier,ext=$ext,type=$ext"
    val fetch = coursier.Fetch()
      .addDependencies(dep.toCs)
      .addArtifactTypes(Type(ext))
    val files = cache.loggerOpt.getOrElse(CacheLogger.nop).use {
      try fetch.run()
      catch {
        case NonFatal(e) =>
          throw new Exception(e)
      }
    }
    files match {
      case Seq()     => sys.error(s"Cannot find $dep")
      case Seq(file) => file
      case other     => sys.error(s"Unexpectedly got too many files while resolving $dep: $other")
    }
  }

  def init(
    cache: Cache[Task],
    archiveCache: ArchiveCache[Task],
    logger: Logger
  ): Either[BuildException, Unit] = either {

    val allStaticallyLinked =
      Properties.isWin && isGraalvmNativeImage ||
      Properties.isLinux && launcherKindOpt.contains("static")

    if (!allStaticallyLinked) {
      val classifier = FetchExternalBinary.platformSuffix(supportsMusl = false)
      val archiveOpt = libsodiumArchive(classifier, libsodiumVersion).fold(sys.error, identity)

      archiveOpt match {
        case None          => // libsodium is statically linked in libsodiumjni on this platform
        case Some(archive) =>
          val sodiumLibOpt = value {
            FetchExternalBinary.fetchLauncher(
              archive.url,
              changing = false,
              archiveCache,
              logger,
              "",
              launcherPathOpt = Some(archive.pathInArchive),
              makeExecutable = false
            )
          }
          sodiumLibOpt match {
            case Some(sodiumLib) =>
              System.load(sodiumLib.toString)
            case None =>
              val allow = EnvVar.ScalaCli.allowSodiumJni.valueOpt
                .map(_.toLowerCase(Locale.ROOT))
                .forall {
                  case "false" | "0" => false
                  case _             => true
                }
              if (allow)
                System.loadLibrary("sodium")
              else
                value(Left(new LibSodiumNotFound(archive.url)))
          }
      }

      val f = jniLibArtifact(cache, classifier)
      libsodiumjni.internal.LoadLibrary.initialize(f.toString)
    }

    libsodiumjni.Sodium.init()
  }

  final class LibSodiumNotFound(url: String) extends BuildException(s"libsodium: $url not found")

}
