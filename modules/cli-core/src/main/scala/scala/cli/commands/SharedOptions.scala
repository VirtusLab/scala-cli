package scala.cli.commands

import java.io.File

import caseapp._
import caseapp.core.help.Help
import coursier.cache.FileCache
import coursier.jvm.{JavaHome, JvmCache}

import scala.build.bloop.bloopgun
import scala.build.{Bloop, Build, LocalRepo, Os}
import scala.build.internal.{CodeWrapper, Constants, CustomCodeClassWrapper}
import scala.scalanative.{build => sn}
import scala.util.Properties

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

  def buildOptions(jmhOptions: Option[Build.RunJmhOptions], jmhVersion: Option[String]): Build.Options =
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
        if (jmhOptions.nonEmpty) jmhVersion.orElse(Some(Constants.jmhVersion))
        else None,
      runJmh = jmhOptions,
      addScalaLibrary = scalaLibrary.orElse(java.map(!_)),
      addRunnerDependencyOpt = runner,
      generateSemanticDbs = semanticDb,
      extraJars = extraJars.flatMap(_.split(File.pathSeparator).toSeq).filter(_.nonEmpty).map(os.Path(_, os.pwd)),
      extraRepositories = LocalRepo.localRepo(directories.directories.localRepoDir).toSeq
    )

  // This might download a JVM if --jvm … is passed or no system JVM is installed
  private lazy val javaCommand0: String = {
    val javaHomeOpt = javaHome.filter(_.nonEmpty)
      .orElse(if (jvm.isEmpty) sys.props.get("java.home") else None)
      .map(os.Path(_, Os.pwd))
      .orElse {
        implicit val ec = coursierCache.ec
        val (id, path) = javaHomeManager.getWithRetainedId(jvm.getOrElse(JavaHome.defaultId)).unsafeRun()
        if (id == JavaHome.systemId) None
        else Some(os.Path(path))
      }
    val ext = if (Properties.isWin) ".exe" else ""

    javaHomeOpt.fold("java")(javaHome => (javaHome / "bin" / s"java$ext").toString)
  }

  def javaHomeLocation(): os.Path = {
    implicit val ec = coursierCache.ec
    val path = javaHomeManager.get(jvm.getOrElse(JavaHome.defaultId)).unsafeRun()
    os.Path(path)
  }

  def javaCommand(): String = javaCommand0

  // This might download a JVM if --jvm … is passed or no system JVM is installed
  def bloopgunConfig(): bloopgun.BloopgunConfig =
    bloopgun.BloopgunConfig.default(() => Bloop.bloopClassPath(logging.logger)).copy(
      javaPath = javaCommand()
    )

  def coursierCache = FileCache().withLogger(logging.logger.coursierLogger)
  def javaHomeManager = {
    val jvmCache = JvmCache().withCache(coursierCache)
    JavaHome().withCache(jvmCache)
  }
}

object SharedOptions {
  implicit val parser = Parser[SharedOptions]
  implicit val help = Help[SharedOptions]
}
