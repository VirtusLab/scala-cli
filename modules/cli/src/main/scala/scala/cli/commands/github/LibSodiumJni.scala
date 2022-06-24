package scala.cli.commands.github

import coursier.cache.{ArchiveCache, Cache, CacheLogger}
import coursier.core.Type
import coursier.util.Task

import java.io.InputStream
import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.internal.{Constants, FetchExternalBinary}
import scala.cli.internal.{Constants => CliConstants}
import scala.util.Properties
import scala.util.control.NonFatal

object LibSodiumJni {

  private def isGraalvmNativeImage: Boolean =
    sys.props.contains("org.graalvm.nativeimage.imagecode")

  private def launcherKindOpt = {
    val path   = CliConstants.launcherTypeResourcePath
    val resUrl = Thread.currentThread.getContextClassLoader.getResource(path)
    if (resUrl == null) None
    else {
      var is: InputStream = null
      try {
        is = resUrl.openStream()
        Some(new String(resUrl.openStream().readAllBytes(), StandardCharsets.UTF_8))
      }
      finally
        if (is != null)
          is.close()
    }
  }

  private def libsodiumVersion    = Constants.libsodiumVersion
  private def libsodiumjniVersion = Constants.libsodiumjniVersion

  private def archiveUrlAndPath() =
    if (Properties.isLinux && launcherKindOpt.contains("static"))
      // Should actually be unused, as we statically link libsodium from the static launcher
      // Keeping it just-in-case. This could be useful from a musl-based JVM.
      (
        s"https://dl-cdn.alpinelinux.org/alpine/v3.15/main/x86_64/libsodium-$libsodiumVersion-r0.apk",
        os.rel / "usr" / "lib" / "libsodium.so.23.3.0" // FIXME Could this change?
      )
    else {
      val condaPlatform = FetchExternalBinary.condaPlatform
      // FIXME These suffixes seem to be hashes, and seem to change at every version…
      // We'd need a way to find them automatically.
      val suffix = condaPlatform match {
        case "linux-64"      => "-h36c2ea0_1"
        case "linux-aarch64" => "-hb9de7d4_1"
        case "osx-64"        => "-hbcb3906_1"
        case "osx-arm64"     => "-h27ca646_1"
        case "win-64"        => "-h62dcd97_1"
        case other           => sys.error(s"Unrecognized conda platform $other")
      }
      val relPath = condaPlatform match {
        case "linux-64"      => os.rel / "lib" / "libsodium.so"
        case "linux-aarch64" => os.rel / "lib" / "libsodium.so"
        case "osx-64"        => os.rel / "lib" / "libsodium.dylib"
        case "osx-arm64"     => os.rel / "lib" / "libsodium.dylib"
        case "win-64"        => os.rel / "Library" / "bin" / "libsodium.dll"
        case other           => sys.error(s"Unrecognized conda platform $other")
      }
      (
        s"https://anaconda.org/conda-forge/libsodium/$libsodiumVersion/download/$condaPlatform/libsodium-$libsodiumVersion$suffix.tar.bz2",
        relPath
      )
    }

  private def jniLibArtifact(cache: Cache[Task]) = {

    import dependency._
    import scala.build.internal.Util.DependencyOps

    val classifier = FetchExternalBinary.platformSuffix(supportsMusl = false)
    val ext =
      if (Properties.isLinux) "so"
      else if (Properties.isMac) "dylib"
      else if (Properties.isWin) "dll"
      else sys.error(s"Unrecognized operating system: ${sys.props("os.name")}")

    val dep =
      dep"io.github.alexarchambault.tmp.libsodiumjni:libsodiumjni:$libsodiumjniVersion,intransitive,classifier=$classifier,ext=$ext,type=$ext"
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
      val (archiveUrl, pathInArchive) = archiveUrlAndPath()
      val sodiumLib = value {
        FetchExternalBinary.fetch(
          archiveUrl,
          changing = false,
          archiveCache,
          logger,
          "",
          launcherPathOpt = Some(pathInArchive),
          makeExecutable = false
        )
      }

      val f = jniLibArtifact(cache)

      System.load(sodiumLib.toString)
      libsodiumjni.internal.LoadLibrary.initialize(f.toString)
    }

    libsodiumjni.Sodium.init()
  }

}
