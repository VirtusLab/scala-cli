package scala.build.options

import bloop.rifle.VersionUtil.jvmRelease
import coursier.cache.{ArchiveCache, CachePolicy, FileCache}
import coursier.jvm.{JavaHome, JvmCache, JvmChannel, JvmIndex}
import coursier.util.Task
import dependency.AnyDependency

import scala.build.internal.CsLoggerUtil.*
import scala.build.internal.OsLibc
import scala.build.options.BuildOptions.JavaHomeInfo
import scala.build.{Os, Position, Positioned}
import scala.concurrent.ExecutionContextExecutorService
import scala.util.control.NonFatal
import scala.util.{Properties, Try}

/** @javaOpts
  *   java options which will be passed when compiling sources.
  * @javacOptions
  *   java options which will be passed when running an application.
  */
final case class JavaOptions(
  javaHomeOpt: Option[Positioned[os.Path]] = None,
  jvmIdOpt: Option[Positioned[String]] = None,
  jvmIndexOpt: Option[String] = None,
  jvmIndexOs: Option[String] = None,
  jvmIndexArch: Option[String] = None,
  javaOpts: ShadowingSeq[Positioned[JavaOpt]] = ShadowingSeq.empty,
  javacPluginDependencies: Seq[Positioned[AnyDependency]] = Nil,
  javacPlugins: Seq[Positioned[os.Path]] = Nil,
  javacOptions: Seq[Positioned[String]] = Nil
) {

  private def finalJvmIndexOs = jvmIndexOs.getOrElse(OsLibc.jvmIndexOs)

  def javaHomeManager(
    archiveCache: ArchiveCache[Task],
    cache: FileCache[Task],
    verbosity: Int
  ): JavaHome = {
    val indexOs            = finalJvmIndexOs
    val indexArch          = jvmIndexArch.getOrElse(JvmChannel.defaultArchitecture())
    val indexChannel       = JavaOptions.jvmIndexChannel(jvmIndexOpt, indexOs, indexArch)
    val fallbackChannelOpt =
      JavaOptions.offlineJvmIndexFallback(
        jvmIndexOpt,
        indexChannel,
        isOffline = JavaOptions.isOffline(cache)
      )
    val indexTask = {
      val msg    = if (verbosity > 0) "Downloading JVM index" else ""
      val cache0 = cache.withMessage(msg)
      // repositories are only used by module-based channels, which jvmIndexChannel never returns
      def load(channel: JvmChannel): Task[JvmIndex] =
        JvmIndex.load(cache0, Nil, channel, Some(indexOs), Some(indexArch))
      cache0.logger.using {
        fallbackChannelOpt match {
          case None                  => load(indexChannel)
          case Some(fallbackChannel) =>
            load(indexChannel).attempt.flatMap {
              case Right(index) => Task.point(index)
              case Left(ex)     =>
                load(fallbackChannel).attempt.flatMap {
                  case Right(index)     => Task.point(index)
                  case Left(fallbackEx) =>
                    ex.addSuppressed(fallbackEx)
                    Task.fail(ex)
                }
            }
        }
      }
    }
    val jvmCache = JvmCache()
      .withIndex(indexTask)
      .copy(
        archiveCache = archiveCache.copy(cache = cache.withMessage("Downloading JVM")),
        os = indexOs,
        architecture = indexArch
      )
    JavaHome().withCache(jvmCache)
  }

  def javaHomeLocationOpt(
    archiveCache: ArchiveCache[Task],
    cache: FileCache[Task],
    verbosity: Int
  ): Option[Positioned[os.Path]] =
    lazy val javaHomeManager0 = javaHomeManager(archiveCache, cache, verbosity)

    implicit val ec: ExecutionContextExecutorService = cache.ec

    def isJvmVersion(jvmId: String): Boolean =
      jvmId.forall(c => c.isDigit || c == '.' || c == '-')

    javaHomeOpt
      .orElse {
        if (jvmIdOpt.isEmpty)
          findLocalDefaultJava()
        else if (
          Properties.isMac && jvmIdOpt.exists(jvmId =>
            isJvmVersion(jvmId.value.stripPrefix("system|"))
          )
        )
          val jvmVersionOpt = for {
            jvmId <- jvmIdOpt
            maybeJvmVersion = jvmId.value.stripPrefix("system|")
            jvmVersion <- jvmRelease(maybeJvmVersion)
          } yield jvmVersion.toString

          findLocalJavaOnMacOs(jvmVersionOpt)
        else None
      }
      .orElse {
        jvmIdOpt.map(_.value).map { jvmId =>

          cache.logger.use {
            val enforceLiberica =
              finalJvmIndexOs == "linux-musl" &&
              isJvmVersion(jvmId)
            val enforceZulu =
              Os.isArmArchitecture &&
              isJvmVersion(jvmId)
            val jvmId0 =
              if (enforceLiberica)
                s"liberica:$jvmId" // FIXME Workaround, until this is automatically handled by coursier-jvm
              else if (enforceZulu) // default jvmId adoptium doesn't support java 8 for M1
                s"zulu:$jvmId"
              else
                jvmId

            val path =
              try javaHomeManager0
                  .withMessage(s"Downloading JVM $jvmId0")
                  .get(jvmId0).unsafeRun()
              catch {
                case NonFatal(e) => throw new Exception(e)
              }
            Positioned(Position.CommandLine("--jvm"), os.Path(path))
          }
        }
      }

  def javaHomeLocation(
    archiveCache: ArchiveCache[Task],
    cache: FileCache[Task],
    verbosity: Int
  ): Positioned[os.Path] =
    javaHomeLocationOpt(archiveCache, cache, verbosity).getOrElse {
      val jvmId            = OsLibc.defaultJvm(finalJvmIndexOs)
      val javaHomeManager0 = javaHomeManager(archiveCache, cache, verbosity)
        .withMessage(s"Downloading JVM $jvmId")
      implicit val ec: ExecutionContextExecutorService = cache.ec
      cache.logger.use {
        val path =
          try javaHomeManager0.get(jvmId).unsafeRun()
          catch {
            case NonFatal(e) => throw new Exception(e)
          }
        Positioned(Position.Custom("OsLibc.defaultJvm"), os.Path(path))
      }
    }

  def javaHome(
    archiveCache: ArchiveCache[Task],
    cache: FileCache[Task],
    verbosity: Int
  ): Positioned[JavaHomeInfo] =
    javaHomeLocation(archiveCache, cache, verbosity).map { javaHome =>
      val (javaVersion, javaCmd) = OsLibc.javaHomeVersion(javaHome)
      JavaHomeInfo(javaHome, javaCmd, javaVersion)
    }

  private def findLocalDefaultJava(): Option[Positioned[os.Path]] =
    Option(System.getenv("JAVA_HOME")).filter(_.nonEmpty).map(p =>
      Positioned(Position.Custom("JAVA_HOME env"), os.Path(p, os.pwd))
    ).orElse(
      sys.props.get("java.home").map(p =>
        Positioned(Position.Custom("java.home prop"), os.Path(p, os.pwd))
      )
    ).orElse(
      if (Properties.isMac)
        findLocalJavaOnMacOs(None)
      else None
    )
  private def findLocalJavaOnMacOs(jvmIdOpt: Option[String]): Option[Positioned[os.Path]] =
    Try {
      jvmIdOpt.fold(os.proc("/usr/libexec/java_home")) { jvmId =>
        os.proc(
          "/usr/libexec/java_home",
          "-v",
          jvmId.stripPrefix("system|"),
          "--failfast"
        )
      }.call(os.pwd, check = true, mergeErrIntoOut = true)
        .out.text().trim()
    }
      .toOption
      .flatMap(p => Try(os.Path(p, os.pwd)).toOption)
      .filter(os.exists(_))
      .map(p => Positioned(Position.Custom("/usr/libexec/java_home -v"), p))
}

object JavaOptions {
  implicit val hasHashData: HasHashData[JavaOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[JavaOptions]     = ConfigMonoid.derive

  /** The JVM index to use: the one passed by users if any, else the coursier default for the passed
    * OS and CPU architecture (the per-OS / CPU index with checksums when there's one, the legacy
    * index else), same as the coursier CLI. Using the same index as the coursier CLI also allows to
    * use JVMs fetched by it in offline mode.
    */
  def jvmIndexChannel(jvmIndexOpt: Option[String], os: String, arch: String): JvmChannel =
    jvmIndexOpt.fold(JvmChannel.default(os, arch))(JvmChannel.url)

  /** JVM index to fall back to if `indexChannel` can't be loaded.
    *
    * Only in offline mode, when users didn't pass a JVM index, and the default index is the per-OS
    * / CPU one: caches populated by Scala CLI versions using coursier < 2.1.27 (or the coursier CLI
    * < 2.1.27) only have the legacy index, which this falls back to so that JVMs already in the
    * cache can still be used offline. When online, the per-OS / CPU index is always used, as only
    * it allows to check the checksums of JVM archives.
    */
  def offlineJvmIndexFallback(
    jvmIndexOpt: Option[String],
    indexChannel: JvmChannel,
    isOffline: Boolean
  ): Option[JvmChannel] =
    val legacyChannel = JvmChannel.gitHub()
    Option.when(isOffline && jvmIndexOpt.isEmpty && indexChannel != legacyChannel)(legacyChannel)

  /** Whether `cache` is in offline mode, i.e. only reads from the local cache */
  def isOffline(cache: FileCache[Task]): Boolean =
    cache.cachePolicies.nonEmpty && cache.cachePolicies.forall(_ == CachePolicy.LocalOnly)
}
