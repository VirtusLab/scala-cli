package scala.cli.commands

import caseapp._
import coursier.launcher._
import dependency.dependencyString
import packager.config._
import packager.deb.DebianPackage
import packager.docker.DockerPackage
import packager.mac.dmg.DmgPackage
import packager.mac.pkg.PkgPackage
import packager.rpm.RedHatPackage
import packager.windows.WindowsPackage

import java.io.{ByteArrayOutputStream, File}
import java.nio.charset.StandardCharsets
import java.nio.file.attribute.FileTime
import java.nio.file.{Files, Path}
import java.util.zip.{ZipEntry, ZipOutputStream}

import scala.build.EitherCps.{either, value}
import scala.build._
import scala.build.errors.{BuildException, ScalaNativeBuildError}
import scala.build.internal.{NativeBuilderHelper, Runner, ScalaJsLinkerConfig}
import scala.build.options.{PackageType, Platform}
import scala.cli.CurrentParams
import scala.cli.commands.OptionsHelper._
import scala.cli.commands.util.SharedOptionsUtil._
import scala.cli.errors.{ScalaJsLinkingError, ScaladocGenerationFailedError}
import scala.cli.internal.{ProcUtil, ScalaJsLinker}
import scala.cli.packaging.{Library, NativeImage}
import scala.util.Properties

object Package extends ScalaCommand[PackageOptions] {
  override def group                                  = "Main"
  override def sharedOptions(options: PackageOptions) = Some(options.shared)
  def run(options: PackageOptions, args: RemainingArgs): Unit = {
    maybePrintGroupHelp(options)
    CurrentParams.verbosity = options.shared.logging.verbosity
    val inputs = options.shared.inputsOrExit(args.remaining)
    CurrentParams.workspaceOpt = Some(inputs.workspace)

    // FIXME mainClass encoding has issues with special chars, such as '-'

    val initialBuildOptions = options.buildOptions
    val logger              = options.shared.logger
    val threads             = BuildThreads.create()

    val compilerMaker = options.compilerMaker(threads)

    val cross = options.compileCross.cross.getOrElse(false)

    if (options.watch.watch) {
      var expectedModifyEpochSecondOpt = Option.empty[Long]
      val watcher = Build.watch(
        inputs,
        initialBuildOptions,
        compilerMaker,
        logger,
        crossBuilds = cross,
        buildTests = false,
        partial = None,
        postAction = () => WatchUtil.printWatchMessage()
      ) { res =>
        res.orReport(logger).map(_.main).foreach {
          case s: Build.Successful =>
            val mtimeDestPath = doPackage(
              logger,
              options.output.filter(_.nonEmpty),
              options.force,
              options.forcedPackageTypeOpt,
              s,
              args.unparsed,
              expectedModifyEpochSecondOpt
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
          logger,
          crossBuilds = cross,
          buildTests = false,
          partial = None
        )
          .orExit(logger)
      builds.main match {
        case s: Build.Successful =>
          val res0 = doPackage(
            logger,
            options.output.filter(_.nonEmpty),
            options.force,
            options.forcedPackageTypeOpt,
            s,
            args.unparsed,
            None
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

  private def doPackage(
    logger: Logger,
    outputOpt: Option[String],
    force: Boolean,
    forcedPackageType: Option[PackageType],
    build: Build.Successful,
    extraArgs: Seq[String],
    expectedModifyEpochSecondOpt: Option[Long]
  ): Either[BuildException, Option[Long]] = either {

    val packageType = forcedPackageType.getOrElse {
      // FIXME We'll probably need more refined rules if we start to support extra Scala.JS or Scala Native specific types
      if (build.options.notForBloopOptions.packageOptions.isDockerEnabled)
        PackageType.Docker
      else if (build.options.platform.value == Platform.JS)
        PackageType.Js
      else if (build.options.platform.value == Platform.Native)
        PackageType.Native
      else
        build.options.notForBloopOptions.packageOptions.packageTypeOpt
          .getOrElse(PackageType.Bootstrap)
    }

    // TODO When possible, call alreadyExistsCheck() before compiling stuff

    def extension = packageType match {
      case PackageType.LibraryJar                             => ".jar"
      case PackageType.SourceJar                              => ".jar"
      case PackageType.DocJar                                 => ".jar"
      case PackageType.Assembly                               => ".jar"
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
      case PackageType.Assembly                               => "app.jar"
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

    val dest = outputOpt
      .orElse {
        build.sources.mainClass
          .map(n => n.drop(n.lastIndexOf('.') + 1))
          .map(_.stripSuffix("_sc"))
          .map(_ + extension)
      }
      .getOrElse(defaultName)
    val destPath = os.Path(dest, Os.pwd)
    val printableDest =
      if (destPath.startsWith(Os.pwd)) "." + File.separator + destPath.relativeTo(Os.pwd).toString
      else destPath.toString

    def alreadyExistsCheck(): Unit = {
      val alreadyExists = !force &&
        os.exists(destPath) &&
        expectedModifyEpochSecondOpt.forall(exp => os.mtime(destPath) != exp)
      if (alreadyExists) {
        val msg =
          if (expectedModifyEpochSecondOpt.isEmpty) s"$printableDest already exists"
          else s"$printableDest was overwritten by another process"
        System.err.println(s"Error: $msg. Pass -f or --force to force erasing it.")
        sys.exit(1)
      }
    }

    alreadyExistsCheck()

    def mainClass: Either[BuildException, String] =
      build.options.mainClass match {
        case Some(cls) => Right(cls)
        case None      => build.retainedMainClass
      }

    val packageOptions = build.options.notForBloopOptions.packageOptions

    packageType match {
      case PackageType.Bootstrap =>
        bootstrap(build, destPath, value(mainClass), () => alreadyExistsCheck())
      case PackageType.LibraryJar =>
        val content = Library.libraryJar(build)
        alreadyExistsCheck()
        if (force) os.write.over(destPath, content)
        else os.write(destPath, content)
      case PackageType.SourceJar =>
        val now     = System.currentTimeMillis()
        val content = sourceJar(build, now)
        alreadyExistsCheck()
        if (force) os.write.over(destPath, content)
        else os.write(destPath, content)
      case PackageType.DocJar =>
        val content = value(docJar(build, logger, extraArgs))
        alreadyExistsCheck()
        if (force) os.write.over(destPath, content)
        else os.write(destPath, content)
      case PackageType.Assembly =>
        assembly(build, destPath, value(mainClass), () => alreadyExistsCheck())

      case PackageType.Js =>
        value(buildJs(build, destPath, value(mainClass), logger))

      case PackageType.Native =>
        buildNative(build, destPath, value(mainClass), logger)

      case PackageType.GraalVMNativeImage =>
        buildGraalVMNativeImage(build, destPath, value(mainClass), extraArgs, logger)

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
      case PackageType.Docker =>
        docker(build, value(mainClass), logger)
    }

    if (packageType.runnable.nonEmpty)
      logger.message {
        if (packageType.runnable.contains(true))
          s"Wrote $dest, run it with" + System.lineSeparator() +
            "  " + printableDest
        else if (packageType == PackageType.Js)
          s"Wrote $dest, run it with" + System.lineSeparator() +
            "  node " + printableDest
        else
          s"Wrote $dest"
      }

    val mTimeDestPathOpt = if (packageType.runnable.isEmpty) None else Some(os.mtime(destPath))
    mTimeDestPathOpt
  }

  // from https://github.com/VirtusLab/scala-cli/pull/103/files#diff-1039b442cbd23f605a61fdb9c3620b600aa4af6cab757932a719c54235d8e402R60
  private def defaultScaladocArgs = Seq(
    "-snippet-compiler:compile",
    "-Ygenerate-inkuire",
    "-external-mappings:" +
      ".*scala.*::scaladoc3::https://scala-lang.org/api/3.x/," +
      ".*java.*::javadoc::https://docs.oracle.com/javase/8/docs/api/",
    "-author",
    "-groups"
  )
  private def docJar(
    build: Build.Successful,
    logger: Logger,
    extraArgs: Seq[String]
  ): Either[BuildException, Array[Byte]] = either {
    val isScala2 = build.scalaParams.scalaVersion.startsWith("2.")
    if (isScala2)
      Library.libraryJar(
        build,
        hasActualManifest = false,
        contentDirOverride = Some(build.project.scaladocDir)
      )
    else {
      val res = value {
        Artifacts.fetch(
          Positioned.none(Seq(dep"org.scala-lang::scaladoc:${build.scalaParams.scalaVersion}")),
          build.options.finalRepositories,
          build.scalaParams,
          logger,
          build.options.finalCache,
          None
        )
      }
      val destDir = build.project.scaladocDir
      os.makeDir.all(destDir)
      val ext = if (Properties.isWin) ".exe" else ""
      val baseArgs = Seq(
        "-classpath",
        build.artifacts.classPath.map(_.toString).mkString(File.pathSeparator),
        "-d",
        destDir.toString
      )
      val defaultArgs =
        if (
          build.options.notForBloopOptions.packageOptions.useDefaultScaladocOptions.getOrElse(true)
        )
          defaultScaladocArgs
        else
          Nil
      val args = baseArgs ++
        build.project.scalaCompiler.scalacOptions ++
        extraArgs ++
        defaultArgs ++
        Seq(build.output.toString)
      val retCode = Runner.runJvm(
        (build.options.javaHomeLocation().value / "bin" / s"java$ext").toString,
        Nil, // FIXME Allow to customize that?
        res.files,
        "dotty.tools.scaladoc.Main",
        args,
        logger,
        cwd = Some(build.inputs.workspace)
      )
      if (retCode == 0)
        Library.libraryJar(build, hasActualManifest = false, contentDirOverride = Some(destDir))
      else
        value(Left(new ScaladocGenerationFailedError(retCode)))
    }
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
  ): Unit = {
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
      exec = exec
    )

    val appPath = os.temp.dir(prefix = "scala-cli-docker") / "app"
    build.options.platform.value match {
      case Platform.JVM    => bootstrap(build, appPath, mainClass, () => ())
      case Platform.JS     => buildJs(build, appPath, mainClass, logger)
      case Platform.Native => buildNative(build, appPath, mainClass, logger)
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
  ): Either[BuildException, Unit] = {
    val linkerConfig = build.options.scalaJsOptions.linkerConfig(logger)
    linkJs(
      build,
      destPath,
      Some(mainClass),
      addTestInitializer = false,
      linkerConfig,
      build.options.scalaJsOptions.fullOpt.getOrElse(false),
      build.options.scalaJsOptions.noOpt.getOrElse(false),
      logger
    )
  }

  private def buildNative(
    build: Build.Successful,
    destPath: os.Path,
    mainClass: String,
    logger: Logger
  ): Unit = {
    val workDir =
      build.options.scalaNativeOptions.nativeWorkDir(
        build.inputs.workspace,
        build.inputs.projectName
      )

    buildNative(build, mainClass, destPath, workDir, logger)
  }

  private def buildGraalVMNativeImage(
    build: Build.Successful,
    destPath: os.Path,
    mainClass: String,
    extraArgs: Seq[String],
    logger: Logger
  ): Unit = {
    val workDir =
      build.options.nativeImageWorkDir(build.inputs.workspace, build.inputs.projectName)

    NativeImage.buildNativeImage(build, mainClass, destPath, workDir, extraArgs, logger)
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
    val tmpJar = Files.createTempFile(destPath.last.stripSuffix(".jar"), ".jar")
    val tmpJarParams = Parameters.Assembly()
      .withExtraZipEntries(byteCodeZipEntries)
      .withMainClass(mainClass)
    AssemblyGenerator.generate(tmpJarParams, tmpJar)
    val tmpJarContent = os.read.bytes(os.Path(tmpJar))
    Files.deleteIfExists(tmpJar)

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
    val params = Parameters.Bootstrap(Seq(loaderContent), mainClass)
      .withDeterministic(true)
      .withPreamble(preamble)

    alreadyExistsCheck()
    BootstrapGenerator.generate(params, destPath.toNIO)
    ProcUtil.maybeUpdatePreamble(destPath)
  }

  private def assembly(
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

    val preamble = Preamble()
      .withOsKind(Properties.isWin)
      .callsItself(Properties.isWin)
    val params = Parameters.Assembly()
      .withExtraZipEntries(byteCodeZipEntries)
      .withFiles(build.artifacts.artifacts.map(_._2.toIO))
      .withMainClass(mainClass)
      .withPreamble(preamble)
    alreadyExistsCheck()
    AssemblyGenerator.generate(params, destPath.toNIO)
    ProcUtil.maybeUpdatePreamble(destPath)
  }

  def withSourceJar[T](
    build: Build.Successful,
    defaultLastModified: Long,
    fileName: String = "library"
  )(f: Path => T): T = {
    val jarContent = sourceJar(build, defaultLastModified)
    val jar        = Files.createTempFile(fileName.stripSuffix(".jar"), "-sources.jar")
    try {
      Files.write(jar, jarContent)
      f(jar)
    }
    finally Files.deleteIfExists(jar)
  }

  def linkJs(
    build: Build.Successful,
    dest: os.Path,
    mainClassOpt: Option[String],
    addTestInitializer: Boolean,
    config: ScalaJsLinkerConfig,
    fullOpt: Boolean,
    noOpt: Boolean,
    logger: Logger
  ): Either[BuildException, Unit] =
    Library.withLibraryJar(build, dest.last.toString.stripSuffix(".jar")) { mainJar =>
      val classPath  = os.Path(mainJar, os.pwd) +: build.artifacts.classPath
      val linkingDir = os.temp.dir(prefix = "scala-cli-js-linking")
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
        if (os.exists(mainJs)) {
          os.copy(mainJs, dest, replaceExisting = true)
          if (build.options.scalaJsOptions.emitSourceMaps && os.exists(sourceMapJs)) {
            val sourceMapDest =
              build.options.scalaJsOptions.sourceMapsDest.getOrElse(os.Path(s"$dest.map"))
            os.copy(sourceMapJs, sourceMapDest, replaceExisting = true)
            logger.message(s"Emitted js source maps to: $sourceMapDest")
          }
          os.remove.all(linkingDir)
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
    nativeWorkDir: os.Path,
    logger: Logger
  ): Unit = {

    val cliOptions = build.options.scalaNativeOptions.configCliOptions()

    os.makeDir.all(nativeWorkDir)

    val cacheData =
      NativeBuilderHelper.getCacheData(
        build,
        cliOptions,
        dest,
        nativeWorkDir
      )

    if (cacheData.changed)
      Library.withLibraryJar(build, dest.last.stripSuffix(".jar")) { mainJar =>

        val classpath = build.fullClassPath.map(_.toString) :+ mainJar.toString()
        val args =
          cliOptions ++
            logger.scalaNativeCliInternalLoggerOptions ++
            List[String](
              "--outpath",
              dest.toString(),
              "--workdir",
              nativeWorkDir.toString(),
              "--main",
              mainClass
            ) ++ classpath

        val exitCode =
          Runner.runJvm(
            build.options.javaHome().value.javaCommand,
            build.options.javaOptions.javaOpts.toSeq.map(_.value.value),
            build.artifacts.scalaNativeCli.map(_.toIO),
            "scala.scalanative.cli.ScalaNativeLd",
            args,
            logger
          )
        if (exitCode == 0)
          NativeBuilderHelper.updateProjectAndOutputSha(dest, nativeWorkDir, cacheData.projectSha)
        else
          throw new ScalaNativeBuildError
      }
  }
}
