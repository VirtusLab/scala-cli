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
import java.nio.file.{Path => NioPath, Paths}
import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.concurrent.Executors
import java.util.function.Supplier

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
import scala.build._
import scala.build.compiler.ScalaCompilerMaker
import scala.build.errors.{BuildException, CompositeBuildException, NoMainClassFoundError}
import scala.build.internal.Util
import scala.build.internal.Util.ScalaDependencyOps
import scala.build.options.publish.{ComputeVersion, Developer, License, Signer => PSigner, Vcs}
import scala.build.options.{BuildOptions, ConfigMonoid, Scope}
import scala.cli.CurrentParams
import scala.cli.commands.pgp.PgpExternalCommand
import scala.cli.commands.publish.{PublishParamsOptions, PublishRepositoryOptions}
import scala.cli.commands.util.ScalaCliSttpBackend
import scala.cli.commands.util.SharedOptionsUtil._
import scala.cli.commands.{
  MainClassOptions,
  Package => PackageCmd,
  ScalaCommand,
  SharedOptions,
  WatchUtil
}
import scala.cli.errors.{
  FailedToSignFileError,
  MalformedChecksumsError,
  MissingPublishOptionError,
  UploadError
}
import scala.cli.packaging.Library
import scala.cli.publish.BouncycastleSignerMaker

object Publish extends ScalaCommand[PublishOptions] {

  override def group      = "Main"
  override def inSipScala = false
  override def sharedOptions(options: PublishOptions) =
    Some(options.shared)

  def mkBuildOptions(
    shared: SharedOptions,
    publishParams: PublishParamsOptions,
    sharedPublish: SharedPublishOptions,
    publishRepo: PublishRepositoryOptions,
    mainClass: MainClassOptions,
    ivy2LocalLike: Option[Boolean]
  ): Either[BuildException, BuildOptions] = either {
    val baseOptions = shared.buildOptions()
    baseOptions.copy(
      mainClass = mainClass.mainClass.filter(_.nonEmpty),
      notForBloopOptions = baseOptions.notForBloopOptions.copy(
        publishOptions = baseOptions.notForBloopOptions.publishOptions.copy(
          organization = publishParams.organization.map(_.trim).filter(_.nonEmpty).map(
            Positioned.commandLine(_)
          ),
          name = publishParams.name.map(_.trim).filter(_.nonEmpty).map(Positioned.commandLine(_)),
          moduleName =
            publishParams.moduleName.map(_.trim).filter(_.nonEmpty).map(Positioned.commandLine(_)),
          version =
            publishParams.version.map(_.trim).filter(_.nonEmpty).map(Positioned.commandLine(_)),
          url = publishParams.url.map(_.trim).filter(_.nonEmpty).map(Positioned.commandLine(_)),
          license = value {
            publishParams.license
              .map(_.trim).filter(_.nonEmpty)
              .map(Positioned.commandLine(_))
              .map(License.parse(_))
              .sequence
          },
          versionControl = value {
            publishParams.vcs
              .map(_.trim).filter(_.nonEmpty)
              .map(Positioned.commandLine(_))
              .map(Vcs.parse(_))
              .sequence
          },
          description = publishParams.description.map(_.trim).filter(_.nonEmpty),
          developers = value {
            publishParams.developer
              .filter(_.trim.nonEmpty)
              .map(Positioned.commandLine(_))
              .map(Developer.parse(_))
              .sequence
              .left.map(CompositeBuildException(_))
          },
          scalaVersionSuffix = sharedPublish.scalaVersionSuffix.map(_.trim),
          scalaPlatformSuffix = sharedPublish.scalaPlatformSuffix.map(_.trim),
          repository = publishRepo.publishRepository.filter(_.trim.nonEmpty),
          repositoryIsIvy2LocalLike = ivy2LocalLike,
          sourceJar = sharedPublish.sources,
          docJar = sharedPublish.doc,
          gpgSignatureId = sharedPublish.gpgKey.map(_.trim).filter(_.nonEmpty),
          gpgOptions = sharedPublish.gpgOption,
          secretKey = publishParams.secretKey,
          secretKeyPassword = publishParams.secretKeyPassword,
          repoUser = publishRepo.user,
          repoPassword = publishRepo.password,
          repoRealm = publishRepo.realm,
          signer = value {
            sharedPublish.signer
              .map(Positioned.commandLine(_))
              .map(PSigner.parse(_))
              .sequence
          },
          computeVersion = value {
            publishParams.computeVersion
              .map(Positioned.commandLine(_))
              .map(ComputeVersion.parse(_))
              .sequence
          },
          checksums = {
            val input = sharedPublish.checksum.flatMap(_.split(",")).map(_.trim).filter(_.nonEmpty)
            if (input.isEmpty) None
            else Some(input)
          }
        )
      )
    )
  }

  def run(options: PublishOptions, args: RemainingArgs): Unit = {
    maybePrintGroupHelp(options)

    CurrentParams.verbosity = options.shared.logging.verbosity
    val inputs = options.shared.inputsOrExit(args)
    CurrentParams.workspaceOpt = Some(inputs.workspace)

    val logger = options.shared.logger
    val initialBuildOptions = mkBuildOptions(
      options.shared,
      options.publishParams,
      options.sharedPublish,
      options.publishRepo,
      options.mainClass,
      options.ivy2LocalLike
    ).orExit(logger)
    val threads = BuildThreads.create()

    val compilerMaker    = options.shared.compilerMaker(threads)
    val docCompilerMaker = options.shared.compilerMaker(threads, scaladoc = true)

    val cross = options.compileCross.cross.getOrElse(false)

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
      parallelUpload = options.parallelUpload.getOrElse(true),
      options.watch.watch
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
    parallelUpload: Boolean,
    watch: Boolean
  ): Unit = {

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
            parallelUpload = parallelUpload
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
          partial = None
        ).orExit(logger)
      maybePublish(
        builds,
        workingDir,
        ivy2HomeOpt,
        publishLocal,
        logger,
        allowExit = true,
        forceSigningBinary = forceSigningBinary,
        parallelUpload = parallelUpload
      )
    }
  }

  def defaultOrganization: Either[BuildException, String] =
    Left(new MissingPublishOptionError("organization", "--organization", "publish.organization"))
  def defaultName: Either[BuildException, String] =
    Left(new MissingPublishOptionError("name", "--name", "publish.name"))
  def defaultVersion: Either[BuildException, String] =
    Left(new MissingPublishOptionError("version", "--version", "publish.version"))

  private def maybePublish(
    builds: Builds,
    workingDir: os.Path,
    ivy2HomeOpt: Option[os.Path],
    publishLocal: Boolean,
    logger: Logger,
    allowExit: Boolean,
    forceSigningBinary: Boolean,
    parallelUpload: Boolean
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
      val res = doPublish(
        builds0,
        docBuilds0,
        workingDir,
        ivy2HomeOpt,
        publishLocal,
        logger,
        forceSigningBinary,
        parallelUpload
      )
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
    logger: Logger
  ): Either[BuildException, (FileSet, (coursier.core.Module, String))] = either {

    logger.debug(s"Preparing project ${build.project.projectName}")

    val publishOptions = build.options.notForBloopOptions.publishOptions

    val org = publishOptions.organization match {
      case Some(org0) => org0.value
      case None       => value(defaultOrganization)
    }

    val moduleName = publishOptions.moduleName match {
      case Some(name0) => name0.value
      case None =>
        val name = publishOptions.name match {
          case Some(name0) => name0.value
          case None        => value(defaultName)
        }
        build.artifacts.scalaOpt.map(_.params) match {
          case Some(scalaParams) =>
            val pf = publishOptions.scalaPlatformSuffix.getOrElse {
              // FIXME Allow full cross version too
              "_" + scalaParams.scalaBinaryVersion
            }
            val sv = publishOptions.scalaVersionSuffix.getOrElse {
              scalaParams.platform.fold("")("_" + _)
            }
            name + pf + sv
          case None =>
            name
        }
    }

    val ver = publishOptions.version match {
      case Some(ver0) => ver0.value
      case None =>
        value {
          publishOptions.computeVersion match {
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
      if (publishOptions.sourceJar.getOrElse(true)) {
        val content   = PackageCmd.sourceJar(build, now.toEpochMilli)
        val sourceJar = workingDir / org / s"$moduleName-$ver-sources.jar"
        os.write(sourceJar, content, createFolders = true)
        Some(sourceJar)
      }
      else
        None

    val docJarOpt =
      if (publishOptions.docJar.getOrElse(true))
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
      hasPom = true,
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

  private def doPublish(
    builds: Seq[Build.Successful],
    docBuilds: Seq[Build.Successful],
    workingDir: os.Path,
    ivy2HomeOpt: Option[os.Path],
    publishLocal: Boolean,
    logger: Logger,
    forceSigningBinary: Boolean,
    parallelUpload: Boolean
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

    val (repo, targetRepoOpt, hooks, isIvy2LocalLike) = {

      lazy val es =
        Executors.newSingleThreadScheduledExecutor(Util.daemonThreadFactory("publish-retry"))

      lazy val authOpt = {
        val passwordOpt = publishOptions.repoPassword.map(_.get())
        passwordOpt.map { password =>
          val userOpt  = publishOptions.repoUser
          val realmOpt = publishOptions.repoRealm
          val auth     = Authentication(userOpt.fold("")(_.get().value), password.value)
          realmOpt.fold(auth)(auth.withRealm)
        }
      }

      def centralRepo(base: String) = {
        val repo0 = {
          val r = PublishRepository.Sonatype(MavenRepository(base))
          authOpt.fold(r)(r.withAuthentication)
        }
        val backend = ScalaCliSttpBackend.httpURLConnection(logger)
        val api     = SonatypeApi(backend, base + "/service/local", authOpt, logger.verbosity)
        val hooks0 = Hooks.sonatype(
          repo0,
          api,
          logger.compilerOutputStream, // meh
          logger.verbosity,
          batch = coursier.paths.Util.useAnsiOutput(), // FIXME Get via logger
          es
        )
        (repo0, Some("https://repo1.maven.org/maven2"), hooks0, false)
      }

      def ivy2Local = {
        val home = ivy2HomeOpt.getOrElse(os.home / ".ivy2")
        val base = home / "local"
        // not really a Maven repo…
        (
          PublishRepository.Simple(MavenRepository(base.toNIO.toUri.toASCIIString)),
          None,
          Hooks.dummy,
          true
        )
      }

      if (publishLocal)
        ivy2Local
      else
        publishOptions.repository match {
          case None =>
            value(Left(new MissingPublishOptionError(
              "repository",
              "--publish-repository",
              "publish.repository"
            )))
          case Some("ivy2-local") =>
            ivy2Local
          case Some("central" | "maven-central" | "mvn-central") =>
            centralRepo("https://oss.sonatype.org")
          case Some("central-s01" | "maven-central-s01" | "mvn-central-s01") =>
            centralRepo("https://s01.oss.sonatype.org")
          case Some(repoStr) =>
            val repo0 = RepositoryParser.repositoryOpt(repoStr)
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
              .withAuthentication(authOpt)
            (
              PublishRepository.Simple(repo0),
              None,
              Hooks.dummy,
              publishOptions.repositoryIsIvy2LocalLike.getOrElse(false)
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
              isIvy2LocalLike = isIvy2LocalLike,
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

    val signerOpt = publishOptions.signer.orElse {
      if (publishOptions.secretKey.isDefined) Some(PSigner.BouncyCastle)
      else if (publishOptions.gpgSignatureId.isDefined) Some(PSigner.Gpg)
      else None
    }
    val signer: Signer = signerOpt match {
      case Some(PSigner.Gpg) =>
        publishOptions.gpgSignatureId match {
          case Some(gpgSignatureId) =>
            GpgSigner(
              GpgSigner.Key.Id(gpgSignatureId),
              extraOptions = publishOptions.gpgOptions
            )
          case None => NopSigner
        }
      case Some(PSigner.BouncyCastle) =>
        publishOptions.secretKey match {
          case Some(secretKey) =>
            val getLauncher: Supplier[NioPath] = { () =>
              val archiveCache = builds.headOption
                .map(_.options.archiveCache)
                .getOrElse(ArchiveCache())
              PgpExternalCommand.launcher(archiveCache, None, logger) match {
                case Left(e)      => throw new Exception(e)
                case Right(value) => value.wrapped
              }
            }
            if (forceSigningBinary)
              (new scala.cli.internal.BouncycastleSignerMakerSubst).get(
                publishOptions.secretKeyPassword.orNull,
                secretKey,
                getLauncher,
                logger
              )
            else
              (new BouncycastleSignerMaker).get(
                publishOptions.secretKeyPassword.orNull,
                secretKey,
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
    val checksumTypes = publishOptions.checksums match {
      case None              => Seq(ChecksumType.MD5, ChecksumType.SHA1)
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
      if (isIvy2LocalLike) fileSet2
      else fileSet2.order(ec).unsafeRun()(ec)

    val isSnapshot0 = modVersionOpt.exists(_._2.endsWith("SNAPSHOT"))
    val hooksData   = hooks.beforeUpload(finalFileSet, isSnapshot0).unsafeRun()(ec)

    val retainedRepo = hooks.repository(hooksData, repo, isSnapshot0)
      .getOrElse(repo.repo(isSnapshot0))

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
        if (parallelUpload) Some(ec) else None
      ).unsafeRun()(ec)

    errors.toList match {
      case h :: t =>
        value(Left(new UploadError(::(h, t))))
      case Nil =>
        hooks.afterUpload(hooksData).unsafeRun()(ec)
        for ((mod, version) <- modVersionOpt) {
          val checkRepo = repo.checkResultsRepo(isSnapshot0)
          val relPath = {
            val elems =
              if (isIvy2LocalLike)
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
          for (targetRepo <- targetRepoOpt if !isSnapshot0) {
            val url = targetRepo.stripSuffix("/") + relPath
            println("before they land at")
            println(s"  $url")
          }
        }
    }
  }
}
