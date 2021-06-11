package scala.cli.commands

import java.io.File

import caseapp._
import caseapp.core.help.Help
import dependency.{ScalaParameters, ScalaVersion}
import scala.build.{Build, Project}
import scala.build.internal.{CodeWrapper, Constants, CustomCodeClassWrapper, CustomCodeWrapper}
import scala.cli.internal.Util
import scala.scalanative.{build => sn}

final case class SharedOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    js: ScalaJsOptions = ScalaJsOptions(),
  @Recurse
    native: ScalaNativeOptions = ScalaNativeOptions(),

  @Group("Scala")
  @HelpMessage("Set Scala version")
  @ValueDescription("version")
  @Name("scala")
  @Name("S")
    scalaVersion: String = SharedOptions.defaultScalaVersion,
  @Group("Scala")
  @HelpMessage("Set Scala binary version")
  @ValueDescription("version")
  @Name("scalaBinary")
  @Name("scalaBin")
  @Name("B")
    scalaBinaryVersion: String = "",

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

  def computeScalaVersions(): ScalaVersions = {
    val sv =
      if (Util.isFullScalaVersion(scalaVersion)) scalaVersion
      else {
        import coursier._
        import coursier.core.Version
        import scala.concurrent.ExecutionContext.{global => ec}
        val modules = {
          def scala2 = mod"org.scala-lang:scala-library"
          // No unstable, that *ought* not to be a problem down-the-line…?
          def scala3 = mod"org.scala-lang:scala3-library_3"
          if (scalaVersion == "2" || scalaVersion.startsWith("2.")) Seq(scala2)
          else if (scalaVersion == "3" || scalaVersion.startsWith("3.")) Seq(scala3)
          else Seq(scala2, scala3)
        }
        def isStable(v: String): Boolean =
          !v.endsWith("-NIGHTLY") && !v.contains("-RC")
        def moduleVersions(mod: Module): Seq[String] = {
          val res = Versions()
            .withModule(mod)
            .result()
            .unsafeRun()(ec)
          res.versions.available.filter(isStable)
        }
        val allVersions = modules.flatMap(moduleVersions).distinct
        val prefix = if (scalaVersion.endsWith(".")) scalaVersion else scalaVersion + "."
        val matchingVersions = allVersions.filter(_.startsWith(prefix))
        if (matchingVersions.isEmpty)
          sys.error(s"Cannot find matching Scala version for '$scalaVersion'")
        else
          matchingVersions.map(Version(_)).max.repr
      }
    val sbv =
      if (scalaBinaryVersion.isEmpty) ScalaVersion.binary(sv)
      else scalaBinaryVersion
    ScalaVersions(sv, sbv)
  }

  def scalaParams(scalaVersions: ScalaVersions) =
    ScalaParameters(
      scalaVersions.version,
      scalaVersions.binaryVersion,
      if (js.js) Some("sjs" + ScalaVersion.jsBinary(js.finalVersion))
      else if (native.native) Some("native" + ScalaVersion.nativeBinary(native.finalVersion))
      else None
    )

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

  def buildOptions(scalaVersions: ScalaVersions, enableJmh: Option[Boolean], jmhVersion: Option[String]): Build.Options =
    Build.Options(
      scalaVersion = scalaVersions.version,
      scalaBinaryVersion = scalaVersions.binaryVersion,
      codeWrapper = codeWrapper,
      scalaJsOptions = js.buildOptions(scalaVersions),
      scalaNativeOptions = native.buildOptions(scalaVersions),
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
      extraJars = extraJars.flatMap(_.split(File.pathSeparator).toSeq).filter(_.nonEmpty).map(os.Path(_, os.pwd))
    )
}

object SharedOptions {

  def defaultScalaVersion: String =
    scala.util.Properties.versionNumberString

  implicit val parser = Parser[SharedOptions]
  implicit val help = Help[SharedOptions]
}
