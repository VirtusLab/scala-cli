package scala.cli.commands.github

import coursier.cache.{Cache, CacheLogger}
import coursier.core.Type
import coursier.util.Task

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.either
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
      val classifier = FetchExternalBinary.platformSuffix(supportsMusl = false)
      val f          = jniLibArtifact(cache, classifier)
      logger.debug(s"Loading libsodiumjni from $f")
      libsodiumjni.internal.LoadLibrary.initialize(f.toString)

    libsodiumjni.Sodium.init()
  }

}
