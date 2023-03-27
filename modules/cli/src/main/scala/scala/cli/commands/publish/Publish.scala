package scala.cli.commands.publish

import caseapp.core.RemainingArgs
import caseapp.core.help.HelpFormat
import coursier.cache.{ArchiveCache, FileCache}
import coursier.core.{Authentication, Configuration}
import coursier.maven.MavenRepository
import coursier.publish.checksum.logger.InteractiveChecksumLogger
import coursier.publish.checksum.{ChecksumType, Checksums}
import coursier.publish.fileset.{FileSet, Path}
import coursier.publish.signing.logger.InteractiveSignerLogger
import coursier.publish.signing.{GpgSigner, NopSigner, Signer}
import coursier.publish.sonatype.SonatypeApi
import coursier.publish.upload.logger.InteractiveUploadLogger
import coursier.publish.upload.{DummyUpload, FileUpload, HttpURLConnectionUpload}
import coursier.publish.{Content, Hooks, Pom, PublishRepository}

import java.io.{File, OutputStreamWriter}
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.concurrent.Executors
import java.util.function.Supplier

import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.*
import scala.build.compiler.ScalaCompilerMaker
import scala.build.errors.{BuildException, CompositeBuildException, NoMainClassFoundError, Severity}
import scala.build.input.Inputs
import scala.build.internal.Util
import scala.build.internal.Util.ScalaDependencyOps
import scala.build.options.publish.{ComputeVersion, Developer, License, Signer => PSigner, Vcs}
import scala.build.options.{
  BuildOptions,
  ConfigMonoid,
  PublishContextualOptions,
  ScalaSigningCliOptions,
  Scope
}
import scala.cli.CurrentParams
import scala.cli.commands.package0.Package as PackageCmd
import scala.cli.commands.pgp.{PgpExternalCommand, PgpScalaSigningOptions}
import scala.cli.commands.publish.ConfigUtil.*
import scala.cli.commands.publish.{PublishParamsOptions, PublishRepositoryOptions}
import scala.cli.commands.shared.{
  HelpCommandGroup,
  HelpGroup,
  MainClassOptions,
  SharedOptions,
  SharedPythonOptions
}
import scala.cli.commands.util.{BuildCommandHelpers, ScalaCliSttpBackend}
import scala.cli.commands.{ScalaCommand, SpecificationLevel, WatchUtil}
import scala.cli.config.{ConfigDb, Keys, PasswordOption, PublishCredentials}
import scala.cli.errors.{
  FailedToSignFileError,
  MalformedChecksumsError,
  MissingConfigEntryError,
  MissingPublishOptionError,
  UploadError
}
import scala.cli.packaging.Library
import scala.cli.publish.BouncycastleSignerMaker
import scala.cli.util.ArgHelpers.*
import scala.cli.util.ConfigDbUtils
import scala.cli.util.ConfigPasswordOptionHelpers.*
import scala.util.control.NonFatal

object Publish extends ScalaCommand[PublishOptions] with BuildCommandHelpers {

  override def scalaSpecificationLevel: SpecificationLevel = SpecificationLevel.EXPERIMENTAL

  import scala.cli.commands.shared.HelpGroup.*
  val primaryHelpGroups: Seq[HelpGroup] = Seq(Publishing, Signing, PGP)
  val hiddenHelpGroups: Seq[HelpGroup]  = Seq(Scala, Java, Entrypoint, Dependency, Watch)
  override def helpFormat: HelpFormat = super.helpFormat
    .withHiddenGroups(hiddenHelpGroups)
    .withPrimaryGroups(primaryHelpGroups)
  override def group: String = HelpCommandGroup.Main.toString
  override def sharedOptions(options: PublishOptions): Option[SharedOptions] =
    Some(options.shared)

  def mkBuildOptions(
    baseOptions: BuildOptions,
    publishParams: PublishParamsOptions,
    sharedPublish: SharedPublishOptions,
    publishRepo: PublishRepositoryOptions,
    scalaSigning: PgpScalaSigningOptions,
    mainClass: MainClassOptions,
    ivy2LocalLike: Option[Boolean]
  ): Either[BuildException, BuildOptions] = either {
    val contextualOptions = PublishContextualOptions(
      repository = publishRepo.publishRepository.filter(_.trim.nonEmpty),
      repositoryIsIvy2LocalLike = ivy2LocalLike,
      sourceJar = sharedPublish.sources,
      docJar = sharedPublish.doc,
      gpgSignatureId = sharedPublish.gpgKey.map(_.trim).filter(_.nonEmpty),
      gpgOptions = sharedPublish.gpgOption,
      secretKey = publishParams.secretKey.map(_.configPasswordOptions()),
      secretKeyPassword = publishParams.secretKeyPassword.map(_.configPasswordOptions()),
      repoUser = publishRepo.user,
      repoPassword = publishRepo.password,
      repoRealm = publishRepo.realm,
      signer = value {
        sharedPublish.signer
          .map(Positioned.commandLine)
          .map(PSigner.parse)
          .sequence
      },
      computeVersion = value {
        publishParams.computeVersion
          .map(Positioned.commandLine)
          .map(ComputeVersion.parse)
          .sequence
      },
      checksums = {
        val input = sharedPublish.checksum.flatMap(_.split(",")).map(_.trim).filter(_.nonEmpty)
        if (input.isEmpty) None
        else Some(input)
      }
    )
    baseOptions.copy(
      mainClass = mainClass.mainClass.filter(_.nonEmpty),
      notForBloopOptions = baseOptions.notForBloopOptions.copy(
        publishOptions = baseOptions.notForBloopOptions.publishOptions.copy(
          organization =
            publishParams.organization.map(_.trim).filter(_.nonEmpty).map(Positioned.commandLine),
          name = publishParams.name.map(_.trim).filter(_.nonEmpty).map(Positioned.commandLine),
          moduleName =
            publishParams.moduleName.map(_.trim).filter(_.nonEmpty).map(Positioned.commandLine),
          version =
            publishParams.version.map(_.trim).filter(_.nonEmpty).map(Positioned.commandLine),
          url = publishParams.url.map(_.trim).filter(_.nonEmpty).map(Positioned.commandLine),
          license = value {
            publishParams.license
              .map(_.trim).filter(_.nonEmpty)
              .map(Positioned.commandLine)
              .map(License.parse)
              .sequence
          },
          versionControl = value {
            publishParams.vcs
              .map(_.trim).filter(_.nonEmpty)
              .map(Positioned.commandLine)
              .map(Vcs.parse)
              .sequence
          },
          description = publishParams.description.map(_.trim).filter(_.nonEmpty),
          developers = value {
            publishParams.developer
              .filter(_.trim.nonEmpty)
              .map(Positioned.commandLine)
              .map(Developer.parse)
              .sequence
              .left.map(CompositeBuildException(_))
          },
          scalaVersionSuffix = sharedPublish.scalaVersionSuffix.map(_.trim),
          scalaPlatformSuffix = sharedPublish.scalaPlatformSuffix.map(_.trim),
          local = ConfigMonoid.sum(Seq(
            baseOptions.notForBloopOptions.publishOptions.local,
            if (publishParams.isCi) PublishContextualOptions() else contextualOptions
          )),
          ci = ConfigMonoid.sum(Seq(
            baseOptions.notForBloopOptions.publishOptions.ci,
            if (publishParams.isCi) contextualOptions
            else PublishContextualOptions()
          )),
          signingCli = ScalaSigningCliOptions(
            signingCliVersion = scalaSigning.signingCliVersion,
            useJvm = scalaSigning.forceJvmSigningCli,
            javaArgs = scalaSigning.signingCliJavaArg
          )
        )
      )
    )
  }

  def maybePrintLicensesAndExit(params: PublishParamsOptions): Unit =
    if (params.license.contains("list")) {
      for (l <- scala.build.internal.Licenses.list)
        println(s"${l.id}: ${l.name} (${l.url})")
      sys.exit(0)
    }

  def maybePrintChecksumsAndExit(options: SharedPublishOptions): Unit =
    if (options.checksum.contains("list")) {
      for (t <- ChecksumType.all)
        println(t.name)
      sys.exit(0)
    }

  override def runCommand(options: PublishOptions, args: RemainingArgs, logger: Logger): Unit = {
    maybePrintLicensesAndExit(options.publishParams)
    maybePrintChecksumsAndExit(options.sharedPublish)

    val baseOptions = buildOptionsOrExit(options)
    val inputs      = options.shared.inputs(args.all).orExit(logger)
    CurrentParams.workspaceOpt = Some(inputs.workspace)

    val initialBuildOptions = mkBuildOptions(
      baseOptions,
      options.publishParams,
      options.sharedPublish,
      options.publishRepo,
      options.signingCli,
      options.mainClass,
      options.ivy2LocalLike
    ).orExit(logger)
    val threads = BuildThreads.create()

    val compilerMaker    = options.shared.compilerMaker(threads).orExit(logger)
    val docCompilerMaker = options.shared.compilerMaker(threads, scaladoc = true).orExit(logger)

    val cross = options.compileCross.cross.getOrElse(false)

    lazy val configDb = ConfigDbUtils.configDb.orExit(logger)

    lazy val workingDir = options.sharedPublish.workingDir
      .filter(_.trim.nonEmpty)
      .map(os.Path(_, os.pwd))
      .getOrElse {
        os.temp.dir(
          prefix = "scala-cli-publish-",
          deleteOnExit = true
        )
      }

    val ivy2HomeOpt = options.sharedPublish.ivy2Home
      .filter(_.trim.nonEmpty)
      .map(os.Path(_, os.pwd))

    doRun(
      inputs,
      logger,
      initialBuildOptions,
      compilerMaker,
      docCompilerMaker,
      cross,
      workingDir,
      ivy2HomeOpt,
      publishLocal = false,
      forceSigningBinary = options.sharedPublish.forceSigningBinary,
      parallelUpload = options.parallelUpload,
      options.watch.watch,
      isCi = options.publishParams.isCi,
      () => configDb,
      options.mainClass,
      dummy = options.sharedPublish.dummy
    )
  }

  /** Build artifacts
    */
  def doRun(
    inputs: Inputs,
    logger: Logger,
    initialBuildOptions: BuildOptions,
    compilerMaker: ScalaCompilerMaker,
    docCompilerMaker: ScalaCompilerMaker,
    cross: Boolean,
    workingDir: => os.Path,
    ivy2HomeOpt: Option[os.Path],
    publishLocal: Boolean,
    forceSigningBinary: Boolean,
    parallelUpload: Option[Boolean],
    watch: Boolean,
    isCi: Boolean,
    configDb: () => ConfigDb,
    mainClassOptions: MainClassOptions,
    dummy: Boolean
  ): Unit = {

    val actionableDiagnostics = configDb().get(Keys.actions).getOrElse(None)

    if (watch) {
      val watcher = Build.watch(
        inputs,
        initialBuildOptions,
        compilerMaker,
        Some(docCompilerMaker),
        logger,
        crossBuilds = cross,
        buildTests = false,
        partial = None,
        actionableDiagnostics = actionableDiagnostics,
        postAction = () => WatchUtil.printWatchMessage()
      ) { res =>
        res.orReport(logger).foreach { builds =>
          maybePublish(
            builds,
            workingDir,
            ivy2HomeOpt,
            publishLocal,
            logger,
            allowExit = false,
            forceSigningBinary = forceSigningBinary,
            parallelUpload = parallelUpload,
            isCi = isCi,
            configDb,
            mainClassOptions,
            dummy
          )
        }
      }
      try WatchUtil.waitForCtrlC(() => watcher.schedule())
      finally watcher.dispose()
    }
    else {
      val builds =
        Build.build(
          inputs,
          initialBuildOptions,
          compilerMaker,
          Some(docCompilerMaker),
          logger,
          crossBuilds = cross,
          buildTests = false,
          partial = None,
          actionableDiagnostics = actionableDiagnostics
        ).orExit(logger)
      maybePublish(
        builds,
        workingDir,
        ivy2HomeOpt,
        publishLocal,
        logger,
        allowExit = true,
        forceSigningBinary = forceSigningBinary,
        parallelUpload = parallelUpload,
        isCi = isCi,
        configDb,
        mainClassOptions,
        dummy
      )
    }
  }

  private def defaultOrganization(
    ghOrgOpt: Option[String],
    logger: Logger
  ): Either[BuildException, String] =
    ghOrgOpt match {
      case Some(org) =>
        val mavenOrg = s"io.github.$org"
        logger.message(
          s"Using directive publish.organization not set, computed $mavenOrg from GitHub organization $org as default organization"
        )
        Right(mavenOrg)
      case None =>
        Left(new MissingPublishOptionError(
          "organization",
          "--organization",
          "publish.organization"
        ))
    }
  private def defaultName(workspace: os.Path, logger: Logger): String = {
    val name = workspace.last
    logger.message(
      s"Using directive publish.name not specified, using workspace directory name $name as default name"
    )
    name
  }
  def defaultComputeVersion(mayDefaultToGitTag: Boolean): Option[ComputeVersion] =
    if (mayDefaultToGitTag) Some(ComputeVersion.GitTag(os.rel, dynVer = false))
    else None
  def defaultVersionError =
    new MissingPublishOptionError("version", "--version", "publish.version")
  def defaultVersion: Either[BuildException, String] =
    Left(defaultVersionError)

  /** Check if all builds are successful and proceed with preparing files to be uploaded OR print
    * main classes if the option is specified
    */
  private def maybePublish(
    builds: Builds,
    workingDir: os.Path,
    ivy2HomeOpt: Option[os.Path],
    publishLocal: Boolean,
    logger: Logger,
    allowExit: Boolean,
    forceSigningBinary: Boolean,
    parallelUpload: Option[Boolean],
    isCi: Boolean,
    configDb: () => ConfigDb,
    mainClassOptions: MainClassOptions,
    dummy: Boolean
  ): Unit = {

    val allOk = builds.all.forall {
      case _: Build.Successful => true
      case _: Build.Cancelled  => false
      case _: Build.Failed     => false
    }
    val allDocsOk = builds.allDoc.forall {
      case _: Build.Successful => true
      case _: Build.Cancelled  => true
      case _: Build.Failed     => false
    }
    if (allOk && allDocsOk) {
      val builds0 = builds.all.collect {
        case s: Build.Successful => s
      }
      val docBuilds0 = builds.allDoc.collect {
        case s: Build.Successful => s
      }
      val res: Either[BuildException, Unit] =
        builds.main match {
          case s: Build.Successful if mainClassOptions.mainClassLs.contains(true) =>
            mainClassOptions.maybePrintMainClasses(s.foundMainClasses(), shouldExit = allowExit)
          case _ => prepareFilesAndUpload(
              builds0,
              docBuilds0,
              workingDir,
              ivy2HomeOpt,
              publishLocal,
              logger,
              forceSigningBinary,
              parallelUpload,
              isCi,
              configDb,
              dummy
            )
        }
      if (allowExit)
        res.orExit(logger)
      else
        res.orReport(logger)
    }
    else {
      val msg = if (allOk) "Scaladoc generation failed" else "Compilation failed"
      System.err.println(msg)
      if (allowExit)
        sys.exit(1)
    }
  }

  /** Get organization, project name and version from options and directives or try to compute
    * defaults
    */
  private def orgNameVersion(
    publishOptions: scala.build.options.PublishOptions,
    workspace: os.Path,
    logger: Logger,
    scalaArtifactsOpt: Option[ScalaArtifacts],
    isCi: Boolean
  ): Either[BuildException, (String, String, String)] = {

    lazy val orgNameOpt = GitRepo.maybeGhRepoOrgName(workspace, logger)

    val maybeOrg = publishOptions.organization match {
      case Some(org0) => Right(org0.value)
      case None       => defaultOrganization(orgNameOpt.map(_._1), logger)
    }

    val moduleName = publishOptions.moduleName match {
      case Some(name0) => name0.value
      case None =>
        val name = publishOptions.name match {
          case Some(name0) => name0.value
          case None        => defaultName(workspace, logger)
        }
        scalaArtifactsOpt.map(_.params) match {
          case Some(scalaParams) =>
            val pf = publishOptions.scalaPlatformSuffix.getOrElse {
              scalaParams.platform.fold("")("_" + _)
            }
            val sv = publishOptions.scalaVersionSuffix.getOrElse {
              // FIXME Allow full cross version too
              "_" + scalaParams.scalaBinaryVersion
            }
            name + pf + sv
          case None =>
            name
        }
    }

    val maybeVer = publishOptions.version match {
      case Some(ver0) => Right(ver0.value)
      case None =>
        val computeVer = publishOptions.contextual(isCi).computeVersion.orElse {
          def isGitRepo = GitRepo.gitRepoOpt(workspace).isDefined
          val default   = defaultComputeVersion(!isCi && isGitRepo)
          if (default.isDefined)
            logger.message(
              s"Using directive ${defaultVersionError.directiveName} not set, assuming git:tag as publish.computeVersion"
            )
          default
        }
        computeVer match {
          case Some(cv) => cv.get(workspace)
          case None     => defaultVersion
        }
    }

    (maybeOrg, maybeVer)
      .traverseN
      .left.map(CompositeBuildException(_))
      .map {
        case (org, ver) =>
          (org, moduleName, ver)
      }
  }

  private def buildFileSet(
    build: Build.Successful,
    docBuildOpt: Option[Build.Successful],
    workingDir: os.Path,
    now: Instant,
    isIvy2LocalLike: Boolean,
    isCi: Boolean,
    isSonatype: Boolean,
    logger: Logger
  ): Either[BuildException, (FileSet, (coursier.core.Module, String))] = either {

    logger.debug(s"Preparing project ${build.project.projectName}")

    val publishOptions = build.options.notForBloopOptions.publishOptions

    val (org, moduleName, ver) = value {
      orgNameVersion(
        publishOptions,
        build.inputs.workspace,
        logger,
        build.artifacts.scalaOpt,
        isCi
      )
    }

    logger.message(s"Publishing $org:$moduleName:$ver")

    val mainJar = {
      val mainClassOpt = build.options.mainClass.orElse {
        build.retainedMainClass(logger) match {
          case Left(_: NoMainClassFoundError) => None
          case Left(err) =>
            logger.debug(s"Error while looking for main class: $err")
            None
          case Right(cls) => Some(cls)
        }
      }
      val content = Library.libraryJar(build, mainClassOpt)
      val dest    = workingDir / org / s"$moduleName-$ver.jar"
      os.write.over(dest, content, createFolders = true)
      dest
    }

    val sourceJarOpt =
      if (publishOptions.contextual(isCi).sourceJar.getOrElse(true)) {
        val content   = PackageCmd.sourceJar(build, now.toEpochMilli)
        val sourceJar = workingDir / org / s"$moduleName-$ver-sources.jar"
        os.write.over(sourceJar, content, createFolders = true)
        Some(sourceJar)
      }
      else
        None

    val docJarOpt =
      if (publishOptions.contextual(isCi).docJar.getOrElse(true))
        docBuildOpt match {
          case None => None
          case Some(docBuild) =>
            val docJarPath = value(PackageCmd.docJar(docBuild, logger, Nil))
            val docJar     = workingDir / org / s"$moduleName-$ver-javadoc.jar"
            os.copy.over(docJarPath, docJar, createFolders = true)
            Some(docJar)
        }
      else
        None

    val dependencies = build.artifacts.userDependencies
      .map(_.toCs(build.artifacts.scalaOpt.map(_.params)))
      .sequence
      .left.map(CompositeBuildException(_))
      .orExit(logger)
      .map { dep0 =>
        val config =
          if (build.scope == Scope.Main) None
          else Some(Configuration(build.scope.name))
        (dep0.module.organization, dep0.module.name, dep0.version, config)
      }
    val url = publishOptions.url.map(_.value)
    val license = publishOptions.license.map(_.value).map { l =>
      Pom.License(l.name, l.url)
    }
    val scm = publishOptions.versionControl.map { vcs =>
      Pom.Scm(vcs.url, vcs.connection, vcs.developerConnection)
    }
    val developers = publishOptions.developers.map { dev =>
      Pom.Developer(dev.id, dev.name, dev.url, dev.mail)
    }
    val description = publishOptions.description.getOrElse(moduleName)

    val pomContent = Pom.create(
      organization = coursier.Organization(org),
      moduleName = coursier.ModuleName(moduleName),
      version = ver,
      packaging = None,
      url = url,
      name = Some(moduleName), // ?
      dependencies = dependencies,
      description = Some(description),
      license = license,
      scm = scm,
      developers = developers
    )

    if (isSonatype) {
      if (url.isEmpty)
        logger.diagnostic(
          "Publishing to Sonatype, but project URL is empty (set it with the '//> using publish.url' directive)."
        )
      if (license.isEmpty)
        logger.diagnostic(
          "Publishing to Sonatype, but license is empty (set it with the '//> using publish.license' directive)."
        )
      if (scm.isEmpty)
        logger.diagnostic(
          "Publishing to Sonatype, but SCM details are empty (set them with the '//> using publish.scm' directive)."
        )
      if (developers.isEmpty)
        logger.diagnostic(
          "Publishing to Sonatype, but developer details are empty (set them with the '//> using publish.developer' directive)."
        )
    }

    def ivyContent = Ivy.create(
      organization = coursier.Organization(org),
      moduleName = coursier.ModuleName(moduleName),
      version = ver,
      packaging = None,
      url = url,
      name = Some(moduleName), // ?
      dependencies = dependencies,
      description = Some(description),
      license = license,
      scm = scm,
      developers = developers,
      time = LocalDateTime.ofInstant(now, ZoneOffset.UTC),
      hasDoc = docJarOpt.isDefined,
      hasSources = sourceJarOpt.isDefined
    )

    def mavenFileSet = {

      val basePath = Path(org.split('.').toSeq ++ Seq(moduleName, ver))

      val mainEntries = Seq(
        (basePath / s"$moduleName-$ver.pom") -> Content.InMemory(
          now,
          pomContent.getBytes(StandardCharsets.UTF_8)
        ),
        (basePath / s"$moduleName-$ver.jar") -> Content.File(mainJar.toNIO)
      )

      val sourceJarEntries = sourceJarOpt
        .map { sourceJar =>
          (basePath / s"$moduleName-$ver-sources.jar") -> Content.File(sourceJar.toNIO)
        }
        .toSeq

      val docJarEntries = docJarOpt
        .map { docJar =>
          (basePath / s"$moduleName-$ver-javadoc.jar") -> Content.File(docJar.toNIO)
        }
        .toSeq

      // TODO version listings, …
      FileSet(mainEntries ++ sourceJarEntries ++ docJarEntries)
    }

    def ivy2LocalLikeFileSet = {

      val basePath = Path(Seq(org, moduleName, ver))

      val mainEntries = Seq(
        (basePath / "poms" / s"$moduleName.pom") -> Content.InMemory(
          now,
          pomContent.getBytes(StandardCharsets.UTF_8)
        ),
        (basePath / "ivys" / "ivy.xml") -> Content.InMemory(
          now,
          ivyContent.getBytes(StandardCharsets.UTF_8)
        ),
        (basePath / "jars" / s"$moduleName.jar") -> Content.File(mainJar.toNIO)
      )

      val sourceJarEntries = sourceJarOpt
        .map { sourceJar =>
          (basePath / "srcs" / s"$moduleName-sources.jar") -> Content.File(sourceJar.toNIO)
        }
        .toSeq

      val docJarEntries = docJarOpt
        .map { docJar =>
          (basePath / "docs" / s"$moduleName-javadoc.jar") -> Content.File(docJar.toNIO)
        }
        .toSeq

      FileSet(mainEntries ++ sourceJarEntries ++ docJarEntries)
    }

    val fileSet =
      if (isIvy2LocalLike) ivy2LocalLikeFileSet else mavenFileSet

    val mod = coursier.core.Module(
      coursier.core.Organization(org),
      coursier.core.ModuleName(moduleName),
      Map()
    )

    (fileSet, (mod, ver))
  }

  /** Sign and checksum files, then upload everything to the target repository
    */
  private def prepareFilesAndUpload(
    builds: Seq[Build.Successful],
    docBuilds: Seq[Build.Successful],
    workingDir: os.Path,
    ivy2HomeOpt: Option[os.Path],
    publishLocal: Boolean,
    logger: Logger,
    forceSigningBinary: Boolean,
    parallelUpload: Option[Boolean],
    isCi: Boolean,
    configDb: () => ConfigDb,
    dummy: Boolean
  ): Either[BuildException, Unit] = either {

    assert(docBuilds.isEmpty || docBuilds.length == builds.length)

    val it = builds.iterator.zip {
      if (docBuilds.isEmpty) Iterator.continually(None)
      else docBuilds.iterator.map(Some(_))
    }

    val publishOptions = ConfigMonoid.sum(
      builds.map(_.options.notForBloopOptions.publishOptions)
    )

    val ec = builds.head.options.finalCache.ec

    def authOpt(repo: String): Either[BuildException, Option[Authentication]] = either {
      val isHttps = {
        val uri = new URI(repo)
        uri.getScheme == "https"
      }
      val hostOpt = Option.when(isHttps)(new URI(repo).getHost)
      val maybeCredentials: Either[BuildException, Option[PublishCredentials]] = hostOpt match {
        case None => Right(None)
        case Some(host) =>
          configDb().get(Keys.publishCredentials).wrapConfigException.map { credListOpt =>
            credListOpt.flatMap { credList =>
              credList.find { cred =>
                cred.host == host &&
                (isHttps || cred.httpsOnly.contains(false))
              }
            }
          }
      }
      val isSonatype =
        hostOpt.exists(host => host == "oss.sonatype.org" || host.endsWith(".oss.sonatype.org"))
      val passwordOpt = publishOptions.contextual(isCi).repoPassword match {
        case None  => value(maybeCredentials).flatMap(_.password)
        case other => other.map(_.toConfig)
      }
      passwordOpt.map(_.get()) match {
        case None => None
        case Some(password) =>
          val userOpt = publishOptions.contextual(isCi).repoUser match {
            case None  => value(maybeCredentials).flatMap(_.user)
            case other => other.map(_.toConfig)
          }
          val realmOpt = publishOptions.contextual(isCi).repoRealm match {
            case None =>
              value(maybeCredentials)
                .flatMap(_.realm)
                .orElse {
                  if (isSonatype) Some("Sonatype Nexus Repository Manager")
                  else None
                }
            case other => other
          }
          val auth = Authentication(userOpt.fold("")(_.get().value), password.value)
          Some(realmOpt.fold(auth)(auth.withRealm))
      }
    }

    val repoParams = {

      lazy val es =
        Executors.newSingleThreadScheduledExecutor(Util.daemonThreadFactory("publish-retry"))

      if (publishLocal)
        RepoParams.ivy2Local(ivy2HomeOpt)
      else
        value {
          publishOptions.contextual(isCi).repository match {
            case None =>
              Left(new MissingPublishOptionError(
                "repository",
                "--publish-repository",
                "publish.repository"
              ))
            case Some(repo) =>
              RepoParams(
                repo,
                publishOptions.versionControl.map(_.url),
                builds.head.inputs.workspace,
                ivy2HomeOpt,
                publishOptions.contextual(isCi).repositoryIsIvy2LocalLike.getOrElse(false),
                es,
                logger
              )
          }
        }
    }

    val now = Instant.now()
    val (fileSet0, modVersionOpt) = value {
      it
        // TODO Allow to add test JARs to the main build artifacts
        .filter(_._1.scope != Scope.Test)
        .map {
          case (build, docBuildOpt) =>
            val isSonatype = {
              val hostOpt = {
                val repo = repoParams.repo.snapshotRepo.root
                val uri  = new URI(repo)
                if (uri.getScheme == "https") Some(uri.getHost)
                else None
              }
              hostOpt.exists(host =>
                host == "oss.sonatype.org" || host.endsWith(".oss.sonatype.org")
              )
            }
            buildFileSet(
              build,
              docBuildOpt,
              workingDir,
              now,
              isIvy2LocalLike = repoParams.isIvy2LocalLike,
              isCi = isCi,
              isSonatype = isSonatype,
              logger
            )
        }
        .sequence0
        .map { l =>
          val fs             = l.map(_._1).foldLeft(FileSet.empty)(_ ++ _)
          val modVersionOpt0 = l.headOption.map(_._2)
          (fs, modVersionOpt0)
        }
    }

    def getBouncyCastleSigner(
      secretKey: PasswordOption,
      secretKeyPasswordOpt: Option[PasswordOption]
    ) = {
      val getLauncher: Supplier[Array[String]] = { () =>
        val buildOptions = builds.headOption.map(_.options)
        val archiveCache = buildOptions.map(_.archiveCache)
          .getOrElse(ArchiveCache())
        val fileCache = buildOptions.map(_.finalCache).getOrElse(FileCache())
        PgpExternalCommand.launcher(
          fileCache,
          archiveCache,
          logger,
          () => builds.head.options.javaHome().value.javaCommand,
          publishOptions.signingCli
        ) match {
          case Left(e)              => throw new Exception(e)
          case Right(binaryCommand) => binaryCommand.toArray
        }
      }

      if (secretKeyPasswordOpt.isEmpty)
        logger.diagnostic("PGP signing with no password is not recommended since it's not stable")

      if (forceSigningBinary)
        (new scala.cli.internal.BouncycastleSignerMakerSubst).get(
          secretKeyPasswordOpt.fold(null)(_.toCliSigning),
          secretKey.toCliSigning,
          getLauncher,
          logger
        )
      else
        (new BouncycastleSignerMaker).get(
          secretKeyPasswordOpt.fold(null)(_.toCliSigning),
          secretKey.toCliSigning,
          getLauncher,
          logger
        )
    }

    val signerKind: PSigner = publishOptions.contextual(isCi).signer.getOrElse {
      if (!repoParams.supportsSig)
        PSigner.Nop
      else if (publishOptions.contextual(isCi).gpgSignatureId.isDefined)
        PSigner.Gpg
      else if (repoParams.shouldSign || publishOptions.contextual(isCi).secretKey.isDefined)
        PSigner.BouncyCastle
      else
        PSigner.Nop
    }

    def getSecretKeyPasswordOpt: Option[PasswordOption] = {
      val maybeSecretKeyPass = if (publishOptions.contextual(isCi).secretKeyPassword.isDefined)
        for {
          secretKeyPassConfigOpt <- publishOptions.contextual(isCi).secretKeyPassword
          secretKeyPass          <- secretKeyPassConfigOpt.get(configDb()).toOption
        } yield secretKeyPass
      else
        for {
          secretKeyPassOpt <- configDb().get(Keys.pgpSecretKeyPassword).toOption
          secretKeyPass    <- secretKeyPassOpt
        } yield secretKeyPass

      maybeSecretKeyPass
    }

    val signer: Either[BuildException, Signer] = signerKind match {
      // user specified --signer=gpg or --gpgKey=...
      case PSigner.Gpg =>
        publishOptions.contextual(isCi).gpgSignatureId.map { gpgSignatureId =>
          GpgSigner(
            GpgSigner.Key.Id(gpgSignatureId),
            extraOptions = publishOptions.contextual(isCi).gpgOptions
          )
        }.toRight(new MissingPublishOptionError(
          "ID of the GPG key",
          "--gpgKey",
          directiveName = ""
        ))

      // user specified --signer=bc or --secret-key=... or target repository requires signing
      // --secret-key-password is possibly specified (not mandatory)
      case PSigner.BouncyCastle
          if publishOptions.contextual(isCi).secretKey.isDefined =>
        val secretKeyConfigOpt = publishOptions.contextual(isCi).secretKey.get
        for {
          secretKey <- secretKeyConfigOpt.get(configDb())
        } yield getBouncyCastleSigner(secretKey, getSecretKeyPasswordOpt)

      // user specified --signer=bc or target repository requires signing
      // --secret-key-password is possibly specified (not mandatory)
      case PSigner.BouncyCastle =>
        val shouldSignMsg =
          if (repoParams.shouldSign) "signing is required for chosen repository" else ""
        for {
          secretKeyOpt <- configDb().get(Keys.pgpSecretKey).wrapConfigException
          secretKey <- secretKeyOpt.toRight(
            new MissingPublishOptionError(
              "secret key",
              "--secret-key",
              directiveName = "",
              configKeys = Seq(Keys.pgpSecretKey.fullName),
              extraMessage = shouldSignMsg
            )
          )
        } yield getBouncyCastleSigner(secretKey, getSecretKeyPasswordOpt)
      case _ =>
        if (!publishOptions.contextual(isCi).signer.contains(PSigner.Nop))
          logger.message(
            " \ud83d\udd13 Artifacts NOT signed as it's not required nor has it been specified"
          )
        Right(NopSigner)
    }

    val signerLogger =
      new InteractiveSignerLogger(new OutputStreamWriter(System.err), verbosity = 1)
    val signRes = value(signer).signatures(
      fileSet0,
      now,
      ChecksumType.all.map(_.extension).toSet,
      Set("maven-metadata.xml"),
      signerLogger
    )

    val fileSet1 = value {
      signRes
        .left.map {
          case (path, content, err) =>
            val path0 = content.pathOpt
              .map(os.Path(_, Os.pwd))
              .toRight(path.repr)
            new FailedToSignFileError(path0, err)
        }
        .map { signatures =>
          fileSet0 ++ signatures
        }
    }

    val checksumLogger =
      new InteractiveChecksumLogger(new OutputStreamWriter(System.err), verbosity = 1)
    val checksumTypes = publishOptions.contextual(isCi).checksums match {
      case None =>
        if (repoParams.acceptsChecksums) Seq(ChecksumType.MD5, ChecksumType.SHA1)
        else Nil
      case Some(Seq("none")) => Nil
      case Some(inputs) =>
        value {
          inputs
            .map(ChecksumType.parse)
            .sequence
            .left.map(errors => new MalformedChecksumsError(inputs, errors))
        }
    }
    val checksums = Checksums(
      checksumTypes,
      fileSet1,
      now,
      ec,
      checksumLogger
    ).unsafeRun()(ec)
    val fileSet2 = fileSet1 ++ checksums

    val finalFileSet =
      if (repoParams.isIvy2LocalLike) fileSet2
      else fileSet2.order(ec).unsafeRun()(ec)

    val isSnapshot0 = modVersionOpt.exists(_._2.endsWith("SNAPSHOT"))
    val authOpt0    = value(authOpt(repoParams.repo.repo(isSnapshot0).root))
    if (repoParams.shouldAuthenticate && authOpt0.isEmpty)
      logger.diagnostic(
        "Publishing to a repository that needs authentication, but no credentials are available.",
        Severity.Warning
      )
    val repoParams0 = repoParams.withAuth(authOpt0)
    val hooksDataOpt = Option.when(!dummy) {
      try repoParams0.hooks.beforeUpload(finalFileSet, isSnapshot0).unsafeRun()(ec)
      catch {
        case NonFatal(e) =>
          throw new Exception(e)
      }
    }

    val retainedRepo = hooksDataOpt match {
      case None => // dummy mode
        repoParams0.repo.repo(isSnapshot0)
      case Some(hooksData) =>
        repoParams0.hooks.repository(hooksData, repoParams0.repo, isSnapshot0)
          .getOrElse(repoParams0.repo.repo(isSnapshot0))
    }

    val baseUpload =
      if (retainedRepo.root.startsWith("http://") || retainedRepo.root.startsWith("https://"))
        HttpURLConnectionUpload.create()
      else
        FileUpload(Paths.get(new URI(retainedRepo.root)))

    val upload =
      if (dummy) DummyUpload(baseUpload)
      else baseUpload

    val isLocal      = true
    val uploadLogger = InteractiveUploadLogger.create(System.err, dummy = dummy, isLocal = isLocal)

    val errors =
      try
        upload.uploadFileSet(
          retainedRepo,
          finalFileSet,
          uploadLogger,
          if (parallelUpload.getOrElse(repoParams.defaultParallelUpload)) Some(ec) else None
        ).unsafeRun()(ec)
      catch {
        case NonFatal(e) =>
          // Wrap exception from coursier, as it sometimes throws exceptions from other threads,
          // which lack the current stacktrace.
          throw new Exception(e)
      }

    errors.toList match {
      case h :: t =>
        value(Left(new UploadError(::(h, t))))
      case Nil =>
        for (hooksData <- hooksDataOpt)
          try repoParams0.hooks.afterUpload(hooksData).unsafeRun()(ec)
          catch {
            case NonFatal(e) =>
              throw new Exception(e)
          }
        for ((mod, version) <- modVersionOpt) {
          val checkRepo = repoParams0.repo.checkResultsRepo(isSnapshot0)
          val relPath = {
            val elems =
              if (repoParams.isIvy2LocalLike)
                Seq(mod.organization.value, mod.name.value, version)
              else
                mod.organization.value.split('.').toSeq ++ Seq(mod.name.value, version)
            elems.mkString("/", "/", "/")
          }
          val path = {
            val url = checkRepo.root.stripSuffix("/") + relPath
            if (url.startsWith("file:")) {
              val path = os.Path(Paths.get(new URI(url)), os.pwd)
              if (path.startsWith(os.pwd))
                path.relativeTo(os.pwd).segments.map(_ + File.separator).mkString
              else if (path.startsWith(os.home))
                ("~" +: path.relativeTo(os.home).segments).map(_ + File.separator).mkString
              else
                path.toString
            }
            else url
          }
          if (dummy)
            println("\n \ud83d\udc40 You could have checked results at")
          else
            println("\n \ud83d\udc40 Check results at")
          println(s"  $path")
          for (targetRepo <- repoParams.targetRepoOpt if !isSnapshot0) {
            val url = targetRepo.stripSuffix("/") + relPath
            if (dummy)
              println("before they would have landed at")
            else
              println("before they land at")
            println(s"  $url")
          }
        }
    }
  }
}
