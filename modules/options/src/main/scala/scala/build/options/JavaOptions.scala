package scala.build.options

import coursier.cache.{ArchiveCache, FileCache}
import coursier.jvm.{JavaHome, JvmCache, JvmIndex}
import coursier.util.Task
import dependency.AnyDependency

import scala.build.internal.CsLoggerUtil._
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
    val indexUrl = jvmIndexOpt.getOrElse(JvmIndex.coursierIndexUrl)
    val indexTask = {
      val msg    = if (verbosity > 0) "Downloading JVM index" else ""
      val cache0 = cache.withMessage(msg)
      cache0.logger.using {
        JvmIndex.load(cache0, indexUrl)
      }
    }
    val jvmCache = JvmCache()
      .withIndex(indexTask)
      .withArchiveCache(
        archiveCache.withCache(
          cache.withMessage("Downloading JVM")
        )
      )
      .withOs(finalJvmIndexOs)
      .withArchitecture(jvmIndexArch.getOrElse(JvmIndex.defaultArchitecture()))
    JavaHome().withCache(jvmCache)
  }

  def javaHomeLocationOpt(
    archiveCache: ArchiveCache[Task],
    cache: FileCache[Task],
    verbosity: Int
  ): Option[Positioned[os.Path]] =
    javaHomeOpt
      .orElse {
        if (jvmIdOpt.isEmpty)
          Option(System.getenv("JAVA_HOME")).map(p =>
            Positioned(Position.Custom("JAVA_HOME env"), os.Path(p, os.pwd))
          ).orElse(
            sys.props.get("java.home").map(p =>
              Positioned(Position.Custom("java.home prop"), os.Path(p, os.pwd))
            )
          ).orElse(
            if (Properties.isMac)
              Try(os.proc("/usr/libexec/java_home").call(os.pwd, check = false).out.text().trim())
                .toOption
                .flatMap(p => Try(os.Path(p, os.pwd)).toOption)
                .filter(os.exists(_))
                .map(p => Positioned(Position.Custom("/usr/libexec/java_home"), p))
            else None
          )
        else None
      }
      .orElse {
        jvmIdOpt.map(_.value).map { jvmId =>
          implicit val ec: ExecutionContextExecutorService = cache.ec
          cache.logger.use {
            val enforceLiberica =
              finalJvmIndexOs == "linux-musl" &&
              jvmId.forall(c => c.isDigit || c == '.' || c == '-')
            val enforceZulu =
              Os.isArmArchitecture &&
              jvmId.forall(c => c.isDigit || c == '.' || c == '-')
            val jvmId0 =
              if (enforceLiberica)
                s"liberica:$jvmId" // FIXME Workaround, until this is automatically handled by coursier-jvm
              else if (enforceZulu) // default jvmId adoptium doesn't support java 8 for M1
                s"zulu:$jvmId"
              else
                jvmId
            val javaHomeManager0 = javaHomeManager(archiveCache, cache, verbosity)
              .withMessage(s"Downloading JVM $jvmId0")
            val path =
              try javaHomeManager0.get(jvmId0).unsafeRun()
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
      val jvmId = OsLibc.defaultJvm(finalJvmIndexOs)
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

}

object JavaOptions {
  implicit val hasHashData: HasHashData[JavaOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[JavaOptions]     = ConfigMonoid.derive
}
