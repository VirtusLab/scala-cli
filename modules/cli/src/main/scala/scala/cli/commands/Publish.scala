package scala.cli.commands

import caseapp.core.RemainingArgs
import coursier.cache.ArchiveCache
import coursier.core.Configuration
import coursier.maven.MavenRepository
import coursier.publish.checksum.logger.InteractiveChecksumLogger
import coursier.publish.checksum.{ChecksumType, Checksums}
import coursier.publish.fileset.{FileSet, Path}
import coursier.publish.signing.logger.InteractiveSignerLogger
import coursier.publish.signing.{GpgSigner, NopSigner, Signer}
import coursier.publish.upload.logger.InteractiveUploadLogger
import coursier.publish.upload.{FileUpload, HttpURLConnectionUpload}
import coursier.publish.{Content, Pom}

import java.io.OutputStreamWriter
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.{Path => NioPath, Paths}
import java.time.Instant
import java.util.function.Supplier

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException, NoMainClassFoundError}
import scala.build.internal.Util.ScalaDependencyOps
import scala.build.options.PublishOptions.{Signer => PSigner}
import scala.build.options.{BuildOptions, ConfigMonoid, PublishOptions => BPublishOptions, Scope}
import scala.build.{Build, BuildThreads, Builds, Logger, Os, Positioned}
import scala.cli.CurrentParams
import scala.cli.commands.pgp.PgpExternalCommand
import scala.cli.commands.util.SharedOptionsUtil._
import scala.cli.errors.{FailedToSignFileError, MissingRepositoryError, UploadError}
import scala.cli.packaging.Library
import scala.cli.publish.BouncycastleSignerMaker

object Publish extends ScalaCommand[PublishOptions] {

  override def group      = "Main"
  override def inSipScala = false
  override def sharedOptions(options: PublishOptions) =
    Some(options.shared)

  def mkBuildOptions(ops: PublishOptions): Either[BuildException, BuildOptions] = either {
    import ops._
    val baseOptions = shared.buildOptions(enableJmh = false, jmhVersion = None)
    baseOptions.copy(
      mainClass = mainClass.mainClass.filter(_.nonEmpty),
      notForBloopOptions = baseOptions.notForBloopOptions.copy(
        publishOptions = baseOptions.notForBloopOptions.publishOptions.copy(
          organization = organization.map(_.trim).filter(_.nonEmpty).map(Positioned.commandLine(_)),
          name = moduleName.map(_.trim).filter(_.nonEmpty).map(Positioned.commandLine(_)),
          version = version.map(_.trim).filter(_.nonEmpty).map(Positioned.commandLine(_)),
          url = url.map(_.trim).filter(_.nonEmpty).map(Positioned.commandLine(_)),
          license = value {
            license
              .map(_.trim).filter(_.nonEmpty)
              .map(Positioned.commandLine(_))
              .map(BPublishOptions.parseLicense(_))
              .sequence
          },
          versionControl = value {
            vcs.map(_.trim).filter(_.nonEmpty)
              .map(Positioned.commandLine(_))
              .map(BPublishOptions.parseVcs(_))
              .sequence
          },
          description = description.map(_.trim).filter(_.nonEmpty),
          developers = value {
            developer.filter(_.trim.nonEmpty)
              .map(Positioned.commandLine(_))
              .map(BPublishOptions.parseDeveloper(_))
              .sequence
              .left.map(CompositeBuildException(_))
          },
          scalaVersionSuffix = scalaVersionSuffix.map(_.trim),
          scalaPlatformSuffix = scalaPlatformSuffix.map(_.trim),
          repository = publishRepository.filter(_.trim.nonEmpty),
          sourceJar = sources,
          docJar = doc,
          gpgSignatureId = gpgKey.map(_.trim).filter(_.nonEmpty),
          gpgOptions = gpgOption,
          secretKey = secretKey.filter(_.trim.nonEmpty).map(os.Path(_, Os.pwd)),
          secretKeyPassword = secretKeyPassword,
          signer = value {
            signer
              .map(Positioned.commandLine(_))
              .map(BPublishOptions.parseSigner(_))
              .sequence
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

    val logger              = options.shared.logger
    val initialBuildOptions = mkBuildOptions(options).orExit(logger)
    val threads             = BuildThreads.create()

    val compilerMaker    = options.shared.compilerMaker(threads)
    val docCompilerMaker = options.shared.compilerMaker(threads, scaladoc = true)

    val cross = options.compileCross.cross.getOrElse(false)

    lazy val workingDir = options.workingDir
      .filter(_.trim.nonEmpty)
      .map(os.Path(_, Os.pwd))
      .getOrElse {
        os.temp.dir(
          prefix = "scala-cli-publish-",
          deleteOnExit = true
        )
      }

    if (options.watch.watch) {
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
          maybePublish(builds, workingDir, logger, allowExit = false)
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
      maybePublish(builds, workingDir, logger, allowExit = true)
    }
  }

  def defaultOrganization: Either[BuildException, String] =
    Right("default")
  def defaultName: Either[BuildException, String] =
    Right("default")
  def defaultVersion: Either[BuildException, String] =
    Right("0.1.0-SNAPSHOT")

  private def maybePublish(
    builds: Builds,
    workingDir: os.Path,
    logger: Logger,
    allowExit: Boolean
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
      val res = doPublish(builds0, docBuilds0, workingDir, logger)
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
    logger: Logger
  ): Either[BuildException, FileSet] = either {

    logger.debug(s"Preparing project ${build.project.projectName}")

    val publishOptions = build.options.notForBloopOptions.publishOptions

    val org = publishOptions.organization match {
      case Some(org0) => org0.value
      case None       => value(defaultOrganization)
    }
    val name = publishOptions.name match {
      case Some(name0) => name0.value
      case None        => value(defaultName)
    }
    val ver = publishOptions.version match {
      case Some(ver0) => ver0.value
      case None       => value(defaultVersion)
    }

    val dependencies = build.artifacts.userDependencies.map { dep =>
      val dep0 = dep.toCs(build.artifacts.params)
      val config =
        if (build.scope == Scope.Main) None
        else Some(Configuration(build.scope.name))
      (dep0.module.organization, dep0.module.name, dep0.version, config)
    }

    val fullName = {
      val params = build.artifacts.params
      val pf = publishOptions.scalaPlatformSuffix.getOrElse {
        // FIXME Allow full cross version too
        "_" + params.scalaBinaryVersion
      }
      val sv = publishOptions.scalaVersionSuffix.getOrElse {
        params.platform.fold("")("_" + _)
      }
      name + pf + sv
    }

    val mainClassOpt = build.options.mainClass.orElse {
      build.retainedMainClass match {
        case Left(_: NoMainClassFoundError) => None
        case Left(err) =>
          logger.debug(s"Error while looking for main class: $err")
          None
        case Right(cls) => Some(cls)
      }
    }
    val mainJarContent = Library.libraryJar(build, mainClassOpt)
    val mainJar        = workingDir / org / s"$fullName-$ver.jar"
    os.write(mainJar, mainJarContent, createFolders = true)

    val pomContent = Pom.create(
      organization = coursier.Organization(org),
      moduleName = coursier.ModuleName(fullName),
      version = ver,
      packaging = None,
      url = publishOptions.url.map(_.value),
      name = Some(name), // ?
      dependencies = dependencies,
      description = publishOptions.description,
      license = publishOptions.license.map(_.value).map { l =>
        Pom.License(l.name, l.url)
      },
      scm = publishOptions.versionControl.map { vcs =>
        Pom.Scm(vcs.url, vcs.connection, vcs.developerConnection)
      },
      developers = publishOptions.developers.map { dev =>
        Pom.Developer(dev.id, dev.name, dev.url, dev.mail)
      }
    )

    val sourceJarOpt =
      if (publishOptions.sourceJar.getOrElse(true)) {
        val content   = Package.sourceJar(build, now.toEpochMilli)
        val sourceJar = workingDir / org / s"$fullName-$ver-sources.jar"
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
            val content = value(Package.docJar(docBuild, logger, Nil))
            val docJar  = workingDir / org / s"$fullName-$ver-javadoc.jar"
            os.write(docJar, content, createFolders = true)
            Some(docJar)
        }
      else
        None

    val basePath = Path(org.split('.').toSeq ++ Seq(fullName, ver))

    val mainEntries = Seq(
      (basePath / s"$fullName-$ver.pom") -> Content.InMemory(
        now,
        pomContent.getBytes(StandardCharsets.UTF_8)
      ),
      (basePath / s"$fullName-$ver.jar") -> Content.File(mainJar.toNIO)
    )

    val sourceJarEntries = sourceJarOpt
      .map { sourceJar =>
        (basePath / s"$fullName-$ver-sources.jar") -> Content.File(sourceJar.toNIO)
      }
      .toSeq

    val docJarEntries = docJarOpt
      .map { docJar =>
        (basePath / s"$fullName-$ver-javadoc.jar") -> Content.File(docJar.toNIO)
      }
      .toSeq

    // TODO version listings, …
    FileSet(mainEntries ++ sourceJarEntries ++ docJarEntries)
  }

  private def doPublish(
    builds: Seq[Build.Successful],
    docBuilds: Seq[Build.Successful],
    workingDir: os.Path,
    logger: Logger
  ): Either[BuildException, Unit] = either {

    assert(docBuilds.isEmpty || docBuilds.length == builds.length)

    val it = builds.iterator.zip {
      if (docBuilds.isEmpty) Iterator.continually(None)
      else docBuilds.iterator.map(Some(_))
    }

    val now = Instant.now()
    val fileSet0 = value {
      it
        // TODO Allow to add test JARs to the main build artifacts
        .filter(_._1.scope != Scope.Test)
        .map {
          case (build, docBuildOpt) =>
            buildFileSet(build, docBuildOpt, workingDir, now, logger)
        }
        .toVector
        .sequence
        .left.map(CompositeBuildException(_))
        .map(_.foldLeft(FileSet.empty)(_ ++ _))
    }

    val ec = builds.head.options.finalCache.ec

    val signerOpt = ConfigMonoid.sum(
      builds.map(_.options.notForBloopOptions.publishOptions.signer)
    )
    val signer: Signer = signerOpt match {
      case Some(PSigner.Gpg) =>
        val gpgSignatureIdOpt = ConfigMonoid.sum(
          builds.map(_.options.notForBloopOptions.publishOptions.gpgSignatureId)
        )
        gpgSignatureIdOpt match {
          case Some(gpgSignatureId) =>
            val gpgOptions =
              builds.toList.flatMap(_.options.notForBloopOptions.publishOptions.gpgOptions)
            GpgSigner(
              GpgSigner.Key.Id(gpgSignatureId),
              extraOptions = gpgOptions
            )
          case None => NopSigner
        }
      case Some(PSigner.BouncyCastle) =>
        val secretKeyOpt = ConfigMonoid.sum(
          builds.map(_.options.notForBloopOptions.publishOptions.secretKey)
        )
        secretKeyOpt match {
          case Some(secretKey) =>
            val passwordOpt = ConfigMonoid.sum(
              builds.map(_.options.notForBloopOptions.publishOptions.secretKeyPassword)
            )
            val getLauncher: Supplier[NioPath] = { () =>
              val archiveCache = builds.headOption
                .map(_.options.archiveCache)
                .getOrElse(ArchiveCache())
              PgpExternalCommand.launcher(archiveCache, None, logger) match {
                case Left(e)      => throw new Exception(e)
                case Right(value) => value.wrapped
              }
            }
            (new BouncycastleSignerMaker).get(
              passwordOpt.orNull,
              secretKey.wrapped,
              getLauncher,
              logger
            )
          case None => NopSigner
        }
      case None => NopSigner
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
    val checksums = Checksums(
      Seq(ChecksumType.MD5, ChecksumType.SHA1),
      fileSet1,
      now,
      ec,
      checksumLogger
    ).unsafeRun()(ec)
    val fileSet2 = fileSet1 ++ checksums

    val finalFileSet = fileSet2.order(ec).unsafeRun()(ec)

    val repoUrl = builds.head.options.notForBloopOptions.publishOptions.repository match {
      case None =>
        value(Left(new MissingRepositoryError))
      case Some(repo) =>
        if (repo.contains("://")) repo
        else os.Path(repo, Os.pwd).toNIO.toUri.toASCIIString
    }
    val repo = MavenRepository(repoUrl)

    val upload =
      if (repo.root.startsWith("http")) HttpURLConnectionUpload.create()
      else FileUpload(Paths.get(new URI(repo.root)))

    val dummy        = false
    val isLocal      = true
    val uploadLogger = InteractiveUploadLogger.create(System.err, dummy = dummy, isLocal = isLocal)

    val errors =
      upload.uploadFileSet(repo, finalFileSet, uploadLogger, Some(ec)).unsafeRun()(ec)

    errors.toList match {
      case h :: t =>
        value(Left(new UploadError(::(h, t))))
      case Nil =>
    }
  }
}
