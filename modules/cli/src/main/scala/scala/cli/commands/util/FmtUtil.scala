package scala.cli.commands.util

import com.typesafe.config.parser.{ConfigDocument, ConfigDocumentFactory}
import com.typesafe.config.{ConfigParseOptions, ConfigSyntax}

import scala.build.Logger
import scala.build.internal.Constants
import scala.cli.commands.fmt.FmtOptions
import scala.util.control.NonFatal

object FmtUtil {
  private def getGitRoot(workspace: os.Path, logger: Logger): Option[String] =
    try {
      val result = os.proc("git", "rev-parse", "--show-toplevel").call(
        cwd = workspace,
        stderr = os.ProcessOutput.ReadBytes((_, _) => ())
      ).out.trim()
      Option(result)
    }
    catch {
      case NonFatal(e) =>
        logger.log(
          s"""Could not get root of the git repository.
             |Cause: $e""".stripMargin
        )
        None
    }

  /** Based on scalafmt
    * [comment](https://github.com/scalameta/scalafmt/blob/d0c11e98898334969f5f4dfc4bd511630cf00ab9/scalafmt-cli/src/main/scala/org/scalafmt/cli/CliArgParser.scala).
    * First we look for .scalafmt.conf file in the `cwd`. If not found we go to the `git root` and
    * look there.
    *
    * @return
    *   path to found `.scalafmt.conf` file and `version` with `dialect` read from it
    */
  def readVersionAndDialect(
    workspace: os.Path,
    options: FmtOptions,
    logger: Logger
  ): (Option[String], Option[String], Option[os.Path]) = {
    case class RunnerMetaconfig(dialect: String = "")
    object RunnerMetaconfig {
      lazy val default: RunnerMetaconfig = RunnerMetaconfig("")
      implicit lazy val surface: metaconfig.generic.Surface[RunnerMetaconfig] =
        metaconfig.generic.deriveSurface[RunnerMetaconfig]
      implicit lazy val decoder: metaconfig.ConfDecoder[RunnerMetaconfig] =
        metaconfig.generic.deriveDecoder[RunnerMetaconfig](default)
    }
    case class ScalafmtMetaconfig(
      version: String = "",
      runner: RunnerMetaconfig = RunnerMetaconfig("")
    )
    object ScalafmtMetaconfig {
      lazy val default: ScalafmtMetaconfig = ScalafmtMetaconfig()
      implicit lazy val surface: metaconfig.generic.Surface[ScalafmtMetaconfig] =
        metaconfig.generic.deriveSurface[ScalafmtMetaconfig]
      implicit lazy val decoder: metaconfig.ConfDecoder[ScalafmtMetaconfig] =
        metaconfig.generic.deriveDecoder[ScalafmtMetaconfig](default)
    }
    val confName = ".scalafmt.conf"
    val pathMaybe =
      options.scalafmtConfStr.flatMap { s =>
        val tmpConfPath = workspace / Constants.workspaceDirName / ".scalafmt.conf"
        os.write.over(tmpConfPath, s, createFolders = true)
        Some(tmpConfPath)
      }.orElse {
        options.scalafmtConf.flatMap { p =>
          val confPath = os.Path(p, os.pwd)
          logger.debug(s"Checking for $confPath.")
          if (os.exists(confPath)) Some(confPath)
          else
            logger.message(s"WARNING: provided file doesn't exist $confPath")
            None
        }.orElse {
          logger.debug(s"Checking for $confName in cwd.")
          val confInCwd = workspace / confName
          if (os.exists(confInCwd)) Some(confInCwd)
          else {
            logger.debug(s"Checking for $confName in git root.")
            val gitRootMaybe       = getGitRoot(workspace, logger)
            val confInGitRootMaybe = gitRootMaybe.map(os.Path(_) / confName)
            confInGitRootMaybe.find(os.exists(_))
          }
        }
      }

    val confContentMaybe = pathMaybe.flatMap { path =>
      val either = metaconfig.Hocon.parseInput[ScalafmtMetaconfig](
        metaconfig.Input.File(path.toNIO)
      ).toEither
      either.left.foreach(confErr => logger.log(confErr.toString()))
      either.toOption
    }
    val versionMaybe = confContentMaybe.collect {
      case conf if conf.version.trim.nonEmpty => conf.version
    }
    val dialectMaybe = confContentMaybe.collect {
      case conf if conf.runner.dialect.trim.nonEmpty => conf.runner.dialect
    }
    (versionMaybe, dialectMaybe, pathMaybe)
  }

  // Based on https://github.com/scalameta/metals/blob/main/metals/src/main/scala/scala/meta/internal/metals/ScalafmtDialect.scala
  sealed abstract class ScalafmtDialect(val value: String)
  object ScalafmtDialect {
    case object Scala3          extends ScalafmtDialect("scala3")
    case object Scala213        extends ScalafmtDialect("scala213")
    case object Scala213Source3 extends ScalafmtDialect("scala213source3")
    case object Scala212        extends ScalafmtDialect("scala212")
    case object Scala212Source3 extends ScalafmtDialect("scala212source3")
    case object Scala211        extends ScalafmtDialect("scala211")

    implicit val ord: Ordering[ScalafmtDialect] = new Ordering[ScalafmtDialect] {

      override def compare(x: ScalafmtDialect, y: ScalafmtDialect): Int =
        prio(x) - prio(y)

      private def prio(d: ScalafmtDialect): Int = d match {
        case Scala211        => 1
        case Scala212        => 2
        case Scala212Source3 => 3
        case Scala213        => 4
        case Scala213Source3 => 5
        case Scala3          => 6
      }
    }

    def fromString(v: String): Option[ScalafmtDialect] = v.toLowerCase match {
      case "default"         => Some(Scala213)
      case "scala211"        => Some(Scala211)
      case "scala212"        => Some(Scala212)
      case "scala212source3" => Some(Scala212Source3)
      case "scala213"        => Some(Scala213)
      case "scala213source3" => Some(Scala213Source3)
      case "scala3"          => Some(Scala3)
      case _                 => None
    }
  }

  /** Based on scalameta [fmt
    * config](https://github.com/scalameta/metals/blob/main/metals/src/main/scala/scala/meta/internal/metals/ScalafmtConfig.scala)
    *
    * @return
    *   Scalafmt configuration content based on previousConfigText with updated fields
    */
  def scalafmtConfigWithFields(
    previousConfigText: String,
    version: Option[String] = None,
    runnerDialect: Option[ScalafmtDialect] = None,
    fileOverride: Map[String, ScalafmtDialect] = Map.empty
  ): String = {

    def docFrom(s: String): ConfigDocument = {
      val options = ConfigParseOptions.defaults().setSyntax(ConfigSyntax.CONF)
      ConfigDocumentFactory.parseString(s, options)
    }

    def withUpdatedVersion(content: String, v: String): String = {
      val doc = docFrom(content)
      if (doc.hasPath("version"))
        doc.withValueText("version", '"' + v + '"').render
      else {
        // prepend to the beggining of file
        val sb = new StringBuilder
        sb.append(s"""version = "$v"""")
        sb.append(System.lineSeparator)
        sb.append(content)
        sb.toString
      }
    }

    def withUpdatedDialect(content: String, d: ScalafmtDialect): String = {
      val doc = docFrom(content)
      if (doc.hasPath("runner.dialect"))
        doc.withValueText("runner.dialect", d.value).render
      else {
        // append to the end
        val sb = new StringBuilder
        sb.append(content)
        val sep    = System.lineSeparator
        val lastLn = content.endsWith(sep)
        if (!lastLn) sb.append(sep)
        sb.append(s"runner.dialect = ${d.value}")
        sb.append(sep)
        sb.toString
      }
    }

    def withFileOverride(
      content: String,
      overrides: Map[String, ScalafmtDialect]
    ): String =
      if (overrides.isEmpty) content
      else {
        val sep = System.lineSeparator
        val values = overrides
          .map { case (key, dialect) =>
            s"""|  "$key" {
                |     runner.dialect = ${dialect.value}
                |  }""".stripMargin
          }
          .mkString(s"fileOverride {$sep", sep, s"$sep}$sep")

        val addSep = if (content.endsWith(sep)) "" else sep
        content + addSep + values
      }

    val doNothing = identity[String] _
    val combined = List(
      version.fold(doNothing)(v => withUpdatedVersion(_, v)),
      runnerDialect.fold(doNothing)(v => withUpdatedDialect(_, v)),
      withFileOverride(_, fileOverride)
    ).reduceLeft(_ andThen _)
    combined(previousConfigText)
  }
}
