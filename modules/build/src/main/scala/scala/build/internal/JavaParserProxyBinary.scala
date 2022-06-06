package scala.build.internal

import coursier.cache.ArchiveCache
import coursier.util.Task

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.util.Properties

/** Downloads and runs java-class-name as an external binary. */
class JavaParserProxyBinary(
  archiveCache: ArchiveCache[Task],
  javaClassNameVersionOpt: Option[String],
  logger: Logger
) extends JavaParserProxy {

  /** For internal use only
    *
    * Passing archiveCache as an Object, to work around issues with higher-kind type params from
    * Java code.
    */
  def this(
    archiveCache: Object,
    logger: Logger,
    javaClassNameVersionOpt: Option[String]
  ) =
    this(archiveCache.asInstanceOf[ArchiveCache[Task]], javaClassNameVersionOpt, logger)

  def className(content: Array[Byte]): Either[BuildException, Option[String]] = either {

    val platformSuffix = FetchExternalBinary.platformSuffix()
    val version        = javaClassNameVersionOpt.getOrElse(Constants.javaClassNameVersion)
    val (tag, changing) =
      if (version == "latest") ("nightly", true)
      else ("v" + version, false)
    val ext = if (Properties.isWin) ".zip" else ".gz"
    val url =
      s"https://github.com/scala-cli/java-class-name/releases/download/$tag/java-class-name-$platformSuffix$ext"

    val binary =
      value(FetchExternalBinary.fetch(url, changing, archiveCache, logger, "java-class-name"))

    val source =
      os.temp(content, suffix = ".java", perms = if (Properties.isWin) null else "rw-------")
    val output =
      try {
        logger.debug(s"Running $binary $source")
        val res = os.proc(binary, source).call()
        res.out.text().trim
      }
      finally os.remove(source)

    if (output.isEmpty) None
    else Some(output)
  }
}
