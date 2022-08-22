package scala.build.options

import com.github.plokhotnyuk.jsoniter_scala.core.*
import coursier.cache.{ArchiveCache, FileCache}
import coursier.core.Version
import coursier.util.{Artifact, Task}
import dependency.*

import java.io.File
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import scala.build.EitherCps.{either, value}
import scala.build.actionable.{ActionableDiagnostic, ActionablePreprocessor}
import scala.build.errors.*
import scala.build.interactive.Interactive
import scala.build.interactive.Interactive.*
import scala.build.internal.Constants.*
import scala.build.internal.CsLoggerUtil.*
import scala.build.internal.Regexes.scala3NightlyNicknameRegex
import scala.build.internal.{Constants, OsLibc, StableScalaVersion}
import scala.build.options.validation.BuildOptionsRule
import scala.build.{Artifacts, Logger, Os, Position, Positioned}
import scala.concurrent.duration.*
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
  notForBloopOptions: PostBuildOptions = PostBuildOptions()
) {

  import BuildOptions.JavaHomeInfo

  lazy val platform: Positioned[Platform] =
    scalaOptions.platform.getOrElse(Positioned(List(Position.Custom("DEFAULT")), Platform.JVM))

  lazy val projectParams: Either[BuildException, Seq[String]] = either {
    value(scalaParams) match {
      case Some(scalaParams0) =>
        val platform0 = platform.value match {
          case Platform.JVM    => "JVM"
          case Platform.JS     => "Scala.js"
          case Platform.Native => "Scala Native"
        }
        Seq(s"Scala ${scalaParams0.scalaVersion}", platform0)
      case None =>
        Seq("Java")
    }
  }

  lazy val scalaVersionIsExotic: Boolean = scalaParams.toOption.flatten.exists { scalaParameters =>
    scalaParameters.scalaVersion.startsWith("2") && scalaParameters.scalaVersion.exists(_.isLetter)
  }

  def addRunnerDependency: Option[Boolean] =
    internalDependencies.addRunnerDependencyOpt
      .orElse {
        if (platform.value == Platform.JVM && !scalaVersionIsExotic) None
        else Some(false)
      }

  private def scalaLibraryDependencies: Either[BuildException, Seq[AnyDependency]] = either {
    value(scalaParams).toSeq.flatMap { scalaParams0 =>
      if (platform.value != Platform.Native && scalaOptions.addScalaLibrary.getOrElse(true))
        Seq(
          if (scalaParams0.scalaVersion.startsWith("3."))
            dep"org.scala-lang::scala3-library::${scalaParams0.scalaVersion}"
          else
            dep"org.scala-lang:scala-library:${scalaParams0.scalaVersion}"
        )
      else Nil
    }
  }

  private def maybeJsDependencies: Either[BuildException, Seq[AnyDependency]] = either {
    if (platform.value == Platform.JS)
      value(scalaParams).toSeq.flatMap { scalaParams0 =>
        scalaJsOptions.jsDependencies(scalaParams0.scalaVersion)
      }
    else Nil
  }
  private def maybeNativeDependencies: Either[BuildException, Seq[AnyDependency]] = either {
    if (platform.value == Platform.Native)
      value(scalaParams).toSeq.flatMap { scalaParams0 =>
        scalaNativeOptions.nativeDependencies(scalaParams0.scalaVersion)
      }
    else Nil
  }
  private def dependencies: Either[BuildException, Seq[Positioned[AnyDependency]]] = either {
    value(maybeJsDependencies).map(Positioned.none(_)) ++
      value(maybeNativeDependencies).map(Positioned.none(_)) ++
      value(scalaLibraryDependencies).map(Positioned.none(_)) ++
      classPathOptions.extraDependencies.toSeq
  }

  private def semanticDbPlugins: Either[BuildException, Seq[AnyDependency]] = either {
    val generateSemDbs = scalaOptions.generateSemanticDbs.getOrElse(false) &&
      value(scalaParams).exists(_.scalaVersion.startsWith("2."))
    if (generateSemDbs)
      Seq(
        dep"$semanticDbPluginOrganization:::$semanticDbPluginModuleName:$semanticDbPluginVersion"
      )
    else
      Nil
  }

  private def maybeJsCompilerPlugins: Either[BuildException, Seq[AnyDependency]] = either {
    if (platform.value == Platform.JS)
      value(scalaParams).toSeq.flatMap { scalaParams0 =>
        scalaJsOptions.compilerPlugins(scalaParams0.scalaVersion)
      }
    else Nil
  }
  private def maybeNativeCompilerPlugins: Seq[AnyDependency] =
    if (platform.value == Platform.Native) scalaNativeOptions.compilerPlugins
    else Nil
  def compilerPlugins: Either[BuildException, Seq[Positioned[AnyDependency]]] = either {
    value(maybeJsCompilerPlugins).map(Positioned.none) ++
      maybeNativeCompilerPlugins.map(Positioned.none) ++
      value(semanticDbPlugins).map(Positioned.none) ++
      scalaOptions.compilerPlugins
  }

  private def semanticDbJavacPlugins: Either[BuildException, Seq[AnyDependency]] = either {
    val generateSemDbs = scalaOptions.generateSemanticDbs.getOrElse(false)
    if (generateSemDbs)
      Seq(
        dep"$semanticDbJavacPluginOrganization:$semanticDbJavacPluginModuleName:$semanticDbJavacPluginVersion"
      )
    else
      Nil
  }

  def javacPluginDependencies: Either[BuildException, Seq[Positioned[AnyDependency]]] = either {
    value(semanticDbJavacPlugins).map(Positioned.none(_)) ++
      javaOptions.javacPluginDependencies
  }

  def allExtraJars: Seq[os.Path] =
    classPathOptions.extraClassPath
  def allExtraCompileOnlyJars: Seq[os.Path] =
    classPathOptions.extraCompileOnlyJars
  def allExtraSourceJars: Seq[os.Path] =
    classPathOptions.extraSourceJars

  private def addJvmTestRunner: Boolean =
    platform.value == Platform.JVM &&
    internalDependencies.addTestRunnerDependency
  private def addJsTestBridge: Option[String] =
    if (platform.value == Platform.JS && internalDependencies.addTestRunnerDependency)
      Some(scalaJsOptions.finalVersion)
    else None
  private def addNativeTestInterface: Option[String] = {
    val doAdd =
      platform.value == Platform.Native &&
      internalDependencies.addTestRunnerDependency &&
      Version("0.4.3").compareTo(Version(scalaNativeOptions.finalVersion)) <= 0
    if (doAdd) Some(scalaNativeOptions.finalVersion)
    else None
  }

  lazy val finalCache: FileCache[Task] = internal.cache.getOrElse(FileCache())
  // This might download a JVM if --jvm … is passed or no system JVM is installed

  lazy val archiveCache: ArchiveCache[Task] = ArchiveCache().withCache(finalCache)

  private lazy val javaCommand0: Positioned[JavaHomeInfo] =
    javaHomeLocation().map(JavaHomeInfo(_))

  def javaHomeLocationOpt(): Option[Positioned[os.Path]] =
    javaOptions.javaHomeLocationOpt(archiveCache, finalCache, internal.verbosityOrDefault)

  def javaHomeLocation(): Positioned[os.Path] =
    javaOptions.javaHomeLocation(archiveCache, finalCache, internal.verbosityOrDefault)

  // used when downloading fails
  private def defaultStableScalaVersions =
    Seq(defaultScala212Version, defaultScala213Version, defaultScalaVersion)

  private def latestSupportedStableScalaVersion(scalaCliVersion: String): Seq[Version] = {

    val msg =
      if (internal.verbosityOrDefault > 0)
        "Getting list of Scala CLI-supported Scala versions"
      else ""
    val cache                     = finalCache.withMessage(msg)
    val supportedScalaVersionsUrl = scalaOptions.scalaVersionsUrl
    val ignoreErrors              = scalaOptions.ignoreSupportedScalaVersionsErrors.getOrElse(true)

    val task = {
      val art = Artifact(supportedScalaVersionsUrl).withChanging(true)
      cache.file(art).run.flatMap {
        case Left(e) => Task.fail(e)
        case Right(f) =>
          Task.delay {
            val content = os.read.bytes(os.Path(f, Os.pwd))
            readFromArray(content)(StableScalaVersion.seqCodec)
          }
      }
    }

    val launchersTask = cache.logger.using(task)

    //  If an error occurred while downloading stable versions,
    //  it uses stable scala versions from Deps.sc
    val supportedScalaVersions =
      launchersTask.attempt.unsafeRun()(cache.ec) match {
        case Left(e) =>
          if (ignoreErrors)
            // FIXME Log the exception
            defaultStableScalaVersions
          else
            // wrapped in an exception so that the current stack trace appears in the exception
            throw new Exception(e)
        case Right(versions) =>
          versions.find(_.scalaCliVersion == scalaCliVersion)
            .orElse {
              val retainedCliVersion =
                if (scalaCliVersion.endsWith("-SNAPSHOT"))
                  if (scalaCliVersion.contains("-g"))
                    // version like 0.1.7-30-g51330f19d-SNAPSHOT
                    scalaCliVersion.takeWhile(_ != '-').split('.') match {
                      case Array(maj, min, patch) if patch.nonEmpty && patch.forall(_.isDigit) =>
                        val patch0 = patch.toInt + 1
                        s"$maj.$min.$patch0"
                      case _ =>
                        // shouldn't happen
                        scalaCliVersion
                    }
                  else
                    // version like 0.1.8-SNAPSHOT
                    scalaCliVersion.takeWhile(_ != '-')
                else
                  scalaCliVersion
              val retainedCliVersion0 = Version(retainedCliVersion)
              versions
                .filter(_.scalaCliVersion0.compareTo(retainedCliVersion0) <= 0)
                .maxByOption(_.scalaCliVersion0)
            }
            .map(_.supportedScalaVersions)
            .getOrElse {
              // FIXME Log that: logger.debug(s"Couldn't find Scala CLI version $scalaCliVersion in $versions")
              defaultStableScalaVersions
            }
      }

    supportedScalaVersions
      .map(Version(_))
      .sorted
      .reverse
  }

  def javaHome(): Positioned[JavaHomeInfo] = javaCommand0

  lazy val javaHomeManager =
    javaOptions.javaHomeManager(archiveCache, finalCache, internal.verbosityOrDefault)

  private val scala2NightlyRepo = Seq(coursier.Repositories.scalaIntegration.root)

  def finalRepositories: Seq[String] = {

    val nightlyRepos =
      if (
        scalaParams.toOption.flatten.exists(params =>
          ScalaVersionUtil.isScala2Nightly(params.scalaVersion)
        )
      )
        scala2NightlyRepo
      else
        Nil

    nightlyRepos ++
      classPathOptions.extraRepositories ++
      internal.localRepository.toSeq
  }

  lazy val scalaParams: Either[BuildException, Option[ScalaParameters]] =
    if (System.getenv("CI") == null)
      computeScalaParams(Constants.version, finalCache).orElse(
        // when the passed scala version is missed in the cache, we always force a cache refresh
        // https://github.com/VirtusLab/scala-cli/issues/1090
        computeScalaParams(Constants.version, finalCache.withTtl(0.seconds))
      )
    else
      computeScalaParams(Constants.version, finalCache.withTtl(0.seconds))

  private[build] def computeScalaParams(
    scalaCliVersion: String,
    cache: FileCache[Task] = finalCache
  ): Either[BuildException, Option[ScalaParameters]] = either {

    lazy val maxSupportedStableScalaVersions = latestSupportedStableScalaVersion(scalaCliVersion)
    lazy val latestSupportedStableVersions   = maxSupportedStableScalaVersions.map(_.repr)

    val svOpt: Option[String] = scalaOptions.scalaVersion match {
      case Some(MaybeScalaVersion(None)) =>
        None
      case Some(MaybeScalaVersion(Some(svInput))) =>
        val sv = value {
          svInput match {
            case "3.nightly" =>
              ScalaVersionUtil.GetNightly.scala3(cache)
            case scala3NightlyNicknameRegex(threeSubBinaryNum) =>
              ScalaVersionUtil.GetNightly.scala3X(
                threeSubBinaryNum,
                cache,
                latestSupportedStableVersions
              )
            case "2.nightly" | "2.13.nightly" =>
              ScalaVersionUtil.GetNightly.scala2("2.13", cache)
            case "2.12.nightly" =>
              ScalaVersionUtil.GetNightly.scala2("2.12", cache)
            case versionString if ScalaVersionUtil.isScala3Nightly(versionString) =>
              ScalaVersionUtil.CheckNightly.scala3(
                versionString,
                cache,
                latestSupportedStableVersions
              )
                .map(_ => versionString)
            case versionString if ScalaVersionUtil.isScala2Nightly(versionString) =>
              ScalaVersionUtil.CheckNightly.scala2(
                versionString,
                cache,
                latestSupportedStableVersions
              )
                .map(_ => versionString)
            case versionString if versionString.exists(_.isLetter) =>
              ScalaVersionUtil.validateNonStable(
                versionString,
                cache,
                latestSupportedStableVersions
              )
            case versionString =>
              ScalaVersionUtil.validateStable(
                versionString,
                cache,
                latestSupportedStableVersions,
                maxSupportedStableScalaVersions
              )
          }
        }
        Some(sv)

      case None =>
        val allStableVersions = ScalaVersionUtil.allMatchingVersions(None, finalCache)
          .filter(ScalaVersionUtil.isStable)
        val sv = value {
          ScalaVersionUtil.default(
            allStableVersions,
            latestSupportedStableVersions,
            maxSupportedStableScalaVersions
          )
        }
        Some(sv)
    }

    svOpt match {
      case Some(scalaVersion) =>
        val scalaBinaryVersion = scalaOptions.scalaBinaryVersion.getOrElse {
          ScalaVersion.binary(scalaVersion)
        }

        val maybePlatformSuffix = platform.value match {
          case Platform.JVM    => None
          case Platform.JS     => Some(scalaJsOptions.platformSuffix)
          case Platform.Native => Some(scalaNativeOptions.platformSuffix)
        }

        Some(ScalaParameters(scalaVersion, scalaBinaryVersion, maybePlatformSuffix))
      case None =>
        None
    }
  }

  def artifacts(
    logger: Logger,
    scope: Scope,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e)
  ): Either[BuildException, Artifacts] = either {
    val isTests = scope == Scope.Test
    val scalaArtifactsParamsOpt = value(scalaParams) match {
      case Some(scalaParams0) =>
        val params = Artifacts.ScalaArtifactsParams(
          params = scalaParams0,
          compilerPlugins = value(compilerPlugins),
          addJsTestBridge = addJsTestBridge.filter(_ => isTests),
          addNativeTestInterface = addNativeTestInterface.filter(_ => isTests),
          scalaJsCliVersion =
            if (platform.value == Platform.JS) Some(scalaJsCliVersion) else None,
          scalaNativeCliVersion =
            if (platform.value == Platform.Native) Some(scalaNativeOptions.finalVersion) else None
        )
        Some(params)
      case None =>
        None
    }
    val addRunnerDependency0 = addRunnerDependency.orElse {
      if (scalaArtifactsParamsOpt.isDefined) None
      else Some(false) // no runner in pure Java mode
    }
    val maybeArtifacts = Artifacts(
      scalaArtifactsParamsOpt,
      javacPluginDependencies = value(javacPluginDependencies),
      extraJavacPlugins = javaOptions.javacPlugins.map(_.value),
      dependencies = value(dependencies),
      extraClassPath = allExtraJars,
      extraCompileOnlyJars = allExtraCompileOnlyJars,
      extraSourceJars = allExtraSourceJars,
      fetchSources = classPathOptions.fetchSources.getOrElse(false),
      addStubs = internalDependencies.addStubsDependency,
      addJvmRunner = addRunnerDependency0,
      addJvmTestRunner = isTests && addJvmTestRunner,
      addJmhDependencies = jmhOptions.addJmhDependencies,
      extraRepositories = finalRepositories,
      keepResolution = internal.keepResolution,
      cache = finalCache,
      logger = logger,
      maybeRecoverOnError = maybeRecoverOnError
    )
    value(maybeArtifacts)
  }

  private def allCrossScalaVersionOptions: Seq[BuildOptions] = {
    val scalaOptions0 = scalaOptions.normalize
    val sortedExtraScalaVersions = scalaOptions0
      .extraScalaVersions
      .toVector
      .map(coursier.core.Version(_))
      .sorted
      .map(_.repr)
      .reverse
    this +: sortedExtraScalaVersions.map { sv =>
      copy(
        scalaOptions = scalaOptions0.copy(
          scalaVersion = Some(MaybeScalaVersion(sv)),
          extraScalaVersions = Set.empty
        )
      )
    }
  }

  private def allCrossScalaPlatformOptions: Seq[BuildOptions] = {
    val scalaOptions0 = scalaOptions.normalize
    val sortedExtraPlatforms = scalaOptions0
      .extraPlatforms
      .toVector
    this +: sortedExtraPlatforms.map { case (pf, pos) =>
      copy(
        scalaOptions = scalaOptions0.copy(
          platform = Some(Positioned(pos.positions, pf)),
          extraPlatforms = Map.empty
        )
      )
    }
  }

  def crossOptions: Seq[BuildOptions] = {
    val allOptions = for {
      svOpt   <- allCrossScalaVersionOptions
      svPfOpt <- svOpt.allCrossScalaPlatformOptions
    } yield svPfOpt
    allOptions.drop(1) // First one if basically 'this', dropping it
  }

  private def clearJsOptions: BuildOptions =
    copy(scalaJsOptions = ScalaJsOptions())
  private def clearNativeOptions: BuildOptions =
    copy(scalaNativeOptions = ScalaNativeOptions())
  private def normalize: BuildOptions = {
    var opt = this

    if (platform.value != Platform.JS)
      opt = opt.clearJsOptions
    if (platform.value != Platform.Native)
      opt = opt.clearNativeOptions

    opt.copy(
      scalaOptions = opt.scalaOptions.normalize
    )
  }

  lazy val hash: Option[String] = {
    val md = MessageDigest.getInstance("SHA-1")

    var hasAnyOverride = false

    BuildOptions.hasHashData.add(
      "",
      normalize,
      s => {
        val bytes = s.getBytes(StandardCharsets.UTF_8)
        if (bytes.nonEmpty) {
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

  def validate: Seq[Diagnostic] = BuildOptionsRule.validateAll(this)

  def logActionableDiagnostics(logger: Logger): Unit = {
    val actionableDiagnostics = ActionablePreprocessor.generateActionableDiagnostics(this)
    actionableDiagnostics match {
      case Left(e) =>
        logger.debug(e)
      case Right(diagnostics) =>
        logger.log(diagnostics)
    }
  }

  lazy val interactive: Interactive = internal.interactive.getOrElse(() => InteractiveNop)()
}

object BuildOptions {

  final case class CrossKey(
    scalaVersion: String,
    platform: Platform
  )

  final case class JavaHomeInfo(
    javaHome: os.Path,
    javaCommand: String,
    version: Int
  ) {
    def envUpdates(currentEnv: Map[String, String]): Map[String, String] = {
      // On Windows, AFAIK, env vars are "case-insensitive but case-preserving".
      // If PATH was defined as "Path", we need to update "Path", not "PATH".
      // Same for JAVA_HOME.
      def keyFor(name: String) =
        if (Properties.isWin)
          currentEnv.keys.find(_.equalsIgnoreCase(name)).getOrElse(name)
        else
          name
      val javaHomeKey = keyFor("JAVA_HOME")
      val pathKey     = keyFor("PATH")
      val updatedPath = {
        val valueOpt = currentEnv.get(pathKey)
        val entry    = (javaHome / "bin").toString
        valueOpt.fold(entry)(entry + File.pathSeparator + _)
      }
      Map(
        javaHomeKey -> javaHome.toString,
        pathKey     -> updatedPath
      )
    }
  }

  object JavaHomeInfo {
    def apply(javaHome: os.Path): JavaHomeInfo = {
      val ext         = if (Properties.isWin) ".exe" else ""
      val javaCmd     = (javaHome / "bin" / s"java$ext").toString
      val javaVersion = OsLibc.javaVersion(javaCmd)
      JavaHomeInfo(javaHome, javaCmd, javaVersion)
    }
  }

  implicit val hasHashData: HasHashData[BuildOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[BuildOptions]     = ConfigMonoid.derive
}
