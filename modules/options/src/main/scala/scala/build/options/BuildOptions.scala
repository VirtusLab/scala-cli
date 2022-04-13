package scala.build.options

import com.github.plokhotnyuk.jsoniter_scala.core._
import coursier.cache.{ArchiveCache, FileCache}
import coursier.core.{Version, Versions => CoreVersions}
import coursier.jvm.{JavaHome, JvmCache, JvmIndex}
import coursier.util.{Artifact, Task}
import coursier.{Module, Versions}
import dependency._

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import scala.build.CoursierUtils._
import scala.build.EitherCps.{either, value}
import scala.build.errors._
import scala.build.internal.Constants._
import scala.build.internal.CsLoggerUtil._
import scala.build.internal.Regexes.{scala2NightlyRegex, scala3NightlyNicknameRegex}
import scala.build.internal.{OsLibc, StableScalaVersion, Util}
import scala.build.options.validation.BuildOptionsRule
import scala.build.{Artifacts, Logger, Os, Position, Positioned}
import scala.util.control.NonFatal

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
    val platform0 = platform.value match {
      case Platform.JVM    => "JVM"
      case Platform.JS     => "Scala.js"
      case Platform.Native => "Scala Native"
    }
    Seq(s"Scala ${value(scalaParams).scalaVersion}", platform0)
  }

  lazy val scalaVersionIsExotic = scalaParams.exists { scalaParameters =>
    scalaParameters.scalaVersion.startsWith("2") && scalaParameters.scalaVersion.exists(_.isLetter)
  }

  def addRunnerDependency: Option[Boolean] =
    internalDependencies.addRunnerDependencyOpt
      .orElse {
        if (platform.value == Platform.JVM && !scalaVersionIsExotic) None
        else Some(false)
      }

  private def scalaLibraryDependencies: Either[BuildException, Seq[AnyDependency]] = either {
    if (platform.value != Platform.Native && scalaOptions.addScalaLibrary.getOrElse(true)) {
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

  private def maybeJsDependencies: Either[BuildException, Seq[AnyDependency]] = either {
    if (platform.value == Platform.JS)
      scalaJsOptions.jsDependencies(value(scalaParams).scalaVersion)
    else Nil
  }
  private def maybeNativeDependencies: Either[BuildException, Seq[AnyDependency]] = either {
    if (platform.value == Platform.Native)
      scalaNativeOptions.nativeDependencies(value(scalaParams).scalaVersion)
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
      value(scalaParams).scalaVersion.startsWith("2.")
    if (generateSemDbs)
      Seq(
        dep"$semanticDbPluginOrganization:::$semanticDbPluginModuleName:$semanticDbPluginVersion"
      )
    else
      Nil
  }

  private def maybeJsCompilerPlugins: Either[BuildException, Seq[AnyDependency]] = either {
    if (platform.value == Platform.JS)
      scalaJsOptions.compilerPlugins(value(scalaParams).scalaVersion)
    else Nil
  }
  private def maybeNativeCompilerPlugins: Seq[AnyDependency] =
    if (platform.value == Platform.Native) scalaNativeOptions.compilerPlugins
    else Nil
  def compilerPlugins: Either[BuildException, Seq[Positioned[AnyDependency]]] = either {
    value(maybeJsCompilerPlugins).map(Positioned.none(_)) ++
      maybeNativeCompilerPlugins.map(Positioned.none(_)) ++
      value(semanticDbPlugins).map(Positioned.none(_)) ++
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

  lazy val finalCache = internal.cache.getOrElse(FileCache())
  // This might download a JVM if --jvm … is passed or no system JVM is installed

  lazy val archiveCache = ArchiveCache().withCache(finalCache)

  private lazy val javaCommand0: Positioned[JavaHomeInfo] = javaHomeLocation().map { javaHome =>
    val (javaVersion, javaCmd) = OsLibc.javaHomeVersion(javaHome)
    JavaHomeInfo(javaHome, javaCmd, javaVersion)
  }

  private def jvmIndexOs = javaOptions.jvmIndexOs.getOrElse(OsLibc.jvmIndexOs)

  def javaHomeLocationOpt(): Option[Positioned[os.Path]] =
    javaOptions.javaHomeOpt
      .orElse {
        if (javaOptions.jvmIdOpt.isEmpty)
          sys.props.get("java.home").map(p =>
            Positioned(Position.Custom("java.home prop"), os.Path(p, Os.pwd))
          )
        else None
      }
      .orElse {
        javaOptions.jvmIdOpt.map { jvmId =>
          implicit val ec = finalCache.ec
          finalCache.logger.use {
            val enforceLiberica =
              jvmIndexOs == "linux-musl" && jvmId.forall(c => c.isDigit || c == '.' || c == '-')
            val jvmId0 =
              if (enforceLiberica)
                s"liberica:$jvmId" // FIXME Workaround, until this is automatically handled by coursier-jvm
              else
                jvmId
            val javaHomeManager0 = javaHomeManager
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

  def javaHomeLocation(): Positioned[os.Path] =
    javaHomeLocationOpt().getOrElse {
      val jvmId = OsLibc.defaultJvm(jvmIndexOs)
      val javaHomeManager0 = javaHomeManager
        .withMessage(s"Downloading JVM $jvmId")
      implicit val ec = finalCache.ec
      finalCache.logger.use {
        val path =
          try javaHomeManager0.get(jvmId).unsafeRun()
          catch {
            case NonFatal(e) => throw new Exception(e)
          }
        Positioned(Position.Custom("OsLibc.defaultJvm"), os.Path(path))
      }
    }

  // used when downloading fails
  private def defaultStableScalaVersions =
    Seq(defaultScala212Version, defaultScala213Version, defaultScalaVersion)

  private def latestSupportedStableScalaVersion(): Seq[Version] = {

    val msg =
      if (internal.verbosityOrDefault > 0)
        "Getting list of Scala CLI-supported Scala versions"
      else ""
    val cache                     = finalCache.withMessage(msg)
    val supportedScalaVersionsUrl = scalaOptions.scalaVersionsUrl

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

    val scalaCliVersion = version
    val launchersTask   = cache.logger.using(task)

    //  If an error occurred while downloading stable versions,
    //  it uses stable scala versions from Deps.sc
    val supportedScalaVersions =
      launchersTask.attempt.unsafeRun()(cache.ec) match {
        case Left(_) =>
          // FIXME Log the exception
          defaultStableScalaVersions
        case Right(versions) =>
          versions
            .find(_.scalaCliVersion == scalaCliVersion)
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

  lazy val javaHomeManager = {
    val indexUrl = javaOptions.jvmIndexOpt.getOrElse(JvmIndex.coursierIndexUrl)
    val indexTask = {
      val msg   = if (internal.verbosityOrDefault > 0) "Downloading JVM index" else ""
      val cache = finalCache.withMessage(msg)
      cache.logger.using {
        JvmIndex.load(cache, indexUrl)
      }
    }
    val jvmCache = JvmCache()
      .withIndex(indexTask)
      .withArchiveCache(
        archiveCache.withCache(
          finalCache.withMessage("Downloading JVM")
        )
      )
      .withOs(jvmIndexOs)
      .withArchitecture(javaOptions.jvmIndexArch.getOrElse(JvmIndex.defaultArchitecture()))
    JavaHome().withCache(jvmCache)
  }

  private val scala2NightlyRepo = Seq(coursier.Repositories.scalaIntegration.root)

  def finalRepositories: Seq[String] =
    scalaParams.map { params =>
      if (isScala2Nightly(params.scalaVersion)) scala2NightlyRepo else Seq.empty
    }.getOrElse(Seq.empty) ++
      classPathOptions.extraRepositories ++ internal.localRepository.toSeq

  private lazy val maxSupportedStableScalaVersions = latestSupportedStableScalaVersion()

  private lazy val latestSupportedStableVersions = maxSupportedStableScalaVersions.map(_.repr)

  private def getAllMatchingStableVersions(scalaVersionArg: Option[String]): Seq[String] = {

    def isStable(version: String): Boolean =
      !version.exists(_.isLetter)

    modules(scalaVersionArg).flatMap(moduleVersions(_).versions.available.filter(isStable)).distinct
  }

  private def modules(maybeScalaVersionArg: Option[String]) = {
    def scala2 = cmod"org.scala-lang:scala-library"
    // No unstable, that *ought* not to be a problem down-the-line…?
    def scala3 = cmod"org.scala-lang:scala3-library_3"
    if (maybeScalaVersionArg.contains("2") || maybeScalaVersionArg.exists(_.startsWith("2.")))
      Seq(scala2)
    else if (maybeScalaVersionArg.contains("3") || maybeScalaVersionArg.exists(_.startsWith("3.")))
      Seq(scala3)
    else Seq(scala2, scala3)
  }

  private def moduleVersions(mod: Module): Versions.Result =
    finalCache.logger.use {
      try Versions(finalCache)
          .withModule(mod)
          .result()
          .unsafeRun()(finalCache.ec)
      catch {
        case NonFatal(e) => throw new Exception(e)
      }
    }

  private def getAllMatchingVersions(maybeScalaVersionArg: Option[String]): Seq[String] =
    modules(maybeScalaVersionArg).flatMap(moduleVersions(_).versions.available).distinct

  /** @param scalaVersionArg
    *   the command line, using directive, or default argument passed as scala version
    * @param scalaBinaryVersionArg
    *   the command line, using directive, or default argument passed as scala Binary version
    * @return
    *   Either a BuildException or the calculated (ScalaVersion, ScalaBinaryVersion) tuple
    */
  private def turnScalaVersionArgToStableScalaVersions(
    scalaVersionArg: Option[String],
    scalaBinaryVersionArg: Option[String]
  ): Either[BuildException, (String, String)] = either {

    lazy val allStableVersions = getAllMatchingStableVersions(scalaVersionArg)

    val scalaVersion = value(matchNewestStableScalaVersion(scalaVersionArg, allStableVersions))
    val scalaBinaryVersion = scalaBinaryVersionArg.getOrElse(ScalaVersion.binary(scalaVersion))
    (scalaVersion, scalaBinaryVersion)
  }

  private def turnScalaVersionArgToNonStableScalaVersions(
    scalaVersionArg: Option[String],
    scalaBinaryVersionArg: Option[String]
  ): Either[BuildException, (String, String)] = either {

    lazy val allStableVersions = getAllMatchingVersions(scalaVersionArg)

    val scalaVersion = value(matchNewestNonStableScalaVersion(scalaVersionArg, allStableVersions))
    val scalaBinaryVersion = scalaBinaryVersionArg.getOrElse(ScalaVersion.binary(scalaVersion))
    (scalaVersion, scalaBinaryVersion)
  }

  private def isSupportedVersion(version: String): Boolean =
    version.startsWith("2.12.") || version.startsWith("2.13.") || version.startsWith("3.")

  private def matchNewestStableScalaVersion(
    maybeScalaVersionStringArg: Option[String],
    versionPool: Seq[String]
  ): Either[ScalaVersionError, String] =
    maybeScalaVersionStringArg match {
      case Some(scalaVersionStringArg) =>
        val prefix =
          if (Util.isFullScalaVersion(scalaVersionStringArg)) scalaVersionStringArg
          else if (scalaVersionStringArg.endsWith(".")) scalaVersionStringArg
          else scalaVersionStringArg + "."
        val matchingStableVersions = versionPool.filter(_.startsWith(prefix)).map(Version(_))
        if (matchingStableVersions.isEmpty)
          Left(new InvalidBinaryScalaVersionError(
            scalaVersionStringArg,
            latestSupportedStableVersions
          ))
        else {
          val validMaxVersions = maxSupportedStableScalaVersions
            .filter(_.repr.startsWith(prefix))
          val validMatchingVersions = {
            val filtered = matchingStableVersions.filter(v => validMaxVersions.exists(v <= _))
            if (filtered.isEmpty) matchingStableVersions
            else filtered
          }.filter(v => isSupportedVersion(v.repr))
          if (validMatchingVersions.isEmpty)
            Left(new UnsupportedScalaVersionError(
              scalaVersionStringArg,
              latestSupportedStableVersions
            ))
          else
            Right(validMatchingVersions.max.repr)
        }
      case None =>
        val validVersions = versionPool
          .map(Version(_))
          .filter(v => maxSupportedStableScalaVersions.exists(v <= _))
        if (validVersions.isEmpty)
          Left(new NoValidScalaVersionFoundError(
            versionPool,
            latestSupportedStableVersions
          ))
        else
          Right(validVersions.max.repr)
    }

  private def matchNewestNonStableScalaVersion(
    maybeScalaVersionStringArg: Option[String],
    versionPool: Seq[String]
  ): Either[ScalaVersionError, String] =
    maybeScalaVersionStringArg match {
      case Some(scalaVersionStringArg) =>
        if (versionPool.contains(scalaVersionStringArg))
          if (isSupportedVersion(scalaVersionStringArg))
            Right(scalaVersionStringArg)
          else
            Left(new UnsupportedScalaVersionError(
              scalaVersionStringArg,
              latestSupportedStableVersions
            ))
        else
          Left(new InvalidBinaryScalaVersionError(
            scalaVersionStringArg,
            latestSupportedStableVersions
          ))

      case None =>
        Left(new NoValidScalaVersionFoundError(
          versionPool,
          latestSupportedStableVersions
        ))

    }

  private def latestScalaVersionFrom(
    versions: CoreVersions,
    desc: String
  ): Either[scala.build.errors.ScalaVersionError, String] =
    versions.latest(coursier.core.Latest.Release) match {
      case Some(versionString) => Right(versionString)
      case None =>
        val msg =
          s"Unable to find matching version for $desc in available version: ${versions.available.mkString(", ")}. " +
            "This error may indicate a network or other problem accessing repository."
        Left(new ScalaVersionError(msg))
    }

  /** @return
    *   Either a BuildException or the calculated (ScalaVersion, ScalaBinaryVersion) tuple
    */
  private def computeLatestScalaThreeNightlyVersions(): Either[BuildException, (String, String)] =
    either {
      val moduleVersion: Either[ScalaVersionError, String] = {
        def scala3 = cmod"org.scala-lang:scala3-library_3"
        val res = finalCache.logger.use {
          Versions(finalCache)
            .withModule(scala3)
            .result()
            .unsafeRun()(finalCache.ec)
        }
        latestScalaVersionFrom(res.versions, "latest Scala 3 nightly build")
      }

      val scalaVersion       = value(moduleVersion)
      val scalaBinaryVersion = ScalaVersion.binary(scalaVersion)
      (scalaVersion, scalaBinaryVersion)
    }

  /** @return
    *   Either a BuildException or the calculated (ScalaVersion, ScalaBinaryVersion) tuple
    */
  private def computeLatestScalaThreeXNightlyVersions(threeSubBinaryNum: String)
    : Either[BuildException, (String, String)] =
    either {
      val moduleVersion: Either[ScalaVersionError, String] = {
        def scala3 = cmod"org.scala-lang:scala3-library_3"
        val res = finalCache.logger.use {
          Versions(finalCache)
            .withModule(scala3)
            .result()
            .unsafeRun()(finalCache.ec)
        }.versions.available.filter(_.endsWith("-NIGHTLY"))

        val threeXNightlies = res.filter(_.startsWith(s"3.$threeSubBinaryNum.")).map(Version(_))
        if (threeXNightlies.nonEmpty) Right(threeXNightlies.max.repr)
        else Left(
          new NoValidScalaVersionFoundError(res, latestSupportedStableVersions)
        )
      }
      val scalaVersion       = value(moduleVersion)
      val scalaBinaryVersion = ScalaVersion.binary(scalaVersion)
      (scalaVersion, scalaBinaryVersion)
    }

  /** @return
    *   Either a BuildException or the calculated (ScalaVersion, ScalaBinaryVersion) tuple
    */
  private def computeLatestScalaTwoNightlyVersions(): Either[BuildException, (String, String)] =
    either {
      val moduleVersion: Either[ScalaVersionError, String] = {
        def scalaNightly2Module: Module = cmod"org.scala-lang:scala-library"
        val res = finalCache.logger.use {
          Versions(finalCache)
            .withModule(scalaNightly2Module)
            .withRepositories(Seq(coursier.Repositories.scalaIntegration))
            .result()
            .unsafeRun()(finalCache.ec)
        }
        latestScalaVersionFrom(res.versions, "latest Scala 2 nightly build")
      }

      val scalaVersion       = value(moduleVersion)
      val scalaBinaryVersion = ScalaVersion.binary(scalaVersion)
      (scalaVersion, scalaBinaryVersion)
    }

  private def turnScala2NightlyVersionArgToVersions(versionString: String)
    : Either[BuildException, (String, String)] = either {

    val moduleVersion: Either[ScalaVersionError, String] = {
      def scalaNightly2Module: Module = cmod"org.scala-lang:scala-library"
      val res = finalCache.logger.use {
        Versions(finalCache)
          .withModule(scalaNightly2Module)
          .withRepositories(Seq(coursier.Repositories.scalaIntegration))
          .result()
          .unsafeRun()(finalCache.ec)
      }
      if (res.versions.available.contains(versionString)) Right(versionString)
      else
        Left(
          new NoValidScalaVersionFoundError(res.versions.available, latestSupportedStableVersions)
        )
    }

    val scalaVersion       = value(moduleVersion)
    val scalaBinaryVersion = ScalaVersion.binary(scalaVersion)
    (scalaVersion, scalaBinaryVersion)
  }

  private def turnScala3NightlyVersionArgIntoVersion(versionString: String)
    : Either[BuildException, (String, String)] = either {
    val moduleVersion: Either[ScalaVersionError, String] = {
      def scala3 = cmod"org.scala-lang:scala3-library_3"
      val res = finalCache.logger.use {
        Versions(finalCache)
          .withModule(scala3)
          .result()
          .unsafeRun()(finalCache.ec)
      }
      if (res.versions.available.contains(versionString)) Right(versionString)
      else
        Left(
          new NoValidScalaVersionFoundError(res.versions.available, latestSupportedStableVersions)
        )
    }

    val scalaVersion       = value(moduleVersion)
    val scalaBinaryVersion = ScalaVersion.binary(scalaVersion)
    (scalaVersion, scalaBinaryVersion)
  }

  def computeLatestScalaTwoTwelveNightlyVersions(): Either[BuildException, (String, String)] =
    either {
      val moduleVersion: Either[ScalaVersionError, String] = {
        def scalaNightly2Module: Module = cmod"org.scala-lang:scala-library"
        val res = finalCache.logger.use {
          Versions(finalCache)
            .withModule(scalaNightly2Module)
            .withRepositories(Seq(coursier.Repositories.scalaIntegration))
            .result()
            .unsafeRun()(finalCache.ec)
        }.versions.available
        val twoTwelveNightlies = res.filter(_.startsWith("2.12.")).map(Version(_))
        if (twoTwelveNightlies.nonEmpty) Right(twoTwelveNightlies.max.repr)
        else Left(
          new NoValidScalaVersionFoundError(res, latestSupportedStableVersions)
        )
      }

      val scalaVersion       = value(moduleVersion)
      val scalaBinaryVersion = ScalaVersion.binary(scalaVersion)
      (scalaVersion, scalaBinaryVersion)
    }

  private def isScala2Nightly(version: String): Boolean =
    scala2NightlyRegex.unapplySeq(version).isDefined

  lazy val scalaParams: Either[BuildException, ScalaParameters] = either {
    def isScala3Nightly(version: String): Boolean =
      version.startsWith("3") && version.endsWith("-NIGHTLY")

    val (scalaVersion, scalaBinaryVersion) =
      value {
        scalaOptions.scalaVersion match {
          case Some("3.nightly") => computeLatestScalaThreeNightlyVersions()
          case Some(scala3NightlyNicknameRegex(threeSubBinaryNum)) =>
            computeLatestScalaThreeXNightlyVersions(threeSubBinaryNum)
          case Some("2.nightly")    => computeLatestScalaTwoNightlyVersions()
          case Some("2.13.nightly") => computeLatestScalaTwoNightlyVersions()
          case Some("2.12.nightly") => computeLatestScalaTwoTwelveNightlyVersions()
          case Some(versionString) if isScala3Nightly(versionString) =>
            turnScala3NightlyVersionArgIntoVersion(versionString)
          case Some(versionString) if isScala2Nightly(versionString) =>
            turnScala2NightlyVersionArgToVersions(versionString)
          case Some(versionString) if versionString.exists(_.isLetter) =>
            turnScalaVersionArgToNonStableScalaVersions(
              scalaOptions.scalaVersion,
              scalaOptions.scalaBinaryVersion
            )
          case _ => turnScalaVersionArgToStableScalaVersions(
              scalaOptions.scalaVersion,
              scalaOptions.scalaBinaryVersion
            )
        }
      }

    val maybePlatformSuffix = platform.value match {
      case Platform.JVM    => None
      case Platform.JS     => Some(scalaJsOptions.platformSuffix)
      case Platform.Native => Some(scalaNativeOptions.platformSuffix)
    }
    ScalaParameters(scalaVersion, scalaBinaryVersion, maybePlatformSuffix)
  }

  def artifacts(logger: Logger): Either[BuildException, Artifacts] = either {
    val maybeArtifacts = Artifacts(
      params = value(scalaParams),
      compilerPlugins = value(compilerPlugins),
      javacPluginDependencies = value(javacPluginDependencies),
      extraJavacPlugins = javaOptions.javacPlugins.map(_.value),
      dependencies = value(dependencies),
      extraClassPath = allExtraJars,
      scalaJsCliVersion =
        if (platform.value == Platform.JS) Some(scalaJsCliVersion) else None,
      scalaNativeCliVersion =
        if (platform.value == Platform.Native) Some(scalaNativeOptions.finalVersion) else None,
      extraCompileOnlyJars = allExtraCompileOnlyJars,
      extraSourceJars = allExtraSourceJars,
      fetchSources = classPathOptions.fetchSources.getOrElse(false),
      addStubs = internalDependencies.addStubsDependency,
      addJvmRunner = addRunnerDependency,
      addJvmTestRunner = addJvmTestRunner,
      addJsTestBridge = addJsTestBridge,
      addNativeTestInterface = addNativeTestInterface,
      addJmhDependencies = jmhOptions.addJmhDependencies,
      extraRepositories = finalRepositories,
      cache = finalCache,
      logger = logger
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
          scalaVersion = Some(sv),
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

  def validate: Seq[Diagnostic] = BuildOptionsRule.validateAll(this)

  def nativeImageWorkDir(root: os.Path, projectName: String): os.Path =
    root / workspaceDirName / projectName / "native-image"
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
  )

  implicit val hasHashData: HasHashData[BuildOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[BuildOptions]     = ConfigMonoid.derive
}
