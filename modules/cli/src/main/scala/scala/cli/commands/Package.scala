package scala.cli.commands

import ai.kien.python.Python
import caseapp.*
import coursier.launcher.*
import packager.config.*
import packager.deb.DebianPackage
import packager.docker.DockerPackage
import packager.mac.dmg.DmgPackage
import packager.mac.pkg.PkgPackage
import packager.rpm.RedHatPackage
import packager.windows.WindowsPackage

import java.io.{ByteArrayOutputStream, OutputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.attribute.FileTime
import java.util.zip.{ZipEntry, ZipOutputStream}

import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.*
import scala.build.errors.*
import scala.build.interactive.InteractiveFileOps
import scala.build.internal.Util.*
import scala.build.internal.{Runner, ScalaJsLinkerConfig}
import scala.build.options.{PackageType, Platform}
import scala.cli.CurrentParams
import scala.cli.commands.OptionsHelper.*
import scala.cli.commands.Run.orPythonDetectionError
import scala.cli.commands.packaging.Spark
import scala.cli.commands.util.BuildCommandHelpers
import scala.cli.commands.util.CommonOps.SharedDirectoriesOptionsOps
import scala.cli.commands.util.MainClassOptionsUtil.*
import scala.cli.commands.util.PackageOptionsUtil.*
import scala.cli.commands.util.SharedOptionsUtil.*
import scala.cli.config.{ConfigDb, Keys}
import scala.cli.errors.ScalaJsLinkingError
import scala.cli.internal.{CachedBinary, ProcUtil, ScalaJsLinker}
import scala.cli.packaging.{Library, NativeImage}
import scala.util.Properties

object Package extends ScalaCommand[PackageOptions] with BuildCommandHelpers {
  override def name                                                          = "package"
  override def group                                                         = "Main"
  override def inSipScala                                                    = false
  override def sharedOptions(options: PackageOptions): Option[SharedOptions] = Some(options.shared)
  def run(options: PackageOptions, args: RemainingArgs): Unit = {
    maybePrintGroupHelp(options)
    maybePrintSimpleScalacOutput(options, options.baseBuildOptions)

    CurrentParams.verbosity = options.shared.logging.verbosity
    val logger = options.shared.logger
    val inputs = options.shared.inputs(args.remaining).orExit(logger)
    CurrentParams.workspaceOpt = Some(inputs.workspace)

    // FIXME mainClass encoding has issues with special chars, such as '-'

    val initialBuildOptions = buildOptions(options)
    val threads             = BuildThreads.create()
    val compilerMaker       = options.compilerMaker(threads)
    val docCompilerMakerOpt = options.docCompilerMakerOpt

    val cross = options.compileCross.cross.getOrElse(false)
    val configDb = ConfigDb.open(options.shared.directories.directories)
      .orExit(logger)
    val actionableDiagnostics =
      options.shared.logging.verbosityOptions.actions.orElse(
        configDb.get(Keys.actions).getOrElse(None)
      )

    if (options.watch.watchMode) {
      var expectedModifyEpochSecondOpt = Option.empty[Long]
      val watcher = Build.watch(
        inputs,
        initialBuildOptions,
        compilerMaker,
        docCompilerMakerOpt,
        logger,
        crossBuilds = cross,
        buildTests = false,
        partial = None,
        actionableDiagnostics = actionableDiagnostics,
        postAction = () => WatchUtil.printWatchMessage()
      ) { res =>
        res.orReport(logger).map(_.main).foreach {
          case s: Build.Successful =>
            val mtimeDestPath = doPackage(
              logger = logger,
              outputOpt = options.output.filter(_.nonEmpty),
              force = options.force,
              forcedPackageTypeOpt = options.forcedPackageTypeOpt,
              build = s,
              extraArgs = args.unparsed,
              expectedModifyEpochSecondOpt = expectedModifyEpochSecondOpt,
              allowTerminate = !options.watch.watchMode,
              mainClassOptions = options.mainClass
            )
              .orReport(logger)
            for (valueOpt <- mtimeDestPath)
              expectedModifyEpochSecondOpt = valueOpt
          case _: Build.Failed =>
            System.err.println("Compilation failed")
          case _: Build.Cancelled =>
            System.err.println("Build cancelled")
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
          docCompilerMakerOpt,
          logger,
          crossBuilds = cross,
          buildTests = false,
          partial = None,
          actionableDiagnostics = actionableDiagnostics
        )
          .orExit(logger)
      builds.main match {
        case s: Build.Successful =>
          val res0 = doPackage(
            logger = logger,
            outputOpt = options.output.filter(_.nonEmpty),
            force = options.force,
            forcedPackageTypeOpt = options.forcedPackageTypeOpt,
            build = s,
            extraArgs = args.unparsed,
            expectedModifyEpochSecondOpt = None,
            allowTerminate = !options.watch.watchMode,
            mainClassOptions = options.mainClass
          )
          res0.orExit(logger)
        case _: Build.Failed =>
          System.err.println("Compilation failed")
          sys.exit(1)
        case _: Build.Cancelled =>
          System.err.println("Build cancelled")
          sys.exit(1)
      }
    }
  }

  def buildOptions(options: PackageOptions) = {
    val finalBuildOptions = options.finalBuildOptions.orExit(options.shared.logger)
    val buildOptions = finalBuildOptions.copy(javaOptions =
      finalBuildOptions.javaOptions.copy(javacOptions =
        finalBuildOptions.javaOptions.javacOptions ++ options.java.allJavaOpts
      )
    )
    buildOptions
  }

  private def doPackage(
    logger: Logger,
    outputOpt: Option[String],
    force: Boolean,
    forcedPackageTypeOpt: Option[PackageType],
    build: Build.Successful,
    extraArgs: Seq[String],
    expectedModifyEpochSecondOpt: Option[Long],
    allowTerminate: Boolean,
    mainClassOptions: MainClassOptions
  ): Either[BuildException, Option[Long]] = either {
    if (mainClassOptions.mainClassLs.contains(true))
      value {
        mainClassOptions
          .maybePrintMainClasses(build.foundMainClasses(), shouldExit = allowTerminate)
          .map(_ => None)
      }
    else {
      val packageType: PackageType = value {
        val basePackageTypeOpt = build.options.notForBloopOptions.packageOptions.packageTypeOpt
        lazy val validPackageScalaJS =
          Seq(PackageType.LibraryJar, PackageType.SourceJar, PackageType.DocJar)
        lazy val validPackageScalaNative =
          Seq(PackageType.LibraryJar, PackageType.SourceJar, PackageType.DocJar)

        forcedPackageTypeOpt -> build.options.platform.value match {
          case (Some(forcedPackageType), _) => Right(forcedPackageType)
          case (_, _) if build.options.notForBloopOptions.packageOptions.isDockerEnabled =>
            for (basePackageType <- basePackageTypeOpt)
              Left(new MalformedCliInputError(
                s"Unsupported package type: $basePackageType for Docker."
              ))
            Right(PackageType.Docker)
          case (_, Platform.JS) =>
            val validatedPackageType =
              for (basePackageType <- basePackageTypeOpt)
                yield
                  if (validPackageScalaJS.contains(basePackageType)) Right(basePackageType)
                  else Left(new MalformedCliInputError(
                    s"Unsupported package type: $basePackageType for Scala.js."
                  ))
            validatedPackageType.getOrElse(Right(PackageType.Js))
          case (_, Platform.Native) =>
            val validatedPackageType =
              for (basePackageType <- basePackageTypeOpt)
                yield
                  if (validPackageScalaNative.contains(basePackageType)) Right(basePackageType)
                  else Left(new MalformedCliInputError(
                    s"Unsupported package type: $basePackageType for Scala Native."
                  ))
            validatedPackageType.getOrElse(Right(PackageType.Native))
          case _ => Right(basePackageTypeOpt.getOrElse(PackageType.Bootstrap))
        }
      }

      // TODO When possible, call alreadyExistsCheck() before compiling stuff

      def extension = packageType match {
        case PackageType.LibraryJar                             => ".jar"
        case PackageType.SourceJar                              => ".jar"
        case PackageType.DocJar                                 => ".jar"
        case _: PackageType.Assembly                            => ".jar"
        case PackageType.Spark                                  => ".jar"
        case PackageType.Js                                     => ".js"
        case PackageType.Debian                                 => ".deb"
        case PackageType.Dmg                                    => ".dmg"
        case PackageType.Pkg                                    => ".pkg"
        case PackageType.Rpm                                    => ".rpm"
        case PackageType.Msi                                    => ".msi"
        case PackageType.Native if Properties.isWin             => ".exe"
        case PackageType.GraalVMNativeImage if Properties.isWin => ".exe"
        case _ if Properties.isWin                              => ".bat"
        case _                                                  => ""
      }

      def defaultName = packageType match {
        case PackageType.LibraryJar                             => "library.jar"
        case PackageType.SourceJar                              => "source.jar"
        case PackageType.DocJar                                 => "scaladoc.jar"
        case _: PackageType.Assembly                            => "app.jar"
        case PackageType.Spark                                  => "job.jar"
        case PackageType.Js                                     => "app.js"
        case PackageType.Debian                                 => "app.deb"
        case PackageType.Dmg                                    => "app.dmg"
        case PackageType.Pkg                                    => "app.pkg"
        case PackageType.Rpm                                    => "app.rpm"
        case PackageType.Msi                                    => "app.msi"
        case PackageType.Native if Properties.isWin             => "app.exe"
        case PackageType.GraalVMNativeImage if Properties.isWin => "app.exe"
        case _ if Properties.isWin                              => "app.bat"
        case _                                                  => "app"
      }

      val packageOutput = build.options.notForBloopOptions.packageOptions.output
      val dest = outputOpt.orElse(packageOutput)
        .orElse {
          build.sources.defaultMainClass
            .map(n => n.drop(n.lastIndexOf('.') + 1))
            .map(_.stripSuffix("_sc"))
            .map(_ + extension)
        }
        .orElse(build.retainedMainClass(logger).map(
          _.stripSuffix("_sc") + extension
        ).toOption)
        .orElse(build.sources.paths.collectFirst(_._1.baseName + extension))
        .getOrElse(defaultName)
      val destPath      = os.Path(dest, Os.pwd)
      val printableDest = CommandUtils.printablePath(destPath)

      def alreadyExistsCheck(): Unit = {
        val alreadyExists = !force &&
          os.exists(destPath) &&
          !expectedModifyEpochSecondOpt.contains(os.mtime(destPath))
        if (alreadyExists)
          InteractiveFileOps.erasingPath(build.options.interactive, printableDest, destPath) { () =>
            val errorMsg =
              if (expectedModifyEpochSecondOpt.isEmpty) s"$printableDest already exists"
              else s"$printableDest was overwritten by another process"
            System.err.println(s"Error: $errorMsg. Pass -f or --force to force erasing it.")
            sys.exit(1)
          }
      }

      alreadyExistsCheck()

      def mainClass: Either[BuildException, String] =
        build.options.mainClass match {
          case Some(cls) => Right(cls)
          case None      => build.retainedMainClass(logger)
        }

      def mainClassOpt: Option[String] =
        build.options.mainClass.orElse {
          build.retainedMainClassOpt(build.foundMainClasses(), logger)
        }

      val packageOptions = build.options.notForBloopOptions.packageOptions

      val outputPath = packageType match {
        case PackageType.Bootstrap =>
          bootstrap(build, destPath, value(mainClass), () => alreadyExistsCheck())
          destPath
        case PackageType.LibraryJar =>
          val content = Library.libraryJar(build)
          alreadyExistsCheck()
          if (force) os.write.over(destPath, content)
          else os.write(destPath, content)
          destPath
        case PackageType.SourceJar =>
          val now     = System.currentTimeMillis()
          val content = sourceJar(build, now)
          alreadyExistsCheck()
          if (force) os.write.over(destPath, content)
          else os.write(destPath, content)
          destPath
        case PackageType.DocJar =>
          val docJarPath = value(docJar(build, logger, extraArgs))
          alreadyExistsCheck()
          if (force) os.copy.over(docJarPath, destPath)
          else os.copy(docJarPath, destPath)
          destPath
        case a: PackageType.Assembly =>
          value {
            assembly(
              build,
              destPath,
              a.mainClassInManifest match {
                case None =>
                  if (a.addPreamble) {
                    val clsName = value {
                      mainClass.left.map {
                        case e: NoMainClassFoundError =>
                          // This one has a slightly better error message, suggesting --preamble=false
                          new NoMainClassFoundForAssemblyError(e)
                        case e => e
                      }
                    }
                    Some(clsName)
                  }
                  else
                    mainClassOpt
                case Some(false) => None
                case Some(true)  => Some(value(mainClass))
              },
              Nil,
              withPreamble = a.addPreamble,
              () => alreadyExistsCheck(),
              logger
            )
          }
          destPath
        case PackageType.Spark =>
          value {
            assembly(
              build,
              destPath,
              mainClassOpt,
              // The Spark modules are assumed to be already on the class path,
              // along with all their transitive dependencies (originating from
              // the Spark distribution), so we don't include any of them in the
              // assembly.
              Spark.sparkModules,
              withPreamble = false,
              () => alreadyExistsCheck(),
              logger
            )
          }
          destPath

        case PackageType.Js =>
          value(buildJs(build, destPath, value(mainClass), logger))

        case PackageType.Native =>
          value(buildNative(build, value(mainClass), destPath, logger))
          destPath

        case PackageType.GraalVMNativeImage =>
          NativeImage.buildNativeImage(
            build,
            value(mainClass),
            destPath,
            build.inputs.nativeImageWorkDir,
            extraArgs,
            logger
          )
          destPath

        case nativePackagerType: PackageType.NativePackagerType =>
          val bootstrapPath = os.temp.dir(prefix = "scala-packager") / "app"
          bootstrap(build, bootstrapPath, value(mainClass), () => alreadyExistsCheck())
          val sharedSettings = SharedSettings(
            sourceAppPath = bootstrapPath,
            version = packageOptions.packageVersion,
            force = force,
            outputPath = destPath,
            logoPath = packageOptions.logoPath,
            launcherApp = packageOptions.launcherApp
          )

          lazy val debianSettings = DebianSettings(
            shared = sharedSettings,
            maintainer = packageOptions.maintainer.mandatory("--maintainer", "debian"),
            description = packageOptions.description.mandatory("--description", "debian"),
            debianConflicts = packageOptions.debianOptions.conflicts,
            debianDependencies = packageOptions.debianOptions.dependencies,
            architecture = packageOptions.debianOptions.architecture.mandatory(
              "--deb-architecture",
              "debian"
            )
          )

          lazy val macOSSettings = MacOSSettings(
            shared = sharedSettings,
            identifier =
              packageOptions.macOSidentifier.mandatory("--identifier-parameter", "macOs")
          )

          lazy val redHatSettings = RedHatSettings(
            shared = sharedSettings,
            description = packageOptions.description.mandatory("--description", "redHat"),
            license =
              packageOptions.redHatOptions.license.mandatory("--license", "redHat"),
            release =
              packageOptions.redHatOptions.release.mandatory("--release", "redHat"),
            rpmArchitecture = packageOptions.redHatOptions.architecture.mandatory(
              "--rpm-architecture",
              "redHat"
            )
          )

          lazy val windowsSettings = WindowsSettings(
            shared = sharedSettings,
            maintainer = packageOptions.maintainer.mandatory("--maintainer", "windows"),
            licencePath = packageOptions.windowsOptions.licensePath.mandatory(
              "--licence-path",
              "windows"
            ),
            productName = packageOptions.windowsOptions.productName.mandatory(
              "--product-name",
              "windows"
            ),
            exitDialog = packageOptions.windowsOptions.exitDialog,
            suppressValidation =
              packageOptions.windowsOptions.suppressValidation.getOrElse(false),
            extraConfigs = packageOptions.windowsOptions.extraConfig,
            is64Bits = packageOptions.windowsOptions.is64Bits.getOrElse(true),
            installerVersion = packageOptions.windowsOptions.installerVersion
          )

          nativePackagerType match {
            case PackageType.Debian =>
              DebianPackage(debianSettings).build()
            case PackageType.Dmg =>
              DmgPackage(macOSSettings).build()
            case PackageType.Pkg =>
              PkgPackage(macOSSettings).build()
            case PackageType.Rpm =>
              RedHatPackage(redHatSettings).build()
            case PackageType.Msi =>
              WindowsPackage(windowsSettings).build()
          }
          destPath
        case PackageType.Docker =>
          value(docker(build, value(mainClass), logger))
          destPath
      }

      val printableOutput = CommandUtils.printablePath(outputPath)

      if (packageType.runnable.nonEmpty)
        logger.message {
          if (packageType.runnable.contains(true))
            s"Wrote $outputPath, run it with" + System.lineSeparator() +
              "  " + printableOutput
          else if (packageType == PackageType.Js)
            s"Wrote $outputPath, run it with" + System.lineSeparator() +
              "  node " + printableOutput
          else
            s"Wrote $outputPath"
        }

      val mTimeDestPathOpt = if (packageType.runnable.isEmpty) None else Some(os.mtime(destPath))
      mTimeDestPathOpt
    }
    // end of doPackage
  }

  def docJar(
    build: Build.Successful,
    logger: Logger,
    extraArgs: Seq[String]
  ): Either[BuildException, os.Path] = either {

    val workDir = build.inputs.docJarWorkDir
    val dest    = workDir / "doc.jar"
    val cacheData =
      CachedBinary.getCacheData(
        build,
        extraArgs.toList,
        dest,
        workDir
      )

    if (cacheData.changed) {

      val contentDir = value(Doc.generateScaladocDirPath(build, logger, extraArgs))

      var outputStream: OutputStream = null
      try {
        outputStream = os.write.outputStream(dest, createFolders = true)
        Library.writeLibraryJarTo(
          outputStream,
          build,
          hasActualManifest = false,
          contentDirOverride = Some(contentDir)
        )
      }
      finally
        if (outputStream != null)
          outputStream.close()

      CachedBinary.updateProjectAndOutputSha(dest, workDir, cacheData.projectSha)
    }

    dest
  }

  private val generatedSourcesPrefix = os.rel / "META-INF" / "generated"
  def sourceJar(build: Build.Successful, defaultLastModified: Long): Array[Byte] = {

    val baos                 = new ByteArrayOutputStream
    var zos: ZipOutputStream = null

    def fromSimpleSources = build.sources.paths.iterator.map {
      case (path, relPath) =>
        val lastModified = os.mtime(path)
        val content      = os.read.bytes(path)
        (relPath, content, lastModified)
    }

    def fromGeneratedSources = build.sources.inMemory.iterator.flatMap { inMemSource =>
      val lastModified = inMemSource.originalPath match {
        case Right((_, origPath)) => os.mtime(origPath)
        case Left(_)              => defaultLastModified
      }
      val originalOpt = inMemSource.originalPath.toOption.collect {
        case (subPath, origPath) if subPath != inMemSource.generatedRelPath =>
          val origContent = os.read.bytes(origPath)
          (subPath, origContent, lastModified)
      }
      val prefix = if (originalOpt.isEmpty) os.rel else generatedSourcesPrefix
      val generated = (
        prefix / inMemSource.generatedRelPath,
        inMemSource.generatedContent.getBytes(StandardCharsets.UTF_8),
        lastModified
      )
      Iterator(generated) ++ originalOpt.iterator
    }

    def paths = fromSimpleSources ++ fromGeneratedSources

    try {
      zos = new ZipOutputStream(baos)
      for ((relPath, content, lastModified) <- paths) {
        val name = relPath.toString
        val ent  = new ZipEntry(name)
        ent.setLastModifiedTime(FileTime.fromMillis(lastModified))
        ent.setSize(content.length)

        zos.putNextEntry(ent)
        zos.write(content)
        zos.closeEntry()
      }
    }
    finally if (zos != null) zos.close()

    baos.toByteArray
  }

  private def docker(
    build: Build.Successful,
    mainClass: String,
    logger: Logger
  ): Either[BuildException, Unit] = either {
    val packageOptions = build.options.notForBloopOptions.packageOptions

    if (build.options.platform.value == Platform.Native && (Properties.isMac || Properties.isWin)) {
      System.err.println(
        "Package scala native application to docker image is not supported on MacOs and Windows"
      )
      sys.exit(1)
    }

    val exec = build.options.platform.value match {
      case Platform.JVM    => Some("sh")
      case Platform.JS     => Some("node")
      case Platform.Native => None
    }
    val from = packageOptions.dockerOptions.from.getOrElse {
      build.options.platform.value match {
        case Platform.JVM    => "openjdk:17-slim"
        case Platform.JS     => "node"
        case Platform.Native => "debian:stable-slim"
      }
    }
    val repository = packageOptions.dockerOptions.imageRepository.mandatory(
      "--docker-image-repository",
      "docker"
    )
    val tag = packageOptions.dockerOptions.imageTag.getOrElse("latest")

    val dockerSettings = DockerSettings(
      from = from,
      registry = packageOptions.dockerOptions.imageRegistry,
      repository = repository,
      tag = Some(tag),
      exec = exec,
      dockerExecutable = None
    )

    val appPath = os.temp.dir(prefix = "scala-cli-docker") / "app"
    build.options.platform.value match {
      case Platform.JVM    => bootstrap(build, appPath, mainClass, () => ())
      case Platform.JS     => buildJs(build, appPath, mainClass, logger)
      case Platform.Native => value(buildNative(build, mainClass, appPath, logger))
    }

    logger.message(
      "Started building docker image with your application, it might take some time"
    )

    DockerPackage(appPath, dockerSettings).build()

    logger.message(
      "Built docker image, run it with" + System.lineSeparator() +
        s"  docker run $repository:$tag"
    )
  }

  private def buildJs(
    build: Build.Successful,
    destPath: os.Path,
    mainClass: String,
    logger: Logger
  ): Either[BuildException, os.Path] = {
    val linkerConfig = build.options.scalaJsOptions.linkerConfig(logger)
    linkJs(
      build,
      destPath,
      Some(mainClass),
      addTestInitializer = false,
      linkerConfig,
      build.options.scalaJsOptions.fullOpt,
      build.options.scalaJsOptions.noOpt.getOrElse(false),
      logger
    )
  }

  private def bootstrap(
    build: Build.Successful,
    destPath: os.Path,
    mainClass: String,
    alreadyExistsCheck: () => Unit
  ): Unit = {
    val byteCodeZipEntries = os.walk(build.output)
      .filter(os.isFile(_))
      .map { path =>
        val name         = path.relativeTo(build.output).toString
        val content      = os.read.bytes(path)
        val lastModified = os.mtime(path)
        val ent          = new ZipEntry(name)
        ent.setLastModifiedTime(FileTime.fromMillis(lastModified))
        ent.setSize(content.length)
        (ent, content)
      }

    // TODO Generate that in memory
    val tmpJar = os.temp(prefix = destPath.last.stripSuffix(".jar"), suffix = ".jar")
    val tmpJarParams = Parameters.Assembly()
      .withExtraZipEntries(byteCodeZipEntries)
      .withMainClass(mainClass)
    AssemblyGenerator.generate(tmpJarParams, tmpJar.toNIO)
    val tmpJarContent = os.read.bytes(tmpJar)
    os.remove(tmpJar)

    def dependencyEntries =
      build.artifacts.artifacts.map {
        case (url, path) =>
          if (build.options.notForBloopOptions.packageOptions.isStandalone)
            ClassPathEntry.Resource(path.last, os.mtime(path), os.read.bytes(path))
          else
            ClassPathEntry.Url(url)
      }
    val byteCodeEntry = ClassPathEntry.Resource(s"${destPath.last}-content.jar", 0L, tmpJarContent)

    val allEntries    = Seq(byteCodeEntry) ++ dependencyEntries
    val loaderContent = coursier.launcher.ClassLoaderContent(allEntries)
    val preamble = Preamble()
      .withOsKind(Properties.isWin)
      .callsItself(Properties.isWin)
      .withJavaOpts(build.options.javaOptions.javacOptions)
    val params = Parameters.Bootstrap(Seq(loaderContent), mainClass)
      .withDeterministic(true)
      .withPreamble(preamble)

    alreadyExistsCheck()
    BootstrapGenerator.generate(params, destPath.toNIO)
    ProcUtil.maybeUpdatePreamble(destPath)
  }

  /** Returns the dependency sub-graph of the provided modules, that is, all their JARs and their
    * transitive dependencies' JARs.
    *
    * Note that this is not exactly the same as resolving those modules on their own (with their
    * versions): other dependencies in the whole dependency sub-graph may bump versions in the
    * provided dependencies sub-graph here.
    *
    * Here, among the JARs of the whole dependency graph, we pick the ones that were pulled by the
    * provided modules, and might have been bumped by other modules. This is strictly a subset of
    * the whole dependency graph.
    */
  def providedFiles(
    build: Build.Successful,
    provided: Seq[dependency.AnyModule],
    logger: Logger
  ): Either[BuildException, Seq[os.Path]] = either {

    logger.debug(s"${provided.length} provided dependencies")
    val res = build.artifacts.resolution.getOrElse {
      sys.error("Internal error: expected resolution to have been kept")
    }
    val modules = value {
      provided
        .map(_.toCs(build.scalaParams))
        .sequence
        .left.map(CompositeBuildException(_))
    }
    val modulesSet = modules.toSet
    val providedDeps = res
      .dependencyArtifacts
      .map(_._1)
      .filter(dep => modulesSet.contains(dep.module))
    val providedRes = res.subset(providedDeps)
    val fileMap = build.artifacts.detailedArtifacts
      .map {
        case (_, _, artifact, path) =>
          artifact -> path
      }
      .toMap
    val providedFiles = coursier.Artifacts.artifacts(providedRes, Set.empty, None, None, true)
      .map(_._3)
      .map { a =>
        fileMap.getOrElse(a, sys.error(s"should not happen (missing: $a)"))
      }
    logger.debug {
      val it = Iterator(s"${providedFiles.size} provided JAR(s)") ++
        providedFiles.toVector.map(_.toString).sorted.iterator.map(f => s"  $f")
      it.mkString(System.lineSeparator())
    }
    providedFiles
  }

  def assembly(
    build: Build.Successful,
    destPath: os.Path,
    mainClassOpt: Option[String],
    extraProvided: Seq[dependency.AnyModule],
    withPreamble: Boolean,
    alreadyExistsCheck: () => Unit,
    logger: Logger
  ): Either[BuildException, Unit] = either {
    val byteCodeZipEntries = os.walk(build.output)
      .filter(os.isFile(_))
      .map { path =>
        val name         = path.relativeTo(build.output).toString
        val content      = os.read.bytes(path)
        val lastModified = os.mtime(path)
        val ent          = new ZipEntry(name)
        ent.setLastModifiedTime(FileTime.fromMillis(lastModified))
        ent.setSize(content.length)
        (ent, content)
      }

    val provided = build.options.notForBloopOptions.packageOptions.provided ++ extraProvided
    val allFiles = build.artifacts.artifacts.map(_._2)
    val files =
      if (provided.isEmpty) allFiles
      else {
        val providedFilesSet = value(providedFiles(build, provided, logger)).toSet
        allFiles.filterNot(providedFilesSet.contains)
      }

    val preambleOpt =
      if (withPreamble)
        Some {
          Preamble()
            .withOsKind(Properties.isWin)
            .callsItself(Properties.isWin)
        }
      else
        None
    val params = Parameters.Assembly()
      .withExtraZipEntries(byteCodeZipEntries)
      .withFiles(files.map(_.toIO))
      .withMainClass(mainClassOpt)
      .withPreambleOpt(preambleOpt)
    alreadyExistsCheck()
    AssemblyGenerator.generate(params, destPath.toNIO)
    ProcUtil.maybeUpdatePreamble(destPath)
  }

  final class NoMainClassFoundForAssemblyError(cause: NoMainClassFoundError) extends BuildException(
        "No main class found for assembly. Either pass one with --main-class, or make the assembly non-runnable with --preamble=false",
        cause = cause
      )

  def withSourceJar[T](
    build: Build.Successful,
    defaultLastModified: Long,
    fileName: String = "library"
  )(f: os.Path => T): T = {
    val jarContent = sourceJar(build, defaultLastModified)
    val jar = os.temp(jarContent, prefix = fileName.stripSuffix(".jar"), suffix = "-sources.jar")
    try f(jar)
    finally os.remove(jar)
  }

  def linkJs(
    build: Build.Successful,
    dest: os.Path,
    mainClassOpt: Option[String],
    addTestInitializer: Boolean,
    config: ScalaJsLinkerConfig,
    fullOpt: Boolean,
    noOpt: Boolean,
    logger: Logger,
    scratchDirOpt: Option[os.Path] = None
  ): Either[BuildException, os.Path] =
    Library.withLibraryJar(build, dest.last.stripSuffix(".jar")) { mainJar =>
      val classPath = mainJar +: build.artifacts.classPath
      val delete    = scratchDirOpt.isEmpty
      scratchDirOpt.foreach(os.makeDir.all(_))
      val linkingDir =
        os.temp.dir(
          dir = scratchDirOpt.orNull,
          prefix = "scala-cli-js-linking",
          deleteOnExit = delete
        )
      either {
        value {
          ScalaJsLinker.link(
            build.options.notForBloopOptions.scalaJsLinkerOptions,
            build.options.javaHome().value.javaCommand, // FIXME Allow users to use another JVM here?
            classPath,
            mainClassOpt.orNull,
            addTestInitializer,
            config,
            linkingDir,
            fullOpt,
            noOpt,
            logger,
            build.options.finalCache,
            build.options.archiveCache,
            build.options.scalaJsOptions.finalVersion
          )
        }
        val relMainJs      = os.rel / "main.js"
        val relSourceMapJs = os.rel / "main.js.map"
        val mainJs         = linkingDir / relMainJs
        val sourceMapJs    = linkingDir / relSourceMapJs

        if (os.exists(mainJs))
          if (
            os.walk.stream(linkingDir)
              .filter(_ != mainJs).filter(_ != sourceMapJs)
              .headOption.nonEmpty
          ) {
            // copy linking dir to dest
            os.copy(
              linkingDir,
              dest,
              createFolders = true,
              replaceExisting = true,
              mergeFolders = true
            )
            logger.debug(
              s"Scala.js linker generate multiple files for js multi-modules. Copy files to $dest directory."
            )
            dest / "main.js"
          }
          else {
            os.copy(mainJs, dest, replaceExisting = true)
            if (build.options.scalaJsOptions.emitSourceMaps && os.exists(sourceMapJs)) {
              val sourceMapDest =
                build.options.scalaJsOptions.sourceMapsDest.getOrElse(os.Path(s"$dest.map"))
              val updatedMainJs = ScalaJsLinker.updateSourceMappingURL(dest)
              os.write.over(dest, updatedMainJs)
              os.copy(sourceMapJs, sourceMapDest, replaceExisting = true)
              logger.message(s"Emitted js source maps to: $sourceMapDest")
            }
            if (delete)
              os.remove.all(linkingDir)
            dest
          }
        else {
          val found = os.walk(linkingDir).map(_.relativeTo(linkingDir))
          value(Left(new ScalaJsLinkingError(relMainJs, found)))
        }
      }
    }

  def buildNative(
    build: Build.Successful,
    mainClass: String,
    dest: os.Path,
    logger: Logger
  ): Either[BuildException, Unit] = either {

    val cliOptions = build.options.scalaNativeOptions.configCliOptions()

    val setupPython = build.options.notForBloopOptions.doSetupPython.getOrElse(false)
    val pythonLdFlags =
      if (setupPython)
        value {
          val python       = Python()
          val flagsOrError = python.ldflags
          logger.debug(s"Python ldflags: $flagsOrError")
          flagsOrError.orPythonDetectionError
        }
      else
        Nil
    val pythonCliOptions = pythonLdFlags.flatMap(f => Seq("--linking-option", f)).toList

    val allCliOptions = pythonCliOptions ++ cliOptions

    val nativeWorkDir = build.inputs.nativeWorkDir
    os.makeDir.all(nativeWorkDir)

    val cacheData =
      CachedBinary.getCacheData(
        build,
        allCliOptions,
        dest,
        nativeWorkDir
      )

    if (cacheData.changed)
      Library.withLibraryJar(build, dest.last.stripSuffix(".jar")) { mainJar =>

        val classpath = build.fullClassPath.map(_.toString) :+ mainJar.toString
        val args =
          allCliOptions ++
            logger.scalaNativeCliInternalLoggerOptions ++
            List[String](
              "--outpath",
              dest.toString(),
              "--workdir",
              nativeWorkDir.toString(),
              "--main",
              mainClass
            ) ++ classpath

        val scalaNativeCli = build.artifacts.scalaOpt
          .getOrElse {
            sys.error("Expected Scala artifacts to be fetched")
          }
          .scalaNativeCli

        val exitCode =
          Runner.runJvm(
            build.options.javaHome().value.javaCommand,
            build.options.javaOptions.javaOpts.toSeq.map(_.value.value),
            scalaNativeCli,
            "scala.scalanative.cli.ScalaNativeLd",
            args,
            logger
          ).waitFor()
        if (exitCode == 0)
          CachedBinary.updateProjectAndOutputSha(dest, nativeWorkDir, cacheData.projectSha)
        else
          throw new ScalaNativeBuildError
      }
  }
}
