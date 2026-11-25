package scala.cli.commands.github

import coursier.cache.{Cache, CacheLogger}
import coursier.core.Type
import coursier.util.Task

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.internal.{Constants, FetchExternalBinary}
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

  private def libsodiumjniVersion = Constants.libsodiumjniVersion

  /** CPU architectures libsodiumjni publishes native libraries for. */
  private val supportedArchs = Set("x86_64", "aarch64")

  /** Extension of the libsodiumjni shared library artifact for a given platform. */
  def jniLibExtension(platformSuffix: String): Either[String, String] =
    platformSuffix match {
      case s"$arch-pc-linux" if supportedArchs(arch)     => Right("so")
      case s"$arch-apple-darwin" if supportedArchs(arch) => Right("dylib")
      case s"$arch-pc-win32" if supportedArchs(arch)     => Right("dll")
      case other => Left(s"libsodiumjni isn't supported on platform $other")
    }

  private def jniLibArtifact(cache: Cache[Task], classifier: String, ext: String) = {

    import dependency._
    import scala.build.internal.Util.DependencyOps

    val dep =
      dep"org.virtuslab.scala-cli:libsodiumjni:$libsodiumjniVersion,intransitive,classifier=$classifier,ext=$ext,type=$ext"
    val fetch = coursier.Fetch()
      .withCache(cache)
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

  /** Loads the libsodiumjni native library, unless it's statically linked in the launcher.
    *
    * Since libsodiumjni 1.0.1, libsodium is statically linked in the libsodiumjni shared library on
    * all supported platforms, so nothing else needs to be loaded.
    */
  def init(
    cache: Cache[Task],
    logger: Logger
  ): Either[BuildException, Unit] = either {

    val allStaticallyLinked =
      Properties.isWin && isGraalvmNativeImage ||
      Properties.isLinux && launcherKindOpt.contains("static")

    if !allStaticallyLinked then
      val classifier = value {
        FetchExternalBinary.maybePlatformSuffix(supportsMusl = false)
          .left.map(new LibSodiumJniUnsupportedPlatform(_))
      }
      val ext = value(jniLibExtension(classifier).left.map(new LibSodiumJniUnsupportedPlatform(_)))
      val f   = jniLibArtifact(cache, classifier, ext)
      logger.debug(s"Loading libsodiumjni from $f")
      libsodiumjni.internal.LoadLibrary.initialize(f.toString)

    libsodiumjni.Sodium.init()
  }

  final class LibSodiumJniUnsupportedPlatform(message: String) extends BuildException(message)

}
