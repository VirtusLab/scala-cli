package scala.cli.commands.publish

import caseapp.core.RemainingArgs
import coursier.cache.ArchiveCache
import coursier.core.{Authentication, Configuration}
import coursier.maven.MavenRepository
import coursier.publish.checksum.logger.InteractiveChecksumLogger
import coursier.publish.checksum.{ChecksumType, Checksums}
import coursier.publish.fileset.{FileSet, Path}
import coursier.publish.signing.logger.InteractiveSignerLogger
import coursier.publish.signing.{GpgSigner, NopSigner, Signer}
import coursier.publish.sonatype.SonatypeApi
import coursier.publish.upload.logger.InteractiveUploadLogger
import coursier.publish.upload.{FileUpload, HttpURLConnectionUpload}
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
import scala.build.errors.{BuildException, CompositeBuildException, NoMainClassFoundError}
import scala.build.internal.Util
import scala.build.internal.Util.ScalaDependencyOps
import scala.build.options.publish.{ComputeVersion, Developer, License, Signer => PSigner, Vcs}
import scala.build.options.{BuildOptions, ConfigMonoid, PublishContextualOptions, Scope}
import scala.cli.CurrentParams
import scala.cli.commands.pgp.PgpExternalCommand
import scala.cli.commands.publish.ConfigUtil._
import scala.cli.commands.publish.{PublishParamsOptions, PublishRepositoryOptions}
import scala.cli.commands.util.CommonOps.SharedDirectoriesOptionsOps
import scala.cli.commands.util.MainClassOptionsUtil.*
import scala.cli.commands.util.PublishUtils.*
import scala.cli.commands.util.SharedOptionsUtil.*
import scala.cli.commands.util.{BuildCommandHelpers, ScalaCliSttpBackend}
import scala.cli.commands.{
  MainClassOptions,
  Package => PackageCmd,
  ScalaCommand,
  SharedOptions,
  WatchUtil
}
import scala.cli.config.{ConfigDb, Keys}
import scala.cli.errors.{
  FailedToSignFileError,
  MalformedChecksumsError,
  MissingPublishOptionError,
  UploadError
}
import scala.cli.packaging.Library
import scala.cli.publish.BouncycastleSignerMaker
import scala.cli.util.ConfigPasswordOptionHelpers.*

object Publish extends ScalaCommand[PublishOptions] with BuildCommandHelpers {

  override def group: String         = "Main"
  override def isRestricted: Boolean = true
  override def sharedOptions(options: PublishOptions): Option[SharedOptions] =
    Some(options.shared)

  def mkBuildOptions(
    baseOptions: BuildOptions,
    publishParams: PublishParamsOptions,
    sharedPublish: SharedPublishOptions,
    publishRepo: PublishRepositoryOptions,
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
          ))
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

  override def runCommand(options: PublishOptions, args: RemainingArgs): Unit = {
    maybePrintLicensesAndExit(options.publishParams)
    maybePrintChecksumsAndExit(options.sharedPublish)

    CurrentParams.verbosity = options.shared.logging.verbosity
    val baseOptions = buildOptionsOrExit(options)
    val logger      = options.shared.logger
    val inputs      = options.shared.inputs(args.all).orExit(logger)
    CurrentParams.workspaceOpt = Some(inputs.workspace)

    val initialBuildOptions = mkBuildOptions(
      baseOptions,
      options.publishParams,
      options.sharedPublish,
      options.publishRepo,
      options.mainClass,
      options.ivy2LocalLike
    ).orExit(logger)
    val threads = BuildThreads.create()

    val compilerMaker    = options.shared.compilerMaker(threads).orExit(logger)
    val docCompilerMaker = options.shared.compilerMaker(threads, scaladoc = true).orExit(logger)

    val cross = options.compileCross.cross.getOrElse(false)

    lazy val configDb = options.shared.configDb

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
      options.mainClass
    )
  }

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
    mainClassOptions: MainClassOptions
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
            mainClassOptions
          )
        }
      }
      try WatchUtil.waitForCtrlC()
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
        mainClassOptions
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
      s"Using directive publish.name not set, using workspace file name $name as default name"
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
    mainClassOptions: MainClassOptions
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
          case _ => doPublish(
              builds0,
              docBuilds0,
              workingDir,
              ivy2HomeOpt,
              publishLocal,
              logger,
              forceSigningBinary,
              parallelUpload,
              isCi,
              configDb
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

  private def buildFileSet(
    build: Build.Successful,
    docBuildOpt: Option[Build.Successful],
    workingDir: os.Path,
    now: Instant,
    isIvy2LocalLike: Boolean,
    isCi: Boolean,
    logger: Logger
  ): Either[BuildException, (FileSet, (coursier.core.Module, String))] = either {

    logger.debug(s"Preparing project ${build.project.projectName}")

    val publishOptions = build.options.notForBloopOptions.publishOptions

    lazy val orgNameOpt = GitRepo.maybeGhRepoOrgName(build.inputs.workspace, logger)

    val org = publishOptions.organization match {
      case Some(org0) => org0.value
      case None       => value(defaultOrganization(orgNameOpt.map(_._1), logger))
    }

    val moduleName = publishOptions.moduleName match {
      case Some(name0) => name0.value
      case None =>
        val name = publishOptions.name match {
          case Some(name0) => name0.value
          case None        => defaultName(build.inputs.workspace, logger)
        }
        build.artifacts.scalaOpt.map(_.params) match {
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

    val ver = publishOptions.version match {
      case Some(ver0) => ver0.value
      case None =>
        val computeVer = publishOptions.contextual(isCi).computeVersion.orElse {
          def isGitRepo = GitRepo.gitRepoOpt(build.inputs.workspace).isDefined
          val default   = defaultComputeVersion(!isCi && isGitRepo)
          if (default.isDefined)
            logger.message(
              s"Using directive ${defaultVersionError.directiveName} not set, assuming git:tag as publish.computeVersion"
            )
          default
        }
        value {
          computeVer match {
            case Some(cv) => cv.get(build.inputs.workspace)
            case None     => defaultVersion
          }
        }
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
      os.write(dest, content, createFolders = true)
      dest
    }

    val sourceJarOpt =
      if (publishOptions.contextual(isCi).sourceJar.getOrElse(true)) {
        val content   = PackageCmd.sourceJar(build, now.toEpochMilli)
        val sourceJar = workingDir / org / s"$moduleName-$ver-sources.jar"
        os.write(sourceJar, content, createFolders = true)
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
            os.copy(docJarPath, docJar, createFolders = true)
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

  private final case class RepoParams(
    repo: PublishRepository,
    targetRepoOpt: Option[String],
    hooks: Hooks,
    isIvy2LocalLike: Boolean,
    defaultParallelUpload: Boolean,
    supportsSig: Boolean,
    acceptsChecksums: Boolean
  )

  private def doPublish(
    builds: Seq[Build.Successful],
    docBuilds: Seq[Build.Successful],
    workingDir: os.Path,
    ivy2HomeOpt: Option[os.Path],
    publishLocal: Boolean,
    logger: Logger,
    forceSigningBinary: Boolean,
    parallelUpload: Option[Boolean],
    isCi: Boolean,
    configDb: () => ConfigDb
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

    val repoParams = {

      lazy val es =
        Executors.newSingleThreadScheduledExecutor(Util.daemonThreadFactory("publish-retry"))

      def authOpt(repo: String): Either[BuildException, Option[Authentication]] = either {
        val hostOpt = {
          val uri = new URI(repo)
          if (uri.getScheme == "https") Some(uri.getHost)
          else None
        }
        val isSonatype =
          hostOpt.exists(host => host == "oss.sonatype.org" || host.endsWith(".oss.sonatype.org"))
        val passwordOpt = publishOptions.contextual(isCi).repoPassword match {
          case None if isSonatype =>
            value(configDb().get(Keys.sonatypePassword).wrapConfigException)
          case other => other.map(_.toConfig)
        }
        passwordOpt.map(_.get()) match {
          case None => None
          case Some(password) =>
            val userOpt = publishOptions.contextual(isCi).repoUser match {
              case None if isSonatype =>
                value(configDb().get(Keys.sonatypeUser).wrapConfigException)
              case other => other.map(_.toConfig)
            }
            val realmOpt = publishOptions.contextual(isCi).repoRealm match {
              case None if isSonatype =>
                Some("Sonatype Nexus Repository Manager")
              case other => other
            }
            val auth = Authentication(userOpt.fold("")(_.get().value), password.value)
            Some(realmOpt.fold(auth)(auth.withRealm))
        }
      }

      def centralRepo(base: String) = either {
        val authOpt0 = value(authOpt(base))
        val repo0 = {
          val r = PublishRepository.Sonatype(MavenRepository(base))
          authOpt0.fold(r)(r.withAuthentication)
        }
        val backend = ScalaCliSttpBackend.httpURLConnection(logger)
        val api     = SonatypeApi(backend, base + "/service/local", authOpt0, logger.verbosity)
        val hooks0 = Hooks.sonatype(
          repo0,
          api,
          logger.compilerOutputStream, // meh
          logger.verbosity,
          batch = coursier.paths.Util.useAnsiOutput(), // FIXME Get via logger
          es
        )
        RepoParams(repo0, Some("https://repo1.maven.org/maven2"), hooks0, false, true, true, true)
      }

      def gitHubRepoFor(org: String, name: String) =
        RepoParams(
          PublishRepository.Simple(MavenRepository(s"https://maven.pkg.github.com/$org/$name")),
          None,
          Hooks.dummy,
          false,
          false,
          false,
          false
        )

      def gitHubRepo = either {
        val orgNameFromVcsOpt = publishOptions.versionControl
          .map(_.url)
          .flatMap(url => GitRepo.maybeGhOrgName(url))

        val (org, name) = orgNameFromVcsOpt match {
          case Some(orgName) => orgName
          case None =>
            value(GitRepo.ghRepoOrgName(builds.head.inputs.workspace, logger))
        }

        gitHubRepoFor(org, name)
      }

      def ivy2Local = {
        val home = ivy2HomeOpt.getOrElse(os.home / ".ivy2")
        val base = home / "local"
        // not really a Maven repo…
        RepoParams(
          PublishRepository.Simple(MavenRepository(base.toNIO.toUri.toASCIIString)),
          None,
          Hooks.dummy,
          true,
          true,
          true,
          true
        )
      }

      if (publishLocal)
        ivy2Local
      else
        publishOptions.contextual(isCi).repository match {
          case None =>
            value(Left(new MissingPublishOptionError(
              "repository",
              "--publish-repository",
              "publish.repository"
            )))
          case Some("ivy2-local") =>
            ivy2Local
          case Some("central" | "maven-central" | "mvn-central") =>
            value(centralRepo("https://oss.sonatype.org"))
          case Some("central-s01" | "maven-central-s01" | "mvn-central-s01") =>
            value(centralRepo("https://s01.oss.sonatype.org"))
          case Some("github") =>
            value(gitHubRepo)
          case Some(repoStr) if repoStr.startsWith("github:") && repoStr.count(_ == '/') == 1 =>
            val (org, name) = repoStr.stripPrefix("github:").split('/') match {
              case Array(org0, name0) => (org0, name0)
              case other              => sys.error(s"Cannot happen ('$repoStr' -> ${other.toSeq})")
            }
            gitHubRepoFor(org, name)
          case Some(repoStr) =>
            val repo0 = {
              val r = RepositoryParser.repositoryOpt(repoStr)
                .collect {
                  case m: MavenRepository =>
                    m
                }
                .getOrElse {
                  val url =
                    if (repoStr.contains("://")) repoStr
                    else os.Path(repoStr, Os.pwd).toNIO.toUri.toASCIIString
                  MavenRepository(url)
                }
              r.withAuthentication(value(authOpt(r.root)))
            }

            RepoParams(
              PublishRepository.Simple(repo0),
              None,
              Hooks.dummy,
              publishOptions.contextual(isCi).repositoryIsIvy2LocalLike.getOrElse(false),
              true,
              true,
              true
            )
        }
    }

    val now = Instant.now()
    val (fileSet0, modVersionOpt) = value {
      it
        // TODO Allow to add test JARs to the main build artifacts
        .filter(_._1.scope != Scope.Test)
        .map {
          case (build, docBuildOpt) =>
            buildFileSet(
              build,
              docBuildOpt,
              workingDir,
              now,
              isIvy2LocalLike = repoParams.isIvy2LocalLike,
              isCi = isCi,
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

    val signerOpt = publishOptions.contextual(isCi).signer.orElse {
      if (repoParams.supportsSig)
        if (publishOptions.contextual(isCi).secretKey.isDefined) Some(PSigner.BouncyCastle)
        else if (publishOptions.contextual(isCi).gpgSignatureId.isDefined) Some(PSigner.Gpg)
        else None
      else None
    }
    val signer: Signer = signerOpt match {
      case Some(PSigner.Gpg) =>
        publishOptions.contextual(isCi).gpgSignatureId match {
          case Some(gpgSignatureId) =>
            GpgSigner(
              GpgSigner.Key.Id(gpgSignatureId),
              extraOptions = publishOptions.contextual(isCi).gpgOptions
            )
          case None => NopSigner
        }
      case Some(PSigner.BouncyCastle) =>
        publishOptions.contextual(isCi).secretKey match {
          case Some(secretKey0) =>
            val getLauncher: Supplier[Array[String]] = { () =>
              val archiveCache = builds.headOption
                .map(_.options.archiveCache)
                .getOrElse(ArchiveCache())
              PgpExternalCommand.launcher(
                archiveCache,
                None,
                logger,
                () => builds.head.options.javaHome().value.javaCommand
              ) match {
                case Left(e)       => throw new Exception(e)
                case Right(binary) => binary.command.toArray
              }
            }
            val secretKey = secretKey0.get(configDb()).orExit(logger)
            if (forceSigningBinary)
              (new scala.cli.internal.BouncycastleSignerMakerSubst).get(
                publishOptions
                  .contextual(isCi)
                  .secretKeyPassword
                  .orNull
                  .get(configDb())
                  .orExit(logger)
                  .toCliSigning,
                secretKey.toCliSigning,
                getLauncher,
                logger
              )
            else
              (new BouncycastleSignerMaker).get(
                publishOptions
                  .contextual(isCi)
                  .secretKeyPassword
                  .orNull
                  .get(configDb())
                  .orExit(logger)
                  .toCliSigning,
                secretKey.toCliSigning,
                getLauncher,
                logger
              )
          case None => NopSigner
        }
      case Some(PSigner.Nop) => NopSigner
      case None              => NopSigner
    }
    val signerLogger =
      new InteractiveSignerLogger(new OutputStreamWriter(System.err), verbosity = 1)
    val signRes = signer.signatures(
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
    val hooksData   = repoParams.hooks.beforeUpload(finalFileSet, isSnapshot0).unsafeRun()(ec)

    val retainedRepo = repoParams.hooks.repository(hooksData, repoParams.repo, isSnapshot0)
      .getOrElse(repoParams.repo.repo(isSnapshot0))

    val upload =
      if (retainedRepo.root.startsWith("http://") || retainedRepo.root.startsWith("https://"))
        HttpURLConnectionUpload.create()
      else
        FileUpload(Paths.get(new URI(retainedRepo.root)))

    val dummy        = false
    val isLocal      = true
    val uploadLogger = InteractiveUploadLogger.create(System.err, dummy = dummy, isLocal = isLocal)

    val errors =
      upload.uploadFileSet(
        retainedRepo,
        finalFileSet,
        uploadLogger,
        if (parallelUpload.getOrElse(repoParams.defaultParallelUpload)) Some(ec) else None
      ).unsafeRun()(ec)

    errors.toList match {
      case h :: t =>
        value(Left(new UploadError(::(h, t))))
      case Nil =>
        repoParams.hooks.afterUpload(hooksData).unsafeRun()(ec)
        for ((mod, version) <- modVersionOpt) {
          val checkRepo = repoParams.repo.checkResultsRepo(isSnapshot0)
          val relPath = {
            val elems =
              if (repoParams.isIvy2LocalLike)
                Seq(mod.organization.value, mod.name.value, version)
              else
                mod.organization.value.split('.').toSeq ++ Seq(mod.name.value, version)
            elems.map("/" + _).mkString
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
          println("\n \ud83d\udc40 Check results at")
          println(s"  $path")
          for (targetRepo <- repoParams.targetRepoOpt if !isSnapshot0) {
            val url = targetRepo.stripSuffix("/") + relPath
            println("before they land at")
            println(s"  $url")
          }
        }
    }
  }
}
