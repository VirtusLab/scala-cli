package scala.cli.commands.package0
import caseapp.*
import caseapp.core.help.HelpFormat
import coursier.core
import coursier.core.Resolution
import coursier.launcher.*
import dependency.*
import os.{BasePathImpl, FilePath, Path, SegmentedPath}
import packager.config.*
import packager.deb.DebianPackage
import packager.docker.DockerPackage
import packager.mac.dmg.DmgPackage
import packager.mac.pkg.PkgPackage
import packager.rpm.RedHatPackage
import packager.windows.WindowsPackage

import java.io.{ByteArrayOutputStream, OutputStream}
import java.nio.file.attribute.FileTime
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.{ZipEntry, ZipOutputStream}

import scala.build.*
import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.errors.*
import scala.build.interactive.InteractiveFileOps
import scala.build.internal.Util.*
import scala.build.internal.resource.NativeResourceMapper
import scala.build.internal.{Runner, ScalaJsLinkerConfig}
import scala.build.options.PackageType.Native
import scala.build.options.{BuildOptions, JavaOpt, PackageType, Platform, ScalaNativeTarget, Scope}
import scala.cli.CurrentParams
import scala.cli.commands.OptionsHelper.*
import scala.cli.commands.doc.Doc
import scala.cli.commands.packaging.Spark
import scala.cli.commands.run.Run.{createPythonInstance, orPythonDetectionError}
import scala.cli.commands.shared.{HelpCommandGroup, HelpGroup, MainClassOptions, SharedOptions}
import scala.cli.commands.util.BuildCommandHelpers
import scala.cli.commands.util.BuildCommandHelpers.*
import scala.cli.commands.{CommandUtils, ScalaCommand, WatchUtil}
import scala.cli.config.Keys
import scala.cli.errors.ScalaJsLinkingError
import scala.cli.internal.{CachedBinary, Constants, ProcUtil, ScalaJsLinker}
import scala.cli.packaging.{Library, NativeImage}
import scala.cli.util.ArgHelpers.*
import scala.cli.util.ConfigDbUtils
import scala.util.Properties

object Package extends ScalaCommand[PackageOptions] with BuildCommandHelpers {
  override def name          = "package"
  override def group: String = HelpCommandGroup.Main.toString

  val primaryHelpGroups: Seq[HelpGroup] = Seq(
    HelpGroup.Package,
    HelpGroup.Scala,
    HelpGroup.Java,
    HelpGroup.Debian,
    HelpGroup.MacOS,
    HelpGroup.RedHat,
    HelpGroup.Windows,
    HelpGroup.Docker,
    HelpGroup.NativeImage
  )
  val hiddenHelpGroups: Seq[HelpGroup] = Seq(HelpGroup.Entrypoint, HelpGroup.Watch)
  override def helpFormat: HelpFormat  = super.helpFormat
    .withHiddenGroups(hiddenHelpGroups)
    .withPrimaryGroups(primaryHelpGroups)
  override def sharedOptions(options: PackageOptions): Option[SharedOptions] = Some(options.shared)
  override def scalaSpecificationLevel = SpecificationLevel.RESTRICTED
  override def buildOptions(options: PackageOptions): Option[BuildOptions] =
    Some(options.baseBuildOptions.orExit(options.shared.logger))
  override def runCommand(options: PackageOptions, args: RemainingArgs, logger: Logger): Unit = {
    val inputs = options.shared.inputs(args.remaining).orExit(logger)
    CurrentParams.workspaceOpt = Some(inputs.workspace)

    // FIXME mainClass encoding has issues with special chars, such as '-'

    val initialBuildOptions = finalBuildOptions(options)
    val threads             = BuildThreads.create()
    val compilerMaker       = options.compilerMaker(threads).orExit(logger)
    val docCompilerMakerOpt = options.docCompilerMakerOpt

    val cross                 = options.compileCross.cross.getOrElse(false)
    val configDb              = ConfigDbUtils.configDb.orExit(logger)
    val actionableDiagnostics =
      options.shared.logging.verbosityOptions.actions.orElse(
        configDb.get(Keys.actions).getOrElse(None)
      )

    val withTestScope = options.shared.scope.test.getOrElse(false)
    if options.watch.watchMode then {
      var expectedModifyEpochSecondOpt = Option.empty[Long]
      val isFirstRun                   = new AtomicBoolean(true)
      val watcher                      = Build.watch(
        inputs,
        initialBuildOptions,
        compilerMaker,
        docCompilerMakerOpt,
        logger,
        crossBuilds = cross,
        buildTests = withTestScope,
        partial = None,
        actionableDiagnostics = actionableDiagnostics,
        postAction = () => WatchUtil.printWatchMessage()
      ) { res =>
        if (options.watch.watchClearScreen && !isFirstRun.getAndSet(false))
          WatchUtil.clearScreen()
        res.orReport(logger).map(_.builds).foreach {
          case b if b.forall(_.success) =>
            val successfulBuilds = b.collect { case s: Build.Successful => s }
            successfulBuilds.foreach(_.copyOutput(options.shared))
            val mtimeDestPath = doPackage(
              logger = logger,
              outputOpt = options.output.filter(_.nonEmpty),
              force = options.force,
              forcedPackageTypeOpt = options.forcedPackageTypeOpt,
              builds = successfulBuilds,
              extraArgs = args.unparsed,
              expectedModifyEpochSecondOpt = expectedModifyEpochSecondOpt,
              allowTerminate = !options.watch.watchMode,
              mainClassOptions = options.mainClass,
              withTestScope = withTestScope
            )
              .orReport(logger)
            for (valueOpt <- mtimeDestPath)
              expectedModifyEpochSecondOpt = valueOpt
          case b if b.exists(bb => !bb.success && !bb.cancelled) =>
            System.err.println("Compilation failed")
          case _ => System.err.println("Build cancelled")
        }
      }
      try WatchUtil.waitForCtrlC(() => watcher.schedule())
      finally watcher.dispose()
    }
    else
      Build.build(
        inputs,
        initialBuildOptions,
        compilerMaker,
        docCompilerMakerOpt,
        logger,
        crossBuilds = cross,
        buildTests = withTestScope,
        partial = None,
        actionableDiagnostics = actionableDiagnostics
      )
        .orExit(logger)
        .builds match {
        case b if b.forall(_.success) =>
          val successfulBuilds = b.collect { case s: Build.Successful => s }
          successfulBuilds.foreach(_.copyOutput(options.shared))
          val res0 = doPackage(
            logger = logger,
            outputOpt = options.output.filter(_.nonEmpty),
            force = options.force,
            forcedPackageTypeOpt = options.forcedPackageTypeOpt,
            builds = successfulBuilds,
            extraArgs = args.unparsed,
            expectedModifyEpochSecondOpt = None,
            allowTerminate = !options.watch.watchMode,
            mainClassOptions = options.mainClass,
            withTestScope = withTestScope
          )
          res0.orExit(logger)
        case b if b.exists(bb => !bb.success && !bb.cancelled) =>
          System.err.println("Compilation failed")
          sys.exit(1)
        case _ =>
          System.err.println("Build cancelled")
          sys.exit(1)
      }
  }

  def finalBuildOptions(options: PackageOptions): BuildOptions = {
    val initialOptions    = options.finalBuildOptions.orExit(options.shared.logger)
    val finalBuildOptions = initialOptions.copy(scalaOptions =
      initialOptions.scalaOptions.copy(defaultScalaVersion = Some(defaultScalaVersion))
    )
    val buildOptions = finalBuildOptions.copy(
      javaOptions = finalBuildOptions.javaOptions.copy(
        javaOpts =
          finalBuildOptions.javaOptions.javaOpts ++
            options.java.allJavaOpts.map(JavaOpt(_)).map(Positioned.commandLine)
      )
    )
    buildOptions
  }

  private def doPackage(
    logger: Logger,
    outputOpt: Option[String],
    force: Boolean,
    forcedPackageTypeOpt: Option[PackageType],
    builds: Seq[Build.Successful],
    extraArgs: Seq[String],
    expectedModifyEpochSecondOpt: Option[Long],
    allowTerminate: Boolean,
    mainClassOptions: MainClassOptions,
    withTestScope: Boolean
  ): Either[BuildException, Option[Long]] = either {
    if mainClassOptions.mainClassLs.contains(true) then
      value {
        mainClassOptions
          .maybePrintMainClasses(
            builds.flatMap(_.foundMainClasses()).distinct,
            shouldExit = allowTerminate
          )
          .map(_ => None)
      }
    else {
      val packageType: PackageType = value(resolvePackageType(builds, forcedPackageTypeOpt))
      // TODO When possible, call alreadyExistsCheck() before compiling stuff

      def extension = packageType match {
        case PackageType.LibraryJar  => ".jar"
        case PackageType.SourceJar   => ".jar"
        case PackageType.DocJar      => ".jar"
        case _: PackageType.Assembly => ".jar"
        case PackageType.Spark       => ".jar"
        case PackageType.Js          => ".js"
        case PackageType.Debian      => ".deb"
        case PackageType.Dmg         => ".dmg"
        case PackageType.Pkg         => ".pkg"
        case PackageType.Rpm         => ".rpm"
        case PackageType.Msi         => ".msi"

        case PackageType.Native.Application =>
          if Properties.isWin then ".exe" else ""
        case PackageType.Native.LibraryDynamic =>
          if Properties.isWin then ".dll" else if Properties.isMac then ".dylib" else ".so"
        case PackageType.Native.LibraryStatic =>
          if Properties.isWin then ".lib" else ".a"

        case PackageType.GraalVMNativeImage if Properties.isWin => ".exe"
        case _ if Properties.isWin                              => ".bat"
        case _                                                  => ""
      }

      def defaultName = packageType match {
        case PackageType.LibraryJar  => "library.jar"
        case PackageType.SourceJar   => "source.jar"
        case PackageType.DocJar      => "scaladoc.jar"
        case _: PackageType.Assembly => "app.jar"
        case PackageType.Spark       => "job.jar"
        case PackageType.Js          => "app.js"
        case PackageType.Debian      => "app.deb"
        case PackageType.Dmg         => "app.dmg"
        case PackageType.Pkg         => "app.pkg"
        case PackageType.Rpm         => "app.rpm"
        case PackageType.Msi         => "app.msi"

        case PackageType.Native.Application =>
          if Properties.isWin then "app.exe" else "app"

        case PackageType.Native.LibraryDynamic =>
          if Properties.isWin then "library.dll"
          else if Properties.isMac then "library.dylib"
          else "library.so"

        case PackageType.Native.LibraryStatic =>
          if Properties.isWin then "library.lib" else "library.a"

        case PackageType.GraalVMNativeImage if Properties.isWin => "app.exe"
        case _ if Properties.isWin                              => "app.bat"
        case _                                                  => "app"
      }
      val output = outputOpt.map {
        case path
            if packageType == PackageType.GraalVMNativeImage
            && Properties.isWin && !path.endsWith(".exe") =>
          s"$path.exe" // graalvm-native-image requires .exe extension on Windows
        case path => path
      }

      val packageOutput = builds.head.options.notForBloopOptions.packageOptions.output
      val dest          = output.orElse(packageOutput)
        .orElse {
          builds.flatMap(_.sources.defaultMainClass)
            .headOption
            .map(n => n.drop(n.lastIndexOf('.') + 1))
            .map(_.stripSuffix("_sc"))
            .map(_ + extension)
        }
        .orElse {
          builds.flatMap(_.retainedMainClass(logger).toOption)
            .headOption
            .map(_.stripSuffix("_sc") + extension)
        }
        .orElse(builds.flatMap(_.sources.paths).collectFirst(_._1.baseName + extension))
        .getOrElse(defaultName)
      val destPath      = os.Path(dest, Os.pwd)
      val printableDest = CommandUtils.printablePath(destPath)

      def alreadyExistsCheck(): Either[BuildException, Unit] =
        if !force &&
          os.exists(destPath) &&
          !expectedModifyEpochSecondOpt.contains(os.mtime(destPath))
        then
          builds.head.options.interactive.map { interactive =>
            InteractiveFileOps.erasingPath(interactive, printableDest, destPath) { () =>
              val errorMsg =
                if expectedModifyEpochSecondOpt.isEmpty then s"$printableDest already exists"
                else s"$printableDest was overwritten by another process"
              System.err.println(s"Error: $errorMsg. Pass -f or --force to force erasing it.")
              sys.exit(1)
            }
          }
        else Right(())

      value(alreadyExistsCheck())

      def mainClass: Either[BuildException, String] =
        builds.head.options.mainClass.filter(_.nonEmpty) match {
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

        }

      def mainClassOpt: Option[String] = mainClass.toOption

      val packageOptions = builds.head.options.notForBloopOptions.packageOptions

      val outputPath = packageType match {
        case PackageType.Bootstrap =>
          value(bootstrap(builds, destPath, value(mainClass), () => alreadyExistsCheck(), logger))
          destPath
        case PackageType.LibraryJar =>
          val libraryJar = Library.libraryJar(builds)
          value(alreadyExistsCheck())
          if force then os.copy.over(libraryJar, destPath, createFolders = true)
          else os.copy(libraryJar, destPath, createFolders = true)
          destPath
        case PackageType.SourceJar =>
          val now     = System.currentTimeMillis()
          val content = sourceJar(builds, now)
          value(alreadyExistsCheck())
          if force then os.write.over(destPath, content, createFolders = true)
          else os.write(destPath, content, createFolders = true)
          destPath
        case PackageType.DocJar =>
          val docJarPath = value(docJar(builds, logger, extraArgs, withTestScope))
          value(alreadyExistsCheck())
          if force then os.copy.over(docJarPath, destPath, createFolders = true)
          else os.copy(docJarPath, destPath, createFolders = true)
          destPath
        case a: PackageType.Assembly =>
          value {
            assembly(
              builds = builds,
              destPath = destPath,
              mainClassOpt = a.mainClassInManifest match {
                case None =>
                  if a.addPreamble then {
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
              extraProvided = Nil,
              withPreamble = a.addPreamble,
              alreadyExistsCheck = () => alreadyExistsCheck(),
              logger = logger
            )
          }
          destPath
        case PackageType.Spark =>
          value {
            assembly(
              builds,
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
          value(buildJs(builds, destPath, mainClassOpt, logger))

        case tpe: PackageType.Native =>
          import PackageType.Native.*
          val mainClassO =
            tpe match
              case Application => Some(value(mainClass))
              case _           => None

          val cachedDest = value(buildNative(
            builds = builds,
            mainClass = mainClassO,
            targetType = tpe,
            destPath = Some(destPath),
            logger = logger
          ))
          if force then os.copy.over(cachedDest, destPath, createFolders = true)
          else os.copy(cachedDest, destPath, createFolders = true)
          destPath

        case PackageType.GraalVMNativeImage =>
          NativeImage.buildNativeImage(
            builds,
            value(mainClass),
            destPath,
            builds.head.inputs.nativeImageWorkDir,
            extraArgs,
            logger
          )
          destPath

        case nativePackagerType: PackageType.NativePackagerType =>
          val bootstrapPath = os.temp.dir(prefix = "scala-packager") / "app"
          value {
            bootstrap(
              builds,
              bootstrapPath,
              value(mainClass),
              () => alreadyExistsCheck(),
              logger
            )
          }
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
            ),
            priority = packageOptions.debianOptions.priority,
            section = packageOptions.debianOptions.section
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
            installerVersion = packageOptions.windowsOptions.installerVersion,
            wixUpgradeCodeGuid = packageOptions.windowsOptions.wixUpgradeCodeGuid
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
          value(docker(builds, value(mainClass), logger))
          destPath
      }

      val printableOutput = CommandUtils.printablePath(outputPath)

      if packageType.runnable.nonEmpty then
        logger.message {
          if packageType.runnable.contains(true) then
            s"Wrote $outputPath, run it with" + System.lineSeparator() +
              "  " + printableOutput
          else if packageType == PackageType.Js then
            s"Wrote $outputPath, run it with" + System.lineSeparator() +
              "  node " + printableOutput
          else s"Wrote $outputPath"
        }

      val mTimeDestPathOpt = if packageType.runnable.isEmpty then None else Some(os.mtime(destPath))
      mTimeDestPathOpt
    }
    // end of doPackage
  }

  def docJar(
    builds: Seq[Build.Successful],
    logger: Logger,
    extraArgs: Seq[String],
    withTestScope: Boolean
  ): Either[BuildException, os.Path] = either {

    val workDir   = builds.head.inputs.docJarWorkDir
    val dest      = workDir / "doc.jar"
    val cacheData =
      CachedBinary.getCacheData(
        builds = builds,
        config = extraArgs.toList,
        dest = dest,
        workDir = workDir
      )

    if cacheData.changed then {

      val contentDir = value(Doc.generateScaladocDirPath(builds, logger, extraArgs, withTestScope))

      var outputStream: OutputStream = null
      try {
        outputStream = os.write.outputStream(dest, createFolders = true)
        Library.writeLibraryJarTo(
          outputStream,
          builds,
          hasActualManifest = false,
          contentDirOverride = Some(contentDir)
        )
      }
      finally if outputStream != null then outputStream.close()

      CachedBinary.updateProjectAndOutputSha(dest, workDir, cacheData.projectSha)
    }

    dest
  }

  private val generatedSourcesPrefix = os.rel / "META-INF" / "generated"
  def sourceJar(builds: Seq[Build.Successful], defaultLastModified: Long): Array[Byte] = {

    val baos                 = new ByteArrayOutputStream
    var zos: ZipOutputStream = null

    def fromSimpleSources = builds.flatMap(_.sources.paths).distinct.iterator.map {
      case (path, relPath) =>
        val lastModified = os.mtime(path)
        val content      = os.read.bytes(path)
        (relPath, content, lastModified)
    }

    def fromGeneratedSources =
      builds.flatMap(_.sources.inMemory).distinct.iterator.flatMap { inMemSource =>
        val lastModified = inMemSource.originalPath match {
          case Right((_, origPath)) => os.mtime(origPath)
          case Left(_)              => defaultLastModified
        }
        val originalOpt = inMemSource.originalPath.toOption.collect {
          case (subPath, origPath) if subPath != inMemSource.generatedRelPath =>
            val origContent = os.read.bytes(origPath)
            (subPath, origContent, lastModified)
        }
        val prefix    = if (originalOpt.isEmpty) os.rel else generatedSourcesPrefix
        val generated = (
          prefix / inMemSource.generatedRelPath,
          inMemSource.content,
          lastModified
        )
        Iterator(generated) ++ originalOpt.iterator
      }

    def paths: Iterator[(FilePath & BasePathImpl & SegmentedPath, Array[Byte], Long)] =
      fromSimpleSources ++ fromGeneratedSources

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
    finally if zos != null then zos.close()

    baos.toByteArray
  }

  private def docker(
    builds: Seq[Build.Successful],
    mainClass: String,
    logger: Logger
  ): Either[BuildException, Unit] = either {
    val packageOptions = builds.head.options.notForBloopOptions.packageOptions

    if builds.head.options.platform.value == Platform.Native && (Properties.isMac || Properties.isWin)
    then {
      System.err.println(
        "Package scala native application to docker image is not supported on MacOs and Windows"
      )
      sys.exit(1)
    }

    val exec = packageOptions.dockerOptions.cmd.orElse {
      builds.head.options.platform.value match {
        case Platform.JVM    => Some("sh")
        case Platform.JS     => Some("node")
        case Platform.Native => None
      }
    }
    val from = packageOptions.dockerOptions.from.getOrElse {
      builds.head.options.platform.value match {
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
      dockerExecutable = None,
      extraDirectories = packageOptions.dockerOptions.extraDirectories.map(_.toNIO)
    )

    val appPath = os.temp.dir(prefix = "scala-cli-docker") / "app"
    builds.head.options.platform.value match {
      case Platform.JVM    => value(bootstrap(builds, appPath, mainClass, () => Right(()), logger))
      case Platform.JS     => buildJs(builds, appPath, Some(mainClass), logger)
      case Platform.Native =>
        val dest =
          value(buildNative(
            builds = builds,
            mainClass = Some(mainClass),
            targetType = PackageType.Native.Application,
            destPath = None,
            logger = logger
          ))
        os.copy(dest, appPath)
    }

    logger.message("Started building docker image with your application, it might take some time")

    DockerPackage(appPath, dockerSettings).build()

    logger.message(
      "Built docker image, run it with" + System.lineSeparator() +
        s"  docker run $repository:$tag"
    )
  }

  private def buildJs(
    builds: Seq[Build.Successful],
    destPath: os.Path,
    mainClass: Option[String],
    logger: Logger
  ): Either[BuildException, os.Path] = for {
    isFullOpt <- builds.head.options.scalaJsOptions.fullOpt
    linkerConfig = builds.head.options.scalaJsOptions.linkerConfig(logger)
    linkResult <- linkJs(
      builds = builds,
      dest = destPath,
      mainClassOpt = mainClass,
      addTestInitializer = false,
      config = linkerConfig,
      fullOpt = isFullOpt,
      noOpt = builds.head.options.scalaJsOptions.noOpt.getOrElse(false),
      logger = logger
    )
  } yield linkResult

  private def bootstrap(
    builds: Seq[Build.Successful],
    destPath: os.Path,
    mainClass: String,
    alreadyExistsCheck: () => Either[BuildException, Unit],
    logger: Logger
  ): Either[BuildException, Unit] = either {
    val byteCodeZipEntries = builds.flatMap { build =>
      os.walk(build.output)
        .filter(os.isFile(_))
        .map { path =>
          val name         = path.relativeTo(build.output).toString
          val content      = os.read.bytes(path)
          val lastModified = os.mtime(path)
          val entry        = new ZipEntry(name)
          entry.setLastModifiedTime(FileTime.fromMillis(lastModified))
          entry.setSize(content.length)
          (entry, content)
        }
    }

    // TODO Generate that in memory
    val tmpJar       = os.temp(prefix = destPath.last.stripSuffix(".jar"), suffix = ".jar")
    val tmpJarParams = Parameters.Assembly()
      .withExtraZipEntries(byteCodeZipEntries)
      .withMainClass(mainClass)
    AssemblyGenerator.generate(tmpJarParams, tmpJar.toNIO)
    val tmpJarContent = os.read.bytes(tmpJar)
    os.remove(tmpJar)

    def dependencyEntries: Seq[ClassPathEntry] =
      builds.flatMap(_.artifacts.artifacts).distinct.map {
        case (url, path) =>
          if builds.head.options.notForBloopOptions.packageOptions.isStandalone then
            ClassPathEntry.Resource(path.last, os.mtime(path), os.read.bytes(path))
          else ClassPathEntry.Url(url)
      }
    val byteCodeEntry  = ClassPathEntry.Resource(s"${destPath.last}-content.jar", 0L, tmpJarContent)
    val extraClassPath = builds.head.options.classPathOptions.extraClassPath.map { classPath =>
      ClassPathEntry.Resource(classPath.last, os.mtime(classPath), os.read.bytes(classPath))
    }

    val allEntries    = Seq(byteCodeEntry) ++ dependencyEntries ++ extraClassPath
    val loaderContent = coursier.launcher.ClassLoaderContent(allEntries)
    val preamble      = Preamble()
      .withOsKind(Properties.isWin)
      .callsItself(Properties.isWin)
      .withJavaOpts(builds.head.options.javaOptions.javaOpts.toSeq.map(_.value.value))
    val baseParams = Parameters.Bootstrap(Seq(loaderContent), mainClass)
      .withDeterministic(true)
      .withPreamble(preamble)

    val params: Parameters.Bootstrap =
      if builds.head.options.notForBloopOptions.doSetupPython.getOrElse(false) then {
        val res = value {
          Artifacts.fetchAnyDependencies(
            Seq(Positioned.none(
              dep"${Constants.pythonInterfaceOrg}:${Constants.pythonInterfaceName}:${Constants.pythonInterfaceVersion}"
            )),
            Nil,
            None,
            logger,
            builds.head.options.finalCache,
            None,
            Some(_)
          )
        }
        val entries = res.artifacts.map {
          case (a, f) =>
            val path = os.Path(f)
            if builds.head.options.notForBloopOptions.packageOptions.isStandalone then
              ClassPathEntry.Resource(path.last, os.mtime(path), os.read.bytes(path))
            else ClassPathEntry.Url(a.url)
        }
        val pythonContent = Seq(ClassLoaderContent(entries))
        baseParams.addExtraContent("python", pythonContent).withPython(true)
      }
      else baseParams

    value(alreadyExistsCheck())
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
    builds: Seq[Build.Successful],
    provided: Seq[dependency.AnyModule],
    logger: Logger
  ): Either[BuildException, Seq[os.Path]] = either {
    logger.debug(s"${provided.length} provided dependencies")
    val res = builds.map(_.artifacts.resolution.getOrElse {
      sys.error("Internal error: expected resolution to have been kept")
    })
    val modules: Seq[coursier.Module] = value {
      provided
        .map(_.toCs(builds.head.scalaParams)) // Scala params should be the same for all scopes
        .sequence
        .left.map(CompositeBuildException(_))
    }
    val modulesSet                         = modules.toSet
    val providedDeps: Seq[core.Dependency] = value {
      res
        .map(_.dependencyArtifacts0.safeArtifacts.map(_.map(_._1)))
        .sequence
        .left
        .map(CompositeBuildException(_))
        .map(_.flatten.filter(dep => modulesSet.contains(dep.module)))
    }
    val providedRes: Seq[Resolution] = value {
      res
        .map(_.subset0(providedDeps).left.map(CoursierDependencyError(_)))
        .sequence
        .left
        .map(CompositeBuildException(_))
    }
    val fileMap = builds.flatMap(_.artifacts.detailedRuntimeArtifacts).distinct
      .map { case (_, _, artifact, path) => artifact -> path }
      .toMap
    val providedFiles: Seq[os.Path] = value {
      providedRes
        .map(r =>
          coursier.Artifacts.artifacts0(
            resolution = r,
            classifiers = Set.empty,
            attributes = Seq.empty,
            mainArtifactsOpt = None,
            artifactTypesOpt = None,
            classpathOrder = true
          ).safeArtifacts
        )
        .sequence
        .left
        .map(CompositeBuildException(_))
        .map {
          _.flatten
            .distinct
            .map(_._3)
            .map(a => fileMap.getOrElse(a, sys.error(s"should not happen (missing: $a)")))
        }
    }
    logger.debug {
      val it = Iterator(s"${providedFiles.size} provided JAR(s)") ++
        providedFiles.toVector.map(_.toString).sorted.iterator.map(f => s"  $f")
      it.mkString(System.lineSeparator())
    }
    providedFiles
  }

  def assembly(
    builds: Seq[Build.Successful],
    destPath: os.Path,
    mainClassOpt: Option[String],
    extraProvided: Seq[dependency.AnyModule],
    withPreamble: Boolean,
    alreadyExistsCheck: () => Either[BuildException, Unit],
    logger: Logger
  ): Either[BuildException, Unit] = either {
    val compiledClassesByOutputDir: Seq[(Path, Path)] =
      builds.flatMap(build =>
        os.walk(build.output).filter(os.isFile(_)).map(build.output -> _)
      ).distinct
    val (extraClassesFolders, extraJars) =
      builds.flatMap(_.options.classPathOptions.extraClassPath).partition(os.isDir(_))
    val extraClassesByDefaultOutputDir =
      extraClassesFolders.flatMap(os.walk(_)).filter(os.isFile(_)).map(builds.head.output -> _)

    val byteCodeZipEntries =
      (compiledClassesByOutputDir ++ extraClassesByDefaultOutputDir)
        .distinct
        .map { (outputDir, path) =>
          val name         = path.relativeTo(outputDir).toString
          val content      = os.read.bytes(path)
          val lastModified = os.mtime(path)
          val ent          = new ZipEntry(name)
          ent.setLastModifiedTime(FileTime.fromMillis(lastModified))
          ent.setSize(content.length)
          (ent, content)
        }

    val provided = builds.head.options.notForBloopOptions.packageOptions.provided ++ extraProvided
    val allJars  =
      builds.flatMap(_.artifacts.runtimeArtifacts.map(_._2)) ++ extraJars.filter(os.exists(_))
    val jars =
      if (provided.isEmpty) allJars
      else {
        val providedFilesSet = value(providedFiles(builds, provided, logger)).toSet
        allJars.filterNot(providedFilesSet.contains)
      }

    val preambleOpt =
      if withPreamble then
        Some {
          Preamble()
            .withOsKind(Properties.isWin)
            .callsItself(Properties.isWin)
        }
      else None
    val params = Parameters.Assembly()
      .withExtraZipEntries(byteCodeZipEntries)
      .withFiles(jars.map(_.toIO))
      .withMainClass(mainClassOpt)
      .withPreambleOpt(preambleOpt)
    value(alreadyExistsCheck())
    AssemblyGenerator.generate(params, destPath.toNIO)
    ProcUtil.maybeUpdatePreamble(destPath)
  }

  final class NoMainClassFoundForAssemblyError(cause: NoMainClassFoundError) extends BuildException(
        "No main class found for assembly. Either pass one with --main-class, or make the assembly non-runnable with --preamble=false",
        cause = cause
      )

  private object LinkingDir {
    case class Input(linkJsInput: ScalaJsLinker.LinkJSInput, scratchDirOpt: Option[os.Path])
    private var currentInput: Option[Input]        = None
    private var currentLinkingDir: Option[os.Path] = None
    def getOrCreate(
      linkJsInput: ScalaJsLinker.LinkJSInput,
      scratchDirOpt: Option[os.Path]
    ): os.Path =
      val input = Input(linkJsInput, scratchDirOpt)
      currentLinkingDir match {
        case Some(linkingDir) if currentInput.contains(input) =>
          linkingDir
        case _ =>
          scratchDirOpt.foreach(os.makeDir.all(_))

          currentLinkingDir.foreach(dir => os.remove.all(dir))
          currentLinkingDir = None

          val linkingDirectory = os.temp.dir(
            dir = scratchDirOpt.orNull,
            prefix = "scala-cli-js-linking",
            deleteOnExit = scratchDirOpt.isEmpty
          )

          currentInput = Some(input)
          currentLinkingDir = Some(linkingDirectory)

          linkingDirectory
      }
  }

  def linkJs(
    builds: Seq[Build.Successful],
    dest: os.Path,
    mainClassOpt: Option[String],
    addTestInitializer: Boolean,
    config: ScalaJsLinkerConfig,
    fullOpt: Boolean,
    noOpt: Boolean,
    logger: Logger,
    scratchDirOpt: Option[os.Path] = None
  ): Either[BuildException, os.Path] = {
    val jar       = Library.libraryJar(builds)
    val classPath = Seq(jar) ++ builds.flatMap(_.artifacts.classPath)
    val input     = ScalaJsLinker.LinkJSInput(
      options = builds.head.options.notForBloopOptions.scalaJsLinkerOptions,
      javaCommand =
        builds.head.options.javaHome().value.javaCommand, // FIXME Allow users to use another JVM here?
      classPath = classPath,
      mainClassOrNull = mainClassOpt.orNull,
      addTestInitializer = addTestInitializer,
      config = config,
      fullOpt = fullOpt,
      noOpt = noOpt,
      scalaJsVersion = builds.head.options.scalaJsOptions.finalVersion
    )

    val linkingDir = LinkingDir.getOrCreate(input, scratchDirOpt)

    either {
      value {
        ScalaJsLinker.link(
          input,
          linkingDir,
          logger,
          builds.head.options.finalCache,
          builds.head.options.archiveCache
        )
      }
      os.walk.stream(linkingDir).filter(_.ext == "js").toSeq match {
        case Seq(sourceJs) if os.isFile(sourceJs) && sourceJs.last.endsWith(".js") =>
          // there's just one js file to link, so we copy it directly
          logger.debug(
            s"Scala.js linker generated single file ${sourceJs.last}. Copying it to $dest"
          )
          val sourceMapJs = os.Path(sourceJs.toString + ".map")
          os.copy(sourceJs, dest, replaceExisting = true)
          if builds.head.options.scalaJsOptions.emitSourceMaps && os.exists(sourceMapJs) then {
            logger.debug(
              s"Source maps emission enabled, copying source map file: ${sourceMapJs.last}"
            )
            val sourceMapDest =
              builds.head.options.scalaJsOptions.sourceMapsDest.getOrElse(os.Path(s"$dest.map"))
            val updatedMainJs = ScalaJsLinker.updateSourceMappingURL(dest)
            os.write.over(dest, updatedMainJs)
            os.copy(sourceMapJs, sourceMapDest, replaceExisting = true)
            logger.message(s"Emitted js source maps to: $sourceMapDest")
          }
          dest
        case _ @Seq(jsSource, _*) =>
          os.copy(
            linkingDir,
            dest,
            createFolders = true,
            replaceExisting = true,
            mergeFolders = true
          )
          logger.debug(
            s"Scala.js linker generated multiple files for js multi-modules. Copied files to $dest directory."
          )
          val jsFileToReturn = os.rel / {
            mainClassOpt match {
              case Some(_) if os.exists(linkingDir / "main.js")  => "main.js"
              case Some(mc) if os.exists(linkingDir / s"$mc.js") => s"$mc.js"
              case _                                             => jsSource.relativeTo(linkingDir)
            }
          }
          dest / jsFileToReturn
        case Nil =>
          logger.debug("Scala.js linker did not generate any .js files.")
          val allFilesInLinkingDir = os.walk(linkingDir).map(_.relativeTo(linkingDir))
          value(Left(new ScalaJsLinkingError(
            expected = if mainClassOpt.nonEmpty then "main.js" else "<module>.js",
            foundFiles = allFilesInLinkingDir
          )))
      }
    }
  }

  def buildNative(
    builds: Seq[Build.Successful],
    mainClass: Option[String], // when building a static/dynamic library, we don't need a main class
    targetType: PackageType.Native,
    destPath: Option[os.Path],
    logger: Logger
  ): Either[BuildException, os.Path] = either {
    val dest = builds.head.inputs.nativeWorkDir / s"main${if (Properties.isWin) ".exe" else ""}"

    val cliOptions =
      builds.head.options.scalaNativeOptions.configCliOptions(builds.exists(
        _.sources.resourceDirs.nonEmpty
      ))

    val setupPython   = builds.head.options.notForBloopOptions.doSetupPython.getOrElse(false)
    val pythonLdFlags =
      if setupPython then
        value {
          val python       = value(createPythonInstance().orPythonDetectionError)
          val flagsOrError = python.ldflags
          logger.debug(s"Python ldflags: $flagsOrError")
          flagsOrError.orPythonDetectionError
        }
      else Nil
    val pythonCliOptions = pythonLdFlags.flatMap(f => Seq("--linking-option", f)).toList

    val libraryLinkingOptions: Seq[String] =
      Option.when(targetType != PackageType.Native.Application) {
        /* If we are building a library, we make sure to change the name
         that the linker will put into the loading path - otherwise
         the built library will depend on some internal path within .scala-build
         */

        destPath.flatMap(_.lastOpt).toSeq.flatMap { filename =>
          val linkerOption =
            if Properties.isLinux then s"-Wl,-soname,$filename" else s"-Wl,-install_name,$filename"
          Seq("--linking-option", linkerOption)
        }
      }.toSeq.flatten

    val allCliOptions = pythonCliOptions ++
      cliOptions ++
      libraryLinkingOptions ++
      mainClass.toSeq.flatMap(m => Seq("--main", m))

    val nativeWorkDir = builds.head.inputs.nativeWorkDir
    os.makeDir.all(nativeWorkDir)

    val cacheData =
      CachedBinary.getCacheData(
        builds,
        allCliOptions,
        dest,
        nativeWorkDir
      )

    if (cacheData.changed) {
      builds.foreach(build => NativeResourceMapper.copyCFilesToScalaNativeDir(build, nativeWorkDir))
      val jar       = Library.libraryJar(builds)
      val classpath = (Seq(jar) ++ builds.flatMap(_.artifacts.classPath)).map(_.toString).distinct
      val args      =
        allCliOptions ++
          logger.scalaNativeCliInternalLoggerOptions ++
          List[String](
            "--outpath",
            dest.toString(),
            "--workdir",
            nativeWorkDir.toString()
          ) ++ classpath

      val scalaNativeCli = builds.flatMap(_.artifacts.scalaOpt).headOption
        .getOrElse {
          sys.error("Expected Scala artifacts to be fetched")
        }
        .scalaNativeCli

      val exitCode =
        Runner.runJvm(
          builds.head.options.javaHome().value.javaCommand,
          builds.head.options.javaOptions.javaOpts.toSeq.map(_.value.value),
          scalaNativeCli,
          "scala.scalanative.cli.ScalaNativeLd",
          args,
          logger
        ).waitFor()
      if exitCode == 0 then
        CachedBinary.updateProjectAndOutputSha(dest, nativeWorkDir, cacheData.projectSha)
      else throw new ScalaNativeBuildError
    }

    dest
  }
  def resolvePackageType(
    builds: Seq[Build.Successful],
    forcedPackageTypeOpt: Option[PackageType]
  ): Either[BuildException, PackageType] = {
    val basePackageTypeOpt = builds.head.options.notForBloopOptions.packageOptions.packageTypeOpt
    lazy val validPackageScalaJS =
      Seq(PackageType.Js, PackageType.LibraryJar, PackageType.SourceJar, PackageType.DocJar)
    lazy val validPackageScalaNative =
      Seq(
        PackageType.LibraryJar,
        PackageType.SourceJar,
        PackageType.DocJar,
        PackageType.Native.Application,
        PackageType.Native.LibraryDynamic,
        PackageType.Native.LibraryStatic
      )

    forcedPackageTypeOpt -> builds.head.options.platform.value match {
      case (Some(forcedPackageType), _) => Right(forcedPackageType)
      case (_, _) if builds.head.options.notForBloopOptions.packageOptions.isDockerEnabled =>
        basePackageTypeOpt match {
          case Some(PackageType.Docker) | None => Right(PackageType.Docker)
          case Some(packageType)               => Left(new MalformedCliInputError(
              s"Unsupported package type: $packageType for Docker."
            ))
        }
      case (_, Platform.JS) => {
          for (basePackageType <- basePackageTypeOpt)
            yield
              if validPackageScalaJS.contains(basePackageType) then Right(basePackageType)
              else
                Left(new MalformedCliInputError(
                  s"Unsupported package type: $basePackageType for Scala.js."
                ))
        }.getOrElse(Right(PackageType.Js))
      case (_, Platform.Native) => {
          val specificNativePackageType: Option[Native] =
            import ScalaNativeTarget.*
            builds.head.options.scalaNativeOptions.buildTargetStr.flatMap(fromString).map {
              case Application    => PackageType.Native.Application
              case LibraryDynamic => PackageType.Native.LibraryDynamic
              case LibraryStatic  => PackageType.Native.LibraryStatic
            }
          for basePackageType <- specificNativePackageType orElse basePackageTypeOpt
          yield
            if validPackageScalaNative.contains(basePackageType) then Right(basePackageType)
            else
              Left(new MalformedCliInputError(
                s"Unsupported package type: $basePackageType for Scala Native."
              ))
        }.getOrElse(Right(PackageType.Native.Application))
      case _ => Right(basePackageTypeOpt.getOrElse(PackageType.Bootstrap))
    }
  }
}
