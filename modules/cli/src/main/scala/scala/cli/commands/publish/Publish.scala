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

import java.io.OutputStreamWriter
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.{Path => NioPath, Paths}
import java.time.Instant
import java.util.concurrent.Executors
import java.util.function.Supplier

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException, NoMainClassFoundError}
import scala.build.internal.Util
import scala.build.internal.Util.ScalaDependencyOps
import scala.build.options.publish.{ComputeVersion, Developer, License, Signer => PSigner, Vcs}
import scala.build.options.{BuildOptions, ConfigMonoid, Scope}
import scala.build.{Build, BuildThreads, Builds, Logger, Os, Positioned}
import scala.cli.CurrentParams
import scala.cli.commands.pgp.PgpExternalCommand
import scala.cli.commands.util.ScalaCliSttpBackend
import scala.cli.commands.util.SharedOptionsUtil._
import scala.cli.commands.{Package => PackageCmd, ScalaCommand, WatchUtil}
import scala.cli.errors.{FailedToSignFileError, MissingPublishOptionError, UploadError}
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
          organization = sharedPublish.organization.map(_.trim).filter(_.nonEmpty).map(
            Positioned.commandLine(_)
          ),
          name = sharedPublish.name.map(_.trim).filter(_.nonEmpty).map(Positioned.commandLine(_)),
          moduleName =
            sharedPublish.moduleName.map(_.trim).filter(_.nonEmpty).map(Positioned.commandLine(_)),
          version =
            sharedPublish.version.map(_.trim).filter(_.nonEmpty).map(Positioned.commandLine(_)),
          url = sharedPublish.url.map(_.trim).filter(_.nonEmpty).map(Positioned.commandLine(_)),
          license = value {
            sharedPublish.license
              .map(_.trim).filter(_.nonEmpty)
              .map(Positioned.commandLine(_))
              .map(License.parse(_))
              .sequence
          },
          versionControl = value {
            sharedPublish.vcs
              .map(_.trim).filter(_.nonEmpty)
              .map(Positioned.commandLine(_))
              .map(Vcs.parse(_))
              .sequence
          },
          description = sharedPublish.description.map(_.trim).filter(_.nonEmpty),
          developers = value {
            sharedPublish.developer
              .filter(_.trim.nonEmpty)
              .map(Positioned.commandLine(_))
              .map(Developer.parse(_))
              .sequence
              .left.map(CompositeBuildException(_))
          },
          scalaVersionSuffix = scalaVersionSuffix.map(_.trim),
          scalaPlatformSuffix = scalaPlatformSuffix.map(_.trim),
          repository = sharedPublish.publishRepository.filter(_.trim.nonEmpty),
          sourceJar = sources,
          docJar = doc,
          gpgSignatureId = gpgKey.map(_.trim).filter(_.nonEmpty),
          gpgOptions = gpgOption,
          secretKey = sharedPublish.secretKey,
          secretKeyPassword = sharedPublish.secretKeyPassword,
          repoUser = sharedPublish.user,
          repoPassword = sharedPublish.password,
          signer = value {
            signer
              .map(Positioned.commandLine(_))
              .map(PSigner.parse(_))
              .sequence
          },
          computeVersion = value {
            sharedPublish.computeVersion
              .map(Positioned.commandLine(_))
              .map(ComputeVersion.parse(_))
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
    Left(new MissingPublishOptionError("organization", "--organization", "publish.organization"))
  def defaultName: Either[BuildException, String] =
    Left(new MissingPublishOptionError("name", "--name", "publish.name"))
  def defaultVersion: Either[BuildException, String] =
    Left(new MissingPublishOptionError("version", "--version", "publish.version"))

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
  ): Either[BuildException, (FileSet, String)] = either {

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

    val mainJar = {
      val mainClassOpt = build.options.mainClass.orElse {
        build.retainedMainClass match {
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

    val pomContent = {

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

      Pom.create(
        organization = coursier.Organization(org),
        moduleName = coursier.ModuleName(moduleName),
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
    }

    val fileSet = {

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

    (fileSet, ver)
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

    val publishOptions = ConfigMonoid.sum(
      builds.map(_.options.notForBloopOptions.publishOptions)
    )

    val ec = builds.head.options.finalCache.ec

    val (repo, hooks) = {

      lazy val es =
        Executors.newSingleThreadScheduledExecutor(Util.daemonThreadFactory("publish-retry"))

      lazy val authOpt = {
        val userOpt     = publishOptions.repoUser
        val passwordOpt = publishOptions.repoPassword.map(_.get())
        passwordOpt.map { password =>
          Authentication(userOpt.fold("")(_.get().value), password.value)
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
        (repo0, hooks0)
      }

      publishOptions.repository match {
        case None =>
          value(Left(new MissingPublishOptionError(
            "repository",
            "--publish-repository",
            "publish.repository"
          )))
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
          (PublishRepository.Simple(repo0), Hooks.dummy)
      }
    }

    val now = Instant.now()
    val (fileSet0, versionOpt) = value {
      it
        // TODO Allow to add test JARs to the main build artifacts
        .filter(_._1.scope != Scope.Test)
        .map {
          case (build, docBuildOpt) =>
            buildFileSet(build, docBuildOpt, workingDir, now, logger)
        }
        .sequence0
        .map { l =>
          val fs          = l.map(_._1).foldLeft(FileSet.empty)(_ ++ _)
          val versionOpt0 = l.headOption.map(_._2)
          (fs, versionOpt0)
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
            (new BouncycastleSignerMaker).get(
              publishOptions.secretKeyPassword.orNull,
              secretKey,
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

    val isSnapshot0 = versionOpt.exists(_.endsWith("SNAPSHOT"))
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
      upload.uploadFileSet(retainedRepo, finalFileSet, uploadLogger, Some(ec)).unsafeRun()(ec)

    errors.toList match {
      case h :: t =>
        value(Left(new UploadError(::(h, t))))
      case Nil =>
        hooks.afterUpload(hooksData).unsafeRun()(ec)
    }
  }
}
