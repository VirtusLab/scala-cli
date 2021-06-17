package scala.cli.commands

import java.io.File

import caseapp._
import caseapp.core.help.Help
import coursier.cache.FileCache

import scala.build.bloop.bloopgun
import scala.build.{Bloop, LocalRepo, Os}
import scala.build.internal.{CodeWrapper, Constants, CustomCodeClassWrapper}
import scala.build.options.{BuildOptions, ClassPathOptions, InternalDependenciesOptions, InternalOptions, JavaOptions, JmhOptions, ScalaOptions, ScriptOptions}
import scala.scalanative.{build => sn}
import scala.util.Properties
import scala.build.Inputs
import java.io.InputStream
import scala.build.Logger
import java.io.ByteArrayOutputStream
import dependency.parser.DependencyParser

final case class SharedOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    js: ScalaJsOptions = ScalaJsOptions(),
  @Recurse
    native: ScalaNativeOptions = ScalaNativeOptions(),

  @Recurse
    directories: SharedDirectoriesOptions = SharedDirectoriesOptions(),

  @Recurse
    dependencies: SharedDependencyOptions = SharedDependencyOptions(),

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

  @HelpMessage("Pass configuration files")
  @Name("conf")
  @Name("C")
    config: List[String] = Nil,

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

  def buildOptions(enableJmh: Boolean, jmhVersion: Option[String]): BuildOptions =
    BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = scalaVersion.map(_.trim).filter(_.nonEmpty),
        scalaBinaryVersion = scalaBinaryVersion.map(_.trim).filter(_.nonEmpty),
        addScalaLibrary = scalaLibrary.orElse(java.map(!_)),
        generateSemanticDbs = semanticDb
      ),
      scriptOptions = ScriptOptions(
        codeWrapper = codeWrapper
      ),
      scalaJsOptions = js.buildOptions,
      scalaNativeOptions = native.buildOptions,
      javaOptions = JavaOptions(
        javaHomeOpt = javaHome.filter(_.nonEmpty),
        jvmIdOpt = jvm.filter(_.nonEmpty),
      ),
      internalDependencies = InternalDependenciesOptions(
        addStubsDependencyOpt = addStubs,
        addRunnerDependencyOpt = runner
      ),
      jmhOptions = JmhOptions(
        addJmhDependencies =
          if (enableJmh) jmhVersion.orElse(Some(Constants.jmhVersion))
          else None,
        runJmh = if (enableJmh) Some(true) else None,
      ),
      classPathOptions = ClassPathOptions(
        extraJars = extraJars.flatMap(_.split(File.pathSeparator).toSeq).filter(_.nonEmpty).map(os.Path(_, os.pwd)),
        extraRepositories = dependencies.repository.map(_.trim).filter(_.nonEmpty),
        extraDependencies = dependencies.dependency.map(_.trim).filter(_.nonEmpty).map { depStr =>
          DependencyParser.parse(depStr) match {
            case Left(err) => sys.error(s"Error parsing dependency '$depStr': $err")
            case Right(dep) => dep
          }
        }
      ),
      internal = InternalOptions(
        cache = Some(coursierCache),
        localRepository = LocalRepo.localRepo(directories.directories.localRepoDir)
      )
    )

  // This might download a JVM if --jvm … is passed or no system JVM is installed
  def bloopgunConfig(): bloopgun.BloopgunConfig =
    bloopgun.BloopgunConfig.default(() => Bloop.bloopClassPath(logging.logger)).copy(
      javaPath = buildOptions(false, None).javaCommand()
    )

  lazy val coursierCache = FileCache().withLogger(logging.logger.coursierLogger)

  def inputsOrExit(args: RemainingArgs, defaultInputs: Option[Inputs] = None): Inputs =
    Inputs(args.remaining, Os.pwd, directories.directories, defaultInputs = defaultInputs, stdinOpt = SharedOptions.readStdin(logger = logger), acceptFds = !Properties.isWin) match {
      case Left(message) =>
        System.err.println(message)
        sys.exit(1)
      case Right(i) =>
        val configFiles = config.map(os.Path(_, Os.pwd)).map(Inputs.ConfigFile(_))
        i.add(configFiles)
    }
}

object SharedOptions {
  implicit val parser = Parser[SharedOptions]
  implicit val help = Help[SharedOptions]

  def readStdin(in: InputStream = System.in, logger: Logger): Option[Array[Byte]] =
    if (in == null) {
      logger.debug("No stdin available")
      None
    } else {
      logger.debug("Reading stdin")
      val baos = new ByteArrayOutputStream
      val buf = Array.ofDim[Byte](16*1024)
      var read = -1
      while ({
        read = in.read(buf)
        read >= 0
      }) {
        if (read > 0)
          baos.write(buf, 0, read)
      }
      val result = baos.toByteArray
      logger.debug(s"Done reading stdin (${result.length} B)")
      Some(result)
    }

}
