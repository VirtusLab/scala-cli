package scala.cli.commands.publish

import caseapp.core.RemainingArgs
import caseapp.core.help.HelpFormat
import coursier.core.{Authentication, Configuration}
import coursier.publish.checksum.logger.InteractiveChecksumLogger
import coursier.publish.checksum.{ChecksumType, Checksums}
import coursier.publish.fileset.{FileSet, Path}
import coursier.publish.signing.logger.InteractiveSignerLogger
import coursier.publish.signing.{NopSigner, Signer}
import coursier.publish.upload.logger.InteractiveUploadLogger
import coursier.publish.upload.{DummyUpload, FileUpload, HttpURLConnectionUpload, Upload}
import coursier.publish.{Content, Hooks, Pom}

import java.io.{File, OutputStreamWriter}
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.concurrent.Executors

import scala.build.*
import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.compiler.ScalaCompilerMaker
import scala.build.errors.{BuildException, CompositeBuildException, Severity}
import scala.build.input.Inputs
import scala.build.internal.Util
import scala.build.internal.Util.ScalaDependencyOps
import scala.build.options.publish.{Developer, License, Signer as PSigner, Vcs}
import scala.build.options.{
  BuildOptions,
  ComputeVersion,
  ConfigMonoid,
  PublishContextualOptions,
  ScalaSigningCliOptions,
  Scope
}
import scala.cli.CurrentParams
import scala.cli.commands.package0.Package as PackageCmd
import scala.cli.commands.pgp.PgpScalaSigningOptions
import scala.cli.commands.publish.ConfigUtil.*
import scala.cli.commands.publish.PublishUtils.*
import scala.cli.commands.shared.{
  HelpCommandGroup,
  HelpGroup,
  MainClassOptions,
  SharedOptions,
  SharedVersionOptions
}
import scala.cli.commands.util.BuildCommandHelpers
import scala.cli.commands.{ScalaCommand, SpecificationLevel, WatchUtil}
import scala.cli.config.{ConfigDb, Keys, PasswordOption, PublishCredentials}
import scala.cli.errors.{
  FailedToSignFileError,
  InvalidSonatypePublishCredentials,
  MalformedChecksumsError,
  MissingPublishOptionError,
  UploadError,
  WrongSonatypeServerError
}
import scala.cli.packaging.Library
import scala.cli.util.ArgHelpers.*
import scala.cli.util.ConfigDbUtils
import scala.cli.util.ConfigPasswordOptionHelpers.*
import scala.concurrent.duration.DurationInt
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
    sharedVersionOptions: SharedVersionOptions,
    publishParams: PublishParamsOptions,
    sharedPublish: SharedPublishOptions,
    publishRepo: PublishRepositoryOptions,
    scalaSigning: PgpScalaSigningOptions,
    publishConnection: PublishConnectionOptions,
    mainClass: MainClassOptions,
    ivy2LocalLike: Option[Boolean]
  ): Either[BuildException, BuildOptions] = either {
    val contextualOptions = PublishContextualOptions(
      repository = publishRepo.publishRepository.filter(_.trim.nonEmpty),
      repositoryIsIvy2LocalLike = ivy2LocalLike,
      sourceJar = sharedPublish.withSources,
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
        sharedVersionOptions.computeVersion
          .map(Positioned.commandLine)
          .map(ComputeVersion.parse)
          .sequence
      },
      checksums = {
        val input = sharedPublish.checksum.flatMap(_.split(",")).map(_.trim).filter(_.nonEmpty)
        if input.isEmpty then None else Some(input)
      },
      connectionTimeoutRetries = publishConnection.connectionTimeoutRetries,
      connectionTimeoutSeconds = publishConnection.connectionTimeoutSeconds,
      responseTimeoutSeconds = publishConnection.responseTimeoutSeconds,
      stagingRepoRetries = publishConnection.stagingRepoRetries,
      stagingRepoWaitTimeMilis = publishConnection.stagingRepoWaitTimeMilis
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
            sharedVersionOptions.projectVersion.map(_.trim).filter(_.nonEmpty).map(
              Positioned.commandLine
            ),
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
            if publishParams.isCi then PublishContextualOptions() else contextualOptions
          )),
          ci = ConfigMonoid.sum(Seq(
            baseOptions.notForBloopOptions.publishOptions.ci,
            if publishParams.isCi then contextualOptions else PublishContextualOptions()
          )),
          signingCli = ScalaSigningCliOptions(
            signingCliVersion = scalaSigning.signingCliVersion,
            forceExternal = scalaSigning.forceSigningExternally,
            forceJvm = scalaSigning.forceJvmSigningCli,
            javaArgs = scalaSigning.signingCliJavaArg
          )
        )
      )
    )
  }

  def maybePrintLicensesAndExit(params: PublishParamsOptions): Unit =
    if params.license.contains("list") then {
      for (l <- scala.build.internal.Licenses.list)
        println(s"${l.id}: ${l.name} (${l.url})")
      sys.exit(0)
    }

  def maybePrintChecksumsAndExit(options: SharedPublishOptions): Unit =
    if options.checksum.contains("list") then {
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
      baseOptions = baseOptions,
      sharedVersionOptions = options.shared.sharedVersionOptions,
      publishParams = options.publishParams,
      sharedPublish = options.sharedPublish,
      publishRepo = options.publishRepo,
      scalaSigning = options.signingCli,
      publishConnection = options.connectionOptions,
      mainClass = options.mainClass,
      ivy2LocalLike = options.ivy2LocalLike
    ).orExit(logger)
    val threads = BuildThreads.create()

    val compilerMaker       = options.shared.compilerMaker(threads)
    val docCompilerMakerOpt = options.sharedPublish.docCompilerMakerOpt

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
      docCompilerMakerOpt,
      cross,
      workingDir,
      ivy2HomeOpt,
      publishLocal = false,
      forceSigningExternally = options.signingCli.forceSigningExternally.getOrElse(false),
      parallelUpload = options.parallelUpload,
      options.watch.watch,
      isCi = options.publishParams.isCi,
      () => configDb,
      options.mainClass,
      dummy = options.sharedPublish.dummy,
      buildTests = options.shared.scope.test.getOrElse(false)
    )
  }

  /** Build artifacts
    */
  def doRun(
    inputs: Inputs,
    logger: Logger,
    initialBuildOptions: BuildOptions,
    compilerMaker: ScalaCompilerMaker,
    docCompilerMaker: Option[ScalaCompilerMaker],
    cross: Boolean,
    workingDir: => os.Path,
    ivy2HomeOpt: Option[os.Path],
    publishLocal: Boolean,
    forceSigningExternally: Boolean,
    parallelUpload: Option[Boolean],
    watch: Boolean,
    isCi: Boolean,
    configDb: () => ConfigDb,
    mainClassOptions: MainClassOptions,
    dummy: Boolean,
    buildTests: Boolean
  ): Unit = {
    val actionableDiagnostics = configDb().get(Keys.actions).getOrElse(None)

    if watch then {
      val watcher = Build.watch(
        inputs = inputs,
        options = initialBuildOptions,
        compilerMaker = compilerMaker,
        docCompilerMakerOpt = docCompilerMaker,
        logger = logger,
        crossBuilds = cross,
        buildTests = buildTests,
        partial = None,
        actionableDiagnostics = actionableDiagnostics,
        postAction = () => WatchUtil.printWatchMessage()
      ) {
        _.orReport(logger).foreach { builds =>
          maybePublish(
            builds = builds,
            workingDir = workingDir,
            ivy2HomeOpt = ivy2HomeOpt,
            publishLocal = publishLocal,
            logger = logger,
            allowExit = false,
            forceSigningExternally = forceSigningExternally,
            parallelUpload = parallelUpload,
            isCi = isCi,
            configDb = configDb,
            mainClassOptions = mainClassOptions,
            withTestScope = buildTests,
            dummy = dummy
          )
        }
      }
      try WatchUtil.waitForCtrlC(() => watcher.schedule())
      finally watcher.dispose()
    }
    else {
      val builds =
        Build.build(
          inputs = inputs,
          options = initialBuildOptions,
          compilerMaker = compilerMaker,
          docCompilerMakerOpt = docCompilerMaker,
          logger = logger,
          crossBuilds = cross,
          buildTests = buildTests,
          partial = None,
          actionableDiagnostics = actionableDiagnostics
        ).orExit(logger)
      maybePublish(
        builds = builds,
        workingDir = workingDir,
        ivy2HomeOpt = ivy2HomeOpt,
        publishLocal = publishLocal,
        logger = logger,
        allowExit = true,
        forceSigningExternally = forceSigningExternally,
        parallelUpload = parallelUpload,
        isCi = isCi,
        configDb = configDb,
        mainClassOptions = mainClassOptions,
        withTestScope = buildTests,
        dummy = dummy
      )
    }
  }

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
    forceSigningExternally: Boolean,
    parallelUpload: Option[Boolean],
    isCi: Boolean,
    configDb: () => ConfigDb,
    mainClassOptions: MainClassOptions,
    withTestScope: Boolean,
    dummy: Boolean
  ): Unit = {
    val allOk = builds.all.forall {
      case _: Build.Successful => true
      case _: Build.Cancelled  => false
      case _: Build.Failed     => false
    }
    if allOk then logger.log("All standard builds ok...")
    else {
      val failedBuilds    = builds.all.filterNot(_.success)
      val cancelledBuilds = builds.all.filter(_.cancelled)
      logger.log(
        s"Some standard builds were not successful (${failedBuilds.length} failed, ${cancelledBuilds.length} cancelled)."
      )
    }
    val allDocsOk = builds.allDoc.forall {
      case _: Build.Successful => true
      case _: Build.Cancelled  => true
      case _: Build.Failed     => false
    }
    if allDocsOk then logger.log("All doc builds ok...")
    else {
      val failedBuilds    = builds.allDoc.filterNot(_.success)
      val cancelledBuilds = builds.allDoc.filter(_.cancelled)
      logger.log(
        s"Some doc builds were not successful (${failedBuilds.length} failed, ${cancelledBuilds.length} cancelled)."
      )
    }
    if allOk && allDocsOk then {
      val builds0 = builds.all.collect {
        case s: Build.Successful => s
      }
      val docBuilds0 = builds.allDoc.collect {
        case s: Build.Successful => s
      }
      val res: Either[BuildException, Unit] =
        builds.builds match {
          case b if b.forall(_.success) && mainClassOptions.mainClassLs.contains(true) =>
            mainClassOptions.maybePrintMainClasses(
              mainClasses = builds0.flatMap(_.foundMainClasses()).distinct,
              shouldExit = allowExit
            )
          case _ => prepareFilesAndUpload(
              builds = builds0,
              docBuilds = docBuilds0,
              workingDir = workingDir,
              ivy2HomeOpt = ivy2HomeOpt,
              publishLocal = publishLocal,
              logger = logger,
              forceSigningExternally = forceSigningExternally,
              parallelUpload = parallelUpload,
              withTestScope = withTestScope,
              isCi = isCi,
              configDb = configDb,
              dummy = dummy
            )
        }
      if allowExit then res.orExit(logger) else res.orReport(logger)
    }
    else {
      val msg = if allOk then "Scaladoc generation failed" else "Compilation failed"
      System.err.println(msg)
      if allowExit then sys.exit(1)
    }
  }

  private def buildFileSet(
    builds: Seq[Build.Successful],
    docBuilds: Seq[Build.Successful],
    workingDir: os.Path,
    now: Instant,
    isIvy2LocalLike: Boolean,
    isCi: Boolean,
    isSonatype: Boolean,
    withTestScope: Boolean,
    logger: Logger
  ): Either[BuildException, (FileSet, (coursier.core.Module, String))] = either {
    logger.debug(s"Preparing project ${builds.head.project.projectName}")

    val publishOptions = builds.head.options.notForBloopOptions.publishOptions

    val ArtifactData(org, moduleName, ver) = value {
      publishOptions.artifactData(
        workspace = builds.head.inputs.workspace,
        logger = logger,
        scalaArtifactsOpt = builds.head.artifacts.scalaOpt,
        isCi = isCi
      )
    }

    logger.message(s"Publishing $org:$moduleName:$ver")

    val mainJar: os.Path = {
      val mainClassOpt: Option[String] =
        (builds.head.options.mainClass.filter(_.nonEmpty) match {
          case Some(cls) => Right(cls)
          case None      =>
            val potentialMainClasses = builds.flatMap(_.foundMainClasses()).distinct
            builds
              .map { build =>
                build.retainedMainClass(logger, potentialMainClasses)
                  .map(mainClass => build.scope -> mainClass)
              }
              .sequence
              .left
              .map(CompositeBuildException(_))
              .map(_.toMap)
              .map { retainedMainClassesByScope =>
                if retainedMainClassesByScope.size == 1 then retainedMainClassesByScope.head._2
                else
                  retainedMainClassesByScope
                    .get(Scope.Main)
                    .orElse(retainedMainClassesByScope.get(Scope.Test))
                    .get
              }

        }).toOption
      logger.debug(s"Retained main class: ${mainClassOpt.getOrElse("(no main class found)")}")
      val libraryJar: os.Path = Library.libraryJar(builds, mainClassOpt)
      val dest: os.Path       = workingDir / org / s"$moduleName-$ver.jar"
      logger.debug(s"Copying library jar from $libraryJar to $dest...")
      os.copy.over(libraryJar, dest, createFolders = true)
      logger.log(s"Successfully copied library jar from $libraryJar to $dest")
      dest
    }

    val sourceJarOpt =
      if publishOptions.contextual(isCi).sourceJar.getOrElse(true) then {
        val content            = PackageCmd.sourceJar(builds, now.toEpochMilli)
        val sourceJar: os.Path = workingDir / org / s"$moduleName-$ver-sources.jar"
        logger.debug(s"Saving source jar to $sourceJar...")
        os.write.over(sourceJar, content, createFolders = true)
        logger.log(s"Successfully saved source jar to $sourceJar")
        Some(sourceJar)
      }
      else None

    val docJarOpt =
      if publishOptions.contextual(isCi).docJar.getOrElse(true) then
        docBuilds match {
          case Nil       => None
          case docBuilds =>
            val docJarPath: os.Path = value(PackageCmd.docJar(
              builds = docBuilds,
              logger = logger,
              extraArgs = Nil,
              withTestScope = withTestScope
            ))
            val docJar: os.Path = workingDir / org / s"$moduleName-$ver-javadoc.jar"
            logger.debug(s"Copying doc jar from $docJarPath to $docJar...")
            os.copy.over(docJarPath, docJar, createFolders = true)
            logger.log(s"Successfully copied doc jar from $docJarPath to $docJar")
            Some(docJar)
        }
      else None

    val dependencies = builds.flatMap(_.artifacts.userDependencies)
      .map(_.toCs(builds.head.artifacts.scalaOpt.map(_.params)))
      .sequence
      .left.map(CompositeBuildException(_))
      .orExit(logger)
      .map { dep0 =>
        val config =
          builds -> builds.length match {
            case (b, 1) if b.head.scope != Scope.Main => Some(Configuration(b.head.scope.name))
            case _                                    => None
          }
        logger.debug(
          s"Dependency ${dep0.module.organization}:${dep0.module.name}:${dep0.versionConstraint.asString}"
        )
        (
          dep0.module.organization,
          dep0.module.name,
          dep0.versionConstraint.asString,
          config,
          dep0.minimizedExclusions
        )
      }
    val url = publishOptions.url.map(_.value)
    logger.debug(s"Published project URL: ${url.getOrElse("(not set)")}")
    val license = publishOptions.license.map(_.value).map { l =>
      Pom.License(l.name, l.url)
    }
    logger.debug(s"Published project license: ${license.map(_.name).getOrElse("(not set)")}")
    val scm = publishOptions.versionControl.map { vcs =>
      Pom.Scm(vcs.url, vcs.connection, vcs.developerConnection)
    }
    logger.debug(s"Published project SCM: ${scm.map(_.url).getOrElse("(not set)")}")
    val developers = publishOptions.developers.map { dev =>
      Pom.Developer(dev.id, dev.name, dev.url, dev.mail)
    }
    logger.debug(s"Published project developers: ${developers.map(_.name).mkString(", ")}")
    val description = publishOptions.description.getOrElse(moduleName)
    logger.debug(s"Published project description: $description")

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

    if isSonatype then {
      if url.isEmpty then
        logger.diagnostic(
          "Publishing to Sonatype, but project URL is empty (set it with the '//> using publish.url' directive)."
        )
      if license.isEmpty then
        logger.diagnostic(
          "Publishing to Sonatype, but license is empty (set it with the '//> using publish.license' directive)."
        )
      if scm.isEmpty then
        logger.diagnostic(
          "Publishing to Sonatype, but SCM details are empty (set them with the '//> using publish.scm' directive)."
        )
      if developers.isEmpty then
        logger.diagnostic(
          "Publishing to Sonatype, but developer details are empty (set them with the '//> using publish.developer' directive)."
        )
    }

    def ivyContent = Ivy.create(
      organization = coursier.Organization(org),
      moduleName = coursier.ModuleName(moduleName),
      version = ver,
      url = url,
      dependencies = dependencies,
      description = Some(description),
      time = LocalDateTime.ofInstant(now, ZoneOffset.UTC),
      hasDoc = docJarOpt.isDefined,
      hasSources = sourceJarOpt.isDefined
    )

    def mavenFileSet: FileSet = {
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

    def ivy2LocalLikeFileSet: FileSet = {
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

    val fileSet: FileSet = if isIvy2LocalLike then ivy2LocalLikeFileSet else mavenFileSet

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
    forceSigningExternally: Boolean,
    parallelUpload: Option[Boolean],
    withTestScope: Boolean,
    isCi: Boolean,
    configDb: () => ConfigDb,
    dummy: Boolean
  ): Either[BuildException, Unit] = either {
    assert(docBuilds.isEmpty || docBuilds.length == builds.length)

    extension (b: Seq[Build.Successful]) {
      private def groupedByCrossParams =
        b.groupBy(b =>
          b.options.scalaOptions.scalaVersion.map(_.asString).toString ->
            b.options.platform.toString
        )
    }
    val groupedBuilds    = builds.groupedByCrossParams
    val groupedDocBuilds = docBuilds.groupedByCrossParams
    val it: Iterator[(Seq[Build.Successful], Seq[Build.Successful])] =
      groupedBuilds.keysIterator.map { key =>
        (groupedBuilds(key), groupedDocBuilds.getOrElse(key, Seq.empty))
      }

    val publishOptions = ConfigMonoid.sum(
      builds.map(_.options.notForBloopOptions.publishOptions)
    )

    val ec = builds.head.options.finalCache.ec

    def authOpt(
      repo: String,
      isLegacySonatype: Boolean
    ): Either[BuildException, Option[Authentication]] =
      either {
        val publishCredentials: () => Option[PublishCredentials] =
          () => value(PublishUtils.getPublishCredentials(repo, configDb))
        for {
          password <- publishOptions.contextual(isCi).repoPassword
            .map(_.toConfig)
            .orElse(publishCredentials().flatMap(_.password))
            .map(_.get().value)
          user = publishOptions.contextual(isCi).repoUser
            .map(_.toConfig)
            .orElse(publishCredentials().flatMap(_.user))
            .map(_.get().value)
            .getOrElse("")
          auth = Authentication(user, password)
        } yield publishOptions.contextual(isCi).repoRealm
          .orElse {
            publishCredentials()
              .flatMap(_.realm)
              .orElse(if isLegacySonatype then Some("Sonatype Nexus Repository Manager") else None)
          }
          .map(auth.withRealm)
          .getOrElse(auth)
      }

    val repoParams: RepoParams = {
      lazy val es =
        Executors.newSingleThreadScheduledExecutor(Util.daemonThreadFactory("publish-retry"))

      if publishLocal then RepoParams.ivy2Local(ivy2HomeOpt)
      else
        value {
          publishOptions.contextual(isCi).repository match {
            case None       => Left(MissingPublishOptionError.repositoryError)
            case Some(repo) =>
              RepoParams(
                repo = repo,
                vcsUrlOpt = publishOptions.versionControl.map(_.url),
                workspace = builds.head.inputs.workspace,
                ivy2HomeOpt = ivy2HomeOpt,
                isIvy2LocalLike =
                  publishOptions.contextual(isCi).repositoryIsIvy2LocalLike.getOrElse(false),
                es = es,
                logger = logger,
                connectionTimeoutRetries = publishOptions.contextual(isCi).connectionTimeoutRetries,
                connectionTimeoutSeconds = publishOptions.contextual(isCi).connectionTimeoutSeconds,
                stagingRepoRetries = publishOptions.contextual(isCi).stagingRepoRetries,
                stagingRepoWaitTimeMilis = publishOptions.contextual(isCi).stagingRepoWaitTimeMilis
              )
          }
        }
    }

    val now                       = Instant.now()
    val (fileSet0, modVersionOpt) = value {
      it
        .map {
          case (builds, docBuilds) =>
            buildFileSet(
              builds = builds,
              docBuilds = docBuilds,
              workingDir = workingDir,
              now = now,
              isIvy2LocalLike = repoParams.isIvy2LocalLike,
              isCi = isCi,
              isSonatype = repoParams.isSonatype,
              withTestScope = withTestScope,
              logger = logger
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
    ) = PublishUtils.getBouncyCastleSigner(
      secretKey = secretKey,
      secretKeyPasswordOpt = secretKeyPasswordOpt,
      buildOptions = builds.headOption.map(_.options),
      forceSigningExternally = forceSigningExternally,
      logger = logger
    )

    val signerKind: PSigner = publishOptions.contextual(isCi).signer.getOrElse {
      if !repoParams.supportsSig then PSigner.Nop
      else if publishOptions.contextual(isCi).gpgSignatureId.isDefined then PSigner.Gpg
      else if repoParams.shouldSign || publishOptions.contextual(isCi).secretKey.isDefined then
        PSigner.BouncyCastle
      else PSigner.Nop
    }

    def getSecretKeyPasswordOpt: Option[PasswordOption] =
      publishOptions.contextual(isCi).getSecretKeyPasswordOpt(configDb)

    val signer: Either[BuildException, Signer] = signerKind match {
      // user specified --signer=gpg or --gpgKey=...
      case PSigner.Gpg => publishOptions.contextual(isCi).getGpgSigner

      // user specified --signer=bc or --secret-key=... or target repository requires signing
      // --secret-key-password is possibly specified (not mandatory)
      case PSigner.BouncyCastle
          if publishOptions.contextual(isCi).secretKey.isDefined =>
        val secretKeyConfigOpt = publishOptions.contextual(isCi).secretKey.get
        for { secretKey <- secretKeyConfigOpt.get(configDb()) } yield getBouncyCastleSigner(
          secretKey = secretKey,
          secretKeyPasswordOpt = getSecretKeyPasswordOpt
        )

      // user specified --signer=bc or target repository requires signing
      // --secret-key-password is possibly specified (not mandatory)
      case PSigner.BouncyCastle =>
        val shouldSignMsg =
          if repoParams.shouldSign then "signing is required for chosen repository" else ""
        for {
          secretKeyOpt <- configDb().get(Keys.pgpSecretKey).wrapConfigException
          secretKey    <- secretKeyOpt.toRight(
            new MissingPublishOptionError(
              name = "secret key",
              optionName = "--secret-key",
              directiveName = "",
              configKeys = Seq(Keys.pgpSecretKey.fullName),
              extraMessage = shouldSignMsg
            )
          )
        } yield getBouncyCastleSigner(secretKey, getSecretKeyPasswordOpt)
      case _ =>
        if !publishOptions.contextual(isCi).signer.contains(PSigner.Nop) then
          logger.message(
            " \ud83d\udd13 Artifacts NOT signed as it's not required nor has it been specified"
          )
        Right(NopSigner)
    }

    val signerLogger =
      new InteractiveSignerLogger(out = new OutputStreamWriter(System.err), verbosity = 1)
    val signRes: Either[(Path, Content, String), FileSet] = value(signer).signatures(
      fileSet = fileSet0,
      now = now,
      dontSignExtensions = ChecksumType.all.map(_.extension).toSet,
      dontSignFiles = Set("maven-metadata.xml"),
      logger = signerLogger
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
        if repoParams.acceptsChecksums then Seq(ChecksumType.MD5, ChecksumType.SHA1)
        else Nil
      case Some(Seq("none")) => Nil
      case Some(inputs)      =>
        value {
          inputs
            .map(ChecksumType.parse)
            .sequence
            .left.map(errors => new MalformedChecksumsError(inputs, errors))
        }
    }
    val checksums = Checksums(
      types = checksumTypes,
      fileSet = fileSet1,
      now = now,
      pool = ec,
      logger = checksumLogger
    ).unsafeRun()(using ec)
    val fileSet2 = fileSet1 ++ checksums

    val finalFileSet =
      if repoParams.isIvy2LocalLike then fileSet2
      else fileSet2.order(ec).unsafeRun()(using ec)

    val isSnapshot0 = modVersionOpt.exists(_._2.endsWith("SNAPSHOT"))
    if isSnapshot0 then logger.message("Publishing a SNAPSHOT version...")
    val authOpt0: Option[Authentication] = value(authOpt(
      repo = repoParams.repo.repo(isSnapshot0).root,
      isLegacySonatype = repoParams.isSonatype
    ))
    val asciiRegex        = """[\u0000-\u007f]*""".r
    val usernameOnlyAscii = authOpt0.exists(_.userOpt.exists(asciiRegex.matches))
    val passwordOnlyAscii = authOpt0.exists(_.passwordOpt.exists(asciiRegex.matches))

    if repoParams.shouldAuthenticate && authOpt0.isEmpty then
      logger.diagnostic(
        "Publishing to a repository that needs authentication, but no credentials are available.",
        Severity.Warning
      )
    val repoParams0: RepoParams = repoParams.withAuth(authOpt0)
    val isLegacySonatype        =
      repoParams0.isSonatype && !repoParams0.repo.releaseRepo.root.contains("s01")
    val hooksDataOpt = Option.when(!dummy) {
      try repoParams0.hooks.beforeUpload(finalFileSet, isSnapshot0).unsafeRun()(using ec)
      catch {
        case NonFatal(e)
            if "Failed to get .*oss\\.sonatype\\.org.*/staging/profiles \\(http status: 403,".r
              .unanchored.matches(
                e.getMessage
              ) =>
          logger.exit(new WrongSonatypeServerError(isLegacySonatype))
        case NonFatal(e)
            if "Failed to get .*oss\\.sonatype\\.org.*/staging/profiles \\(http status: 401,".r
              .unanchored.matches(
                e.getMessage
              ) =>
          logger.exit(new InvalidSonatypePublishCredentials(usernameOnlyAscii, passwordOnlyAscii))
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
      if retainedRepo.root.startsWith("http://") || retainedRepo.root.startsWith("https://") then
        HttpURLConnectionUpload.create(
          publishOptions.contextual(isCi)
            .connectionTimeoutSeconds.map(_.seconds.toMillis.toInt),
          publishOptions.contextual(isCi)
            .responseTimeoutSeconds.map(_.seconds.toMillis.toInt)
        )
      else
        FileUpload(Paths.get(new URI(retainedRepo.root)))

    val upload = if dummy then DummyUpload(baseUpload) else baseUpload

    val isLocal      = true
    val uploadLogger = InteractiveUploadLogger.create(System.err, dummy = dummy, isLocal = isLocal)

    val errors =
      try
        upload.uploadFileSet(
          repository = retainedRepo,
          fileSet = finalFileSet,
          logger = uploadLogger,
          parallel =
            if parallelUpload.getOrElse(repoParams.defaultParallelUpload) then Some(ec) else None
        ).unsafeRun()(using ec)
      catch {
        case NonFatal(e) =>
          // Wrap exception from coursier, as it sometimes throws exceptions from other threads,
          // which lack the current stacktrace.
          throw new Exception(e)
      }

    errors.toList match {
      case (h @ (_, _, e: Upload.Error.HttpError)) :: t
          if repoParams0.isSonatype && errors.distinctBy(_._3.getMessage()).size == 1 =>
        logger.log(s"Error message: ${e.getMessage}")
        val httpCodeRegex = "HTTP (\\d+)\n.*".r
        e.getMessage match {
          case httpCodeRegex("403") =>
            if logger.verbosity >= 2 then e.printStackTrace()
            logger.error(
              s"""
                 |Uploading files failed!
                 |Possible causes:
                 |- no rights to publish under this organization
                 |- organization name is misspelled
                 | -> have you registered your organisation yet?
                 |""".stripMargin
            )
            value(Left(new UploadError(::(h, t))))
          case _ => value(Left(new UploadError(::(h, t))))
        }
      case h :: t if repoParams0.isSonatype && errors.forall {
            case (_, _, _: Upload.Error.Unauthorized) => true
            case _                                    => false
          } =>
        logger.error(
          s"""
             |Uploading files failed!
             |Possible causes:
             |- incorrect Sonatype credentials
             |- incorrect Sonatype server was used, try ${
              if isLegacySonatype then "'central-s01'" else "'central'"
            }
             | -> consult publish subcommand documentation
             |""".stripMargin
        )
        value(Left(new UploadError(::(h, t))))
      case h :: t =>
        value(Left(new UploadError(::(h, t))))
      case Nil =>
        for (hooksData <- hooksDataOpt)
          try repoParams0.hooks.afterUpload(hooksData).unsafeRun()(using ec)
          catch {
            case NonFatal(e) =>
              throw new Exception(e)
          }
        for ((mod, version) <- modVersionOpt) {
          val checkRepo = repoParams0.repo.checkResultsRepo(isSnapshot0)
          val relPath   = {
            val elems =
              if repoParams.isIvy2LocalLike then
                Seq(mod.organization.value, mod.name.value, version)
              else mod.organization.value.split('.').toSeq ++ Seq(mod.name.value, version)
            elems.mkString("/", "/", "/")
          }
          val path = {
            val url = checkRepo.root.stripSuffix("/") + relPath
            if url.startsWith("file:") then {
              val path = os.Path(Paths.get(new URI(url)), os.pwd)
              if path.startsWith(os.pwd) then
                path.relativeTo(os.pwd).segments.map(_ + File.separator).mkString
              else if path.startsWith(os.home) then
                ("~" +: path.relativeTo(os.home).segments).map(_ + File.separator).mkString
              else path.toString
            }
            else url
          }
          if dummy then println("\n \ud83d\udc40 You could have checked results at")
          else println("\n \ud83d\udc40 Check results at")
          println(s"  $path")
          for (targetRepo <- repoParams.targetRepoOpt if !isSnapshot0) {
            val url = targetRepo.stripSuffix("/") + relPath
            if dummy then println("before they would have landed at")
            else println("before they land at")
            println(s"  $url")
          }
        }
    }
  }
}
