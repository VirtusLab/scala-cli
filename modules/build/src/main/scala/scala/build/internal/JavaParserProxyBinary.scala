package scala.build.internal

import coursier.cache.ArchiveCache
import coursier.util.Task
import dependency.*

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.util.Properties

/** Downloads and runs java-class-name as an external binary. */
class JavaParserProxyBinary(
  archiveCache: ArchiveCache[Task],
  javaClassNameVersionOpt: Option[String],
  logger: Logger,
  javaCommand: () => String
) extends JavaParserProxy {

  def className(content: Array[Byte]): Either[BuildException, Option[String]] = either {

    val platformSuffix  = FetchExternalBinary.platformSuffix()
    val version         = javaClassNameVersionOpt.getOrElse(Constants.javaClassNameVersion)
    val (tag, changing) =
      if (version == "latest") ("nightly", true)
      else ("v" + version, false)
    val ext = if (Properties.isWin) ".zip" else ".gz"
    val url =
      s"https://github.com/VirtusLab/java-class-name/releases/download/$tag/java-class-name-$platformSuffix$ext"

    val params = ExternalBinaryParams(
      url,
      changing,
      "java-class-name",
      Seq(
        dep"${Constants.javaClassNameOrganization}:${Constants.javaClassNameName}:${Constants.javaClassNameVersion}"
      ),
      "scala.cli.javaclassname.JavaClassName" // FIXME I'd rather not hardcode that, but automatic detection is cumbersome to setup…
    )
    val binary =
      value(FetchExternalBinary.fetch(params, archiveCache, logger, javaCommand))

    val source =
      os.temp(content, suffix = ".java", perms = if (Properties.isWin) null else "rw-------")
    val command = binary.command
    val output  =
      try {
        logger.debug(s"Running $command $source")
        val res = os.proc(command, source).call()
        res.out.trim()
      }
      finally os.remove(source)
    if (output.isEmpty) None
    else Some(output)
  }
}
