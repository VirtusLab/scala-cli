package scala.cli.commands

import java.io.File

import caseapp._
import caseapp.core.help.Help
import scala.build.bloop.bloopgun
import scala.build.{Build, LocalRepo}
import scala.build.internal.{CodeWrapper, Constants, CustomCodeClassWrapper}
import scala.scalanative.{build => sn}
import scala.build.Bloop

final case class SharedOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    js: ScalaJsOptions = ScalaJsOptions(),
  @Recurse
    native: ScalaNativeOptions = ScalaNativeOptions(),

  @Recurse
    directories: SharedDirectoriesOptions = SharedDirectoriesOptions(),

  @Group("Scala")
  @HelpMessage("Set Scala version")
  @ValueDescription("version")
  @Name("scala")
  @Name("S")
    scalaVersion: Option[String] = None,
  @Group("Scala")
  @HelpMessage("Set Scala binary version")
  @ValueDescription("version")
  @Name("scalaBinary")
  @Name("scalaBin")
  @Name("B")
    scalaBinaryVersion: Option[String] = None,

  @Group("Java")
  @HelpMessage("Set Java home")
  @ValueDescription("path")
    javaHome: Option[String] = None,

  @Group("Java")
  @HelpMessage("Use a specific JVM, such as 14, adopt:11, or graalvm:21, or system")
  @ValueDescription("jvm-name")
  @Name("j")
    jvm: Option[String] = None,

  @Group("Java")
  @HelpMessage("Add extra JARs in the class path")
  @ValueDescription("paths")
  @Name("jar")
  @Name("jars")
  @Name("extraJar")
    extraJars: List[String] = Nil,

  @Hidden
    classWrap: Boolean = false,

  @HelpMessage("Watch sources for changes")
  @Name("w")
    watch: Boolean = false,

  @Group("Scala")
  @Hidden
    scalaLibrary: Option[Boolean] = None,
  @Group("Java")
  @Hidden
    java: Option[Boolean] = None,
  @Hidden
    runner: Option[Boolean] = None,

  @HelpMessage("Generate SemanticDBs")
    semanticDb: Option[Boolean] = None,
  @Hidden
    addStubs: Option[Boolean] = None
) {

  def logger = logging.logger

  private def codeWrapper: Option[CodeWrapper] =
    if (classWrap) Some(CustomCodeClassWrapper)
    else None

  def nativeWorkDir(root: os.Path, projectName: String) = root / ".scala" / projectName / "native"

  def scalaNativeLogger: sn.Logger =
    new sn.Logger {
      def trace(msg: Throwable) = ()
      def debug(msg: String) = logger.debug(msg)
      def info(msg: String) = logger.log(msg)
      def warn(msg: String) = logger.log(msg)
      def error(msg: String) = logger.log(msg)
    }

  def buildOptions(enableJmh: Option[Boolean], jmhVersion: Option[String]): Build.Options =
    Build.Options(
      scalaVersion = scalaVersion.map(_.trim).filter(_.nonEmpty),
      scalaBinaryVersion = scalaBinaryVersion.map(_.trim).filter(_.nonEmpty),
      codeWrapper = codeWrapper,
      scalaJsOptions = js.buildOptions,
      scalaNativeOptions = native.buildOptions,
      javaHomeOpt = javaHome.filter(_.nonEmpty),
      jvmIdOpt = jvm.filter(_.nonEmpty),
      addStubsDependencyOpt = addStubs,
      addJmhDependencies =
        if (enableJmh.getOrElse(false)) jmhVersion.orElse(Some(Constants.jmhVersion))
        else None,
      runJmh = enableJmh,
      addScalaLibrary = scalaLibrary.orElse(java.map(!_)),
      addRunnerDependencyOpt = runner,
      generateSemanticDbs = semanticDb,
      extraJars = extraJars.flatMap(_.split(File.pathSeparator).toSeq).filter(_.nonEmpty).map(os.Path(_, os.pwd)),
      extraRepositories = LocalRepo.localRepo(directories.directories.localRepoDir).toSeq
    )

  def bloopgunConfig: bloopgun.BloopgunConfig =
    bloopgun.BloopgunConfig.default(() => Bloop.bloopClassPath(logging.logger))
}

object SharedOptions {
  implicit val parser = Parser[SharedOptions]
  implicit val help = Help[SharedOptions]
}
