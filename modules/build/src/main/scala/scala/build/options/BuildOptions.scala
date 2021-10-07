package scala.build.options

import coursier.cache.FileCache
import coursier.jvm.{JavaHome, JvmCache, JvmIndex}
import dependency._

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest

import scala.build.EitherCps.{either, value}
import scala.build.errors.{BuildException, InvalidBinaryScalaVersionError}
import scala.build.internal.Constants._
import scala.build.internal.{Constants, OsLibc, Util}
import scala.build.{Artifacts, Logger, Os}
import scala.util.Properties

final case class BuildOptions(
  scalaOptions: ScalaOptions = ScalaOptions(),
  scalaJsOptions: ScalaJsOptions = ScalaJsOptions(),
  scalaNativeOptions: ScalaNativeOptions = ScalaNativeOptions(),
  internalDependencies: InternalDependenciesOptions = InternalDependenciesOptions(),
  javaOptions: JavaOptions = JavaOptions(),
  jmhOptions: JmhOptions = JmhOptions(),
  classPathOptions: ClassPathOptions = ClassPathOptions(),
  scriptOptions: ScriptOptions = ScriptOptions(),
  internal: InternalOptions = InternalOptions(),
  mainClass: Option[String] = None,
  testOptions: TestOptions = TestOptions(),
  packageOptions: PackageOptions = PackageOptions(),
  replOptions: ReplOptions = ReplOptions()
) {

  lazy val projectParams: Either[BuildException, Seq[String]] = either {
    val platform =
      if (scalaJsOptions.enable) "Scala.JS"
      else if (scalaNativeOptions.enable) "Scala Native"
      else "JVM"
    Seq(s"Scala ${value(scalaParams).scalaVersion}", platform)
  }

  def addRunnerDependency: Option[Boolean] =
    internalDependencies.addRunnerDependencyOpt
      .orElse(if (scalaJsOptions.enable || scalaNativeOptions.enable) Some(false) else None)

  private def scalaLibraryDependencies: Either[BuildException, Seq[AnyDependency]] = either {
    if (scalaOptions.addScalaLibrary.getOrElse(true)) {
      val scalaParams0 = value(scalaParams)
      val lib =
        if (scalaParams0.scalaVersion.startsWith("3."))
          dep"org.scala-lang::scala3-library::${scalaParams0.scalaVersion}"
        else
          dep"org.scala-lang:scala-library:${scalaParams0.scalaVersion}"
      Seq(lib)
    }
    else Nil
  }

  private def dependencies: Either[BuildException, Seq[AnyDependency]] = either {
    scalaJsOptions.jsDependencies(value(scalaParams).scalaVersion) ++
      scalaNativeOptions.nativeDependencies ++
      value(scalaLibraryDependencies) ++
      classPathOptions.extraDependencies
  }

  private def semanticDbPlugins: Either[BuildException, Seq[AnyDependency]] = either {
    val generateSemDbs = scalaOptions.generateSemanticDbs.getOrElse(false) &&
      value(scalaParams).scalaVersion.startsWith("2.")
    if (generateSemDbs)
      Seq(
        dep"$semanticDbPluginOrganization:::$semanticDbPluginModuleName:$semanticDbPluginVersion"
      )
    else
      Nil
  }

  def compilerPlugins: Either[BuildException, Seq[AnyDependency]] = either {
    scalaJsOptions.compilerPlugins(value(scalaParams).scalaVersion) ++
      scalaNativeOptions.compilerPlugins ++
      value(semanticDbPlugins) ++
      scalaOptions.compilerPlugins
  }

  def allExtraJars: Seq[Path] =
    classPathOptions.extraClassPath.map(_.toNIO)
  def allExtraCompileOnlyJars: Seq[Path] =
    classPathOptions.extraCompileOnlyJars.map(_.toNIO)
  def allExtraSourceJars: Seq[Path] =
    classPathOptions.extraSourceJars.map(_.toNIO)

  private def addJvmTestRunner: Boolean =
    !scalaJsOptions.enable &&
    !scalaNativeOptions.enable &&
    internalDependencies.addTestRunnerDependency
  private def addJsTestBridge: Option[String] =
    if (internalDependencies.addTestRunnerDependency) Some(scalaJsOptions.finalVersion)
    else None

  private lazy val finalCache = internal.cache.getOrElse(FileCache())
  // This might download a JVM if --jvm … is passed or no system JVM is installed
  private lazy val javaCommand0: String = {
    val javaHome = javaHomeLocation()
    val ext      = if (Properties.isWin) ".exe" else ""
    (javaHome / "bin" / s"java$ext").toString
  }

  def javaHomeLocationOpt(): Option[os.Path] =
    javaOptions.javaHomeOpt
      .orElse {
        if (javaOptions.jvmIdOpt.isEmpty) sys.props.get("java.home").map(os.Path(_, Os.pwd))
        else None
      }
      .orElse {
        javaOptions.jvmIdOpt.map { jvmId =>
          implicit val ec = finalCache.ec
          finalCache.logger.use {
            val path = javaHomeManager.get(jvmId).unsafeRun()
            os.Path(path)
          }
        }
      }

  def javaHomeLocation(): os.Path =
    javaHomeLocationOpt().getOrElse {
      implicit val ec = finalCache.ec
      finalCache.logger.use {
        val path = javaHomeManager.get(OsLibc.defaultJvm).unsafeRun()
        os.Path(path)
      }
    }

  def javaCommand(): String = javaCommand0

  private lazy val javaHomeManager = {
    val indexUrl  = javaOptions.jvmIndexOpt.getOrElse(JvmIndex.coursierIndexUrl)
    val indexTask = JvmIndex.load(finalCache, indexUrl)
    val jvmCache = JvmCache()
      .withIndex(indexTask)
      .withCache(finalCache)
      .withOs(javaOptions.jvmIndexOs.getOrElse(OsLibc.jvmIndexOs))
      .withArchitecture(javaOptions.jvmIndexArch.getOrElse(JvmIndex.defaultArchitecture()))
    JavaHome().withCache(jvmCache)
  }

  private def finalRepositories: Seq[String] =
    classPathOptions.extraRepositories ++ internal.localRepository.toSeq

  private def computeScalaVersions(
    scalaVersion: Option[String],
    scalaBinaryVersion: Option[String]
  ): Either[BuildException, (String, String)] = either {
    import coursier.core.Version
    lazy val allVersions = {
      import coursier._
      import scala.concurrent.ExecutionContext.{global => ec}
      val modules = {
        def scala2 = mod"org.scala-lang:scala-library"
        // No unstable, that *ought* not to be a problem down-the-line…?
        def scala3 = mod"org.scala-lang:scala3-library_3"
        if (scalaVersion.contains("2") || scalaVersion.exists(_.startsWith("2."))) Seq(scala2)
        else if (scalaVersion.contains("3") || scalaVersion.exists(_.startsWith("3."))) Seq(scala3)
        else Seq(scala2, scala3)
      }
      def isStable(v: String): Boolean =
        !v.endsWith("-NIGHTLY") && !v.contains("-RC")
      def moduleVersions(mod: Module): Seq[String] = {
        val res = finalCache.logger.use {
          Versions()
            .withModule(mod)
            .result()
            .unsafeRun()(ec)
        }
        res.versions.available.filter(isStable)
      }
      modules.flatMap(moduleVersions).distinct
    }
    val maybeSv = scalaVersion match {
      case None => Right(Constants.defaultScalaVersion)
      case Some(sv0) =>
        if (Util.isFullScalaVersion(sv0)) Right(sv0)
        else {
          val prefix           = if (sv0.endsWith(".")) sv0 else sv0 + "."
          val matchingVersions = allVersions.filter(_.startsWith(prefix))
          if (matchingVersions.isEmpty)
            Left(new InvalidBinaryScalaVersionError(sv0))
          else
            Right(matchingVersions.map(Version(_)).max.repr)
        }
    }
    val sv  = value(maybeSv)
    val sbv = scalaBinaryVersion.getOrElse(ScalaVersion.binary(sv))
    (sv, sbv)
  }

  lazy val scalaParams: Either[BuildException, ScalaParameters] = either {
    val (scalaVersion, scalaBinaryVersion) =
      value(computeScalaVersions(scalaOptions.scalaVersion, scalaOptions.scalaBinaryVersion))
    val maybePlatformSuffix =
      scalaJsOptions.platformSuffix
        .orElse(scalaNativeOptions.platformSuffix)
    ScalaParameters(scalaVersion, scalaBinaryVersion, maybePlatformSuffix)
  }

  def artifacts(logger: Logger): Either[BuildException, Artifacts] = either {
    val maybeArtifacts = Artifacts(
      params = value(scalaParams),
      compilerPlugins = value(compilerPlugins),
      dependencies = value(dependencies),
      extraClassPath = allExtraJars,
      extraCompileOnlyJars = allExtraCompileOnlyJars,
      extraSourceJars = allExtraSourceJars,
      fetchSources = classPathOptions.fetchSources.getOrElse(false),
      addStubs = internalDependencies.addStubsDependency,
      addJvmRunner = addRunnerDependency,
      addJvmTestRunner = addJvmTestRunner,
      addJsTestBridge = addJsTestBridge,
      addJmhDependencies = jmhOptions.addJmhDependencies,
      extraRepositories = finalRepositories,
      logger = logger
    )
    value(maybeArtifacts)
  }

  // FIXME We'll probably need more refined rules if we start to support extra Scala.JS or Scala Native specific types
  def packageTypeOpt: Option[PackageType] =
    if (packageOptions.isDockerEnabled) Some(PackageType.Docker)
    else if (scalaJsOptions.enable) Some(PackageType.Js)
    else if (scalaNativeOptions.enable) Some(PackageType.Native)
    else packageOptions.packageTypeOpt

  def crossOptions: Seq[BuildOptions] = {
    val extraScalaVersions =
      scalaOptions.extraScalaVersions.filter(v => !scalaOptions.scalaVersion.contains(v))
    val sortedExtraScalaVersions =
      extraScalaVersions.toVector.map(coursier.core.Version(_)).sorted.map(_.repr).reverse
    sortedExtraScalaVersions.map { sv =>
      copy(
        scalaOptions = scalaOptions.copy(
          scalaVersion = Some(sv),
          extraScalaVersions = Set.empty
        )
      )
    }
  }

  lazy val hash: Option[String] = {
    val md = MessageDigest.getInstance("SHA-1")

    var hasAnyOverride = false

    BuildOptions.hasHashData.add(
      "",
      this,
      s => {
        val bytes = s.getBytes(StandardCharsets.UTF_8)
        if (bytes.length > 0) {
          hasAnyOverride = true
          md.update(bytes)
        }
      }
    )

    if (hasAnyOverride) {
      val digest        = md.digest()
      val calculatedSum = new BigInteger(1, digest)
      val hash          = String.format(s"%040x", calculatedSum).take(10)
      Some(hash)
    }
    else None
  }

  def orElse(other: BuildOptions): BuildOptions =
    BuildOptions.monoid.orElse(this, other)
}

object BuildOptions {
  implicit val hasHashData: HasHashData[BuildOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[BuildOptions]     = ConfigMonoid.derive
}
