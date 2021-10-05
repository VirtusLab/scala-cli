package scala.cli.commands

import caseapp._
import coursier.launcher.{
  AssemblyGenerator,
  BootstrapGenerator,
  ClassPathEntry,
  Parameters,
  Preamble
}
import org.scalajs.linker.interface.StandardConfig
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
import java.util.jar.{Attributes => JarAttributes, JarOutputStream}
import java.util.zip.{ZipEntry, ZipOutputStream}

import scala.build.internal.ScalaJsConfig
import scala.build.options.PackageType
import scala.build.{Build, Inputs, Logger, Os}
import scala.cli.commands.OptionsHelper._
import scala.cli.internal.{GetImageResizer, ScalaJsLinker}
import scala.scalanative.util.Scope
import scala.scalanative.{build => sn}
import scala.util.Properties

object Package extends ScalaCommand[PackageOptions] {
  override def group                                  = "Main"
  override def sharedOptions(options: PackageOptions) = Some(options.shared)
  def run(options: PackageOptions, args: RemainingArgs): Unit = {

    val inputs = options.shared.inputsOrExit(args)

    // FIXME mainClass encoding has issues with special chars, such as '-'

    // TODO Add watch mode

    val initialBuildOptions = options.buildOptions
    val bloopRifleConfig    = options.shared.bloopRifleConfig()
    val logger              = options.shared.logger

    val cross = options.compileCross.cross.getOrElse(false)

    if (options.watch.watch) {
      val watcher = Build.watch(
        inputs,
        initialBuildOptions,
        bloopRifleConfig,
        logger,
        crossBuilds = cross,
        postAction = () => WatchUtil.printWatchMessage()
      ) { res =>
        res.orReport(logger).map(_._1).foreach {
          case s: Build.Successful =>
            doPackage(inputs, logger, options.output.filter(_.nonEmpty), options.force, s)
          case _: Build.Failed =>
            System.err.println("Compilation failed")
        }
      }
      try WatchUtil.waitForCtrlC()
      finally watcher.dispose()
    }
    else {
      val (build, _) =
        Build.build(inputs, initialBuildOptions, bloopRifleConfig, logger, crossBuilds = cross)
          .orExit(logger)
      build match {
        case s: Build.Successful =>
          doPackage(inputs, logger, options.output.filter(_.nonEmpty), options.force, s)
        case _: Build.Failed =>
          System.err.println("Compilation failed")
          sys.exit(1)
      }
    }
  }

  private def doPackage(
    inputs: Inputs,
    logger: Logger,
    outputOpt: Option[String],
    force: Boolean,
    build: Build.Successful
  ): Unit = {

    val packageType = build.options.packageTypeOpt
      .getOrElse(PackageType.Bootstrap)

    // TODO When possible, call alreadyExistsCheck() before compiling stuff

    def extension = packageType match {
      case PackageType.LibraryJar                 => ".jar"
      case PackageType.Assembly                   => ".jar"
      case PackageType.Js                         => ".js"
      case PackageType.Debian                     => ".deb"
      case PackageType.Dmg                        => ".dmg"
      case PackageType.Pkg                        => ".pkg"
      case PackageType.Rpm                        => ".rpm"
      case PackageType.Msi                        => ".msi"
      case PackageType.Native if Properties.isWin => ".exe"
      case _ if Properties.isWin                  => ".bat"
      case _                                      => ""
    }
    def defaultName = packageType match {
      case PackageType.LibraryJar                 => "library.jar"
      case PackageType.Assembly                   => "app.jar"
      case PackageType.Js                         => "app.js"
      case PackageType.Debian                     => "app.deb"
      case PackageType.Dmg                        => "app.dmg"
      case PackageType.Pkg                        => "app.pkg"
      case PackageType.Rpm                        => "app.rpm"
      case PackageType.Msi                        => "app.msi"
      case PackageType.Native if Properties.isWin => "app.exe"
      case _ if Properties.isWin                  => "app.bat"
      case _                                      => "app"
    }

    val dest = outputOpt
      .orElse(build.sources.mainClass.map(n => n.drop(n.lastIndexOf('.') + 1) + extension))
      .getOrElse(defaultName)
    val destPath = os.Path(dest, Os.pwd)
    val printableDest =
      if (destPath.startsWith(Os.pwd)) "." + File.separator + destPath.relativeTo(Os.pwd).toString
      else destPath.toString

    def alreadyExistsCheck(): Unit =
      if (!force && os.exists(destPath)) {
        System.err.println(
          s"Error: $printableDest already exists. Pass -f or --force to force erasing it."
        )
        sys.exit(1)
      }

    alreadyExistsCheck()

    lazy val mainClassOpt =
      build.options.mainClass
        .orElse(build.retainedMainClassOpt(warnIfSeveral = true))
    def mainClass() = mainClassOpt.getOrElse(sys.error("No main class"))

    packageType match {
      case PackageType.Bootstrap =>
        bootstrap(build, destPath, mainClass(), () => alreadyExistsCheck())
      case PackageType.LibraryJar =>
        val content = libraryJar(build)
        alreadyExistsCheck()
        if (force) os.write.over(destPath, content)
        else os.write(destPath, content)
      case PackageType.Assembly =>
        assembly(build, destPath, mainClass(), () => alreadyExistsCheck())

      case PackageType.Js =>
        buildJs(build, destPath, mainClass())

      case PackageType.Native =>
        buildNative(inputs, build, destPath, mainClass(), logger)
      case nativePackagerType: PackageType.NativePackagerType =>
        val bootstrapPath = os.temp.dir(prefix = "scala-packager") / "app"
        bootstrap(build, bootstrapPath, mainClass(), () => alreadyExistsCheck())
        val sharedSettings = SharedSettings(
          sourceAppPath = bootstrapPath,
          version = build.options.packageOptions.packageVersion,
          force = force,
          outputPath = destPath,
          logoPath = build.options.packageOptions.logoPath,
          launcherApp = build.options.packageOptions.launcherApp
        )
        val packageOptions = build.options.packageOptions

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
            val imageResizerOpt = Option((new GetImageResizer).get())
            WindowsPackage(
              windowsSettings,
              imageResizerOpt = imageResizerOpt
            ).build()
        }
      case PackageType.Docker =>
        docker(inputs, build, mainClass(), logger)
    }

    if (!build.options.packageOptions.isDockerEnabled) {
      logger.message {
        if (packageType.runnable)
          s"Wrote $dest, run it with" + System.lineSeparator() +
            "  " + printableDest
        else if (packageType == PackageType.Js)
          s"Wrote $dest, run it with" + System.lineSeparator() +
            "  node " + printableDest
        else
          s"Wrote $dest"
      }
    }
  }

  private def libraryJar(build: Build.Successful): Array[Byte] = {

    val baos = new ByteArrayOutputStream

    val manifest = new java.util.jar.Manifest
    manifest.getMainAttributes.put(JarAttributes.Name.MANIFEST_VERSION, "1.0")
    for (mainClass <- build.sources.mainClass)
      manifest.getMainAttributes.put(JarAttributes.Name.MAIN_CLASS, mainClass)

    var zos: ZipOutputStream = null

    try {
      zos = new JarOutputStream(baos, manifest)
      for (path <- os.walk(build.output) if os.isFile(path)) {
        val name         = path.relativeTo(build.output).toString
        val lastModified = os.mtime(path)
        val ent          = new ZipEntry(name)
        ent.setLastModifiedTime(FileTime.fromMillis(lastModified))

        val content = os.read.bytes(path)
        ent.setSize(content.length)

        zos.putNextEntry(ent)
        zos.write(content)
        zos.closeEntry()
      }
    }
    finally {
      if (zos != null)
        zos.close()
    }

    baos.toByteArray
  }

  private def sourceJar(build: Build.Successful, defaultLastModified: Long): Array[Byte] = {

    val baos                 = new ByteArrayOutputStream
    var zos: ZipOutputStream = null

    def fromSimpleSources = build.sources.paths.iterator.map {
      case (path, relPath) =>
        val lastModified = os.mtime(path)
        val content      = os.read.bytes(path)
        (relPath, content, lastModified)
    }

    def fromGeneratedSources = build.sources.inMemory.iterator.map {
      case (Right(path), relPath, _, _) =>
        val lastModified = os.mtime(path)
        val content      = os.read.bytes(path)
        (relPath, content, lastModified)
      case (Left(_), relPath, content, _) =>
        (relPath, content.getBytes(StandardCharsets.UTF_8), defaultLastModified)
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
    finally {
      if (zos != null)
        zos.close()
    }

    baos.toByteArray
  }

  private def docker(
    inputs: Inputs,
    build: Build.Successful,
    mainClass: String,
    logger: Logger
  ): Unit = {
    if (build.options.scalaNativeOptions.enable && (Properties.isMac || Properties.isWin)) {
      System.err.println(
        "Package scala native application to docker image is not supported on MacOs and Windows"
      )
      sys.exit(1)
    }

    val exec =
      if (build.options.scalaJsOptions.enable) Some("node")
      else if (build.options.scalaNativeOptions.enable) None
      else Some("sh")
    val from = build.options.packageOptions.dockerOptions.from match {
      case Some(baseImage) => baseImage
      case None =>
        if (build.options.scalaJsOptions.enable) "node"
        else if (build.options.scalaNativeOptions.enable)
          "debian:stable-slim"
        else "openjdk:8-jre-slim"
    }
    val repository = build.options.packageOptions.dockerOptions.imageRepository.mandatory(
      "--docker-image-repository",
      "docker"
    )
    val tag = build.options.packageOptions.dockerOptions.imageTag.getOrElse("latest")

    val dockerSettings = DockerSettings(
      from = from,
      registry = build.options.packageOptions.dockerOptions.imageRegistry,
      repository = repository,
      tag = Some(tag),
      exec = exec
    )

    val appPath = os.temp.dir(prefix = "scala-cli-docker") / "app"
    if (build.options.scalaJsOptions.enable) buildJs(build, appPath, mainClass)
    else if (build.options.scalaNativeOptions.enable)
      buildNative(inputs, build, appPath, mainClass, logger)
    else bootstrap(build, appPath, mainClass, () => false)

    logger.message(
      "Started building docker image with your application, it would take some time"
    )

    DockerPackage(appPath, dockerSettings).build()

    logger.message(
      "Built docker image, run it with" + System.lineSeparator() +
        s"  docker run $repository:$tag"
    )
  }

  private def buildJs(build: Build.Successful, destPath: os.Path, mainClass: String): Unit = {
    val linkerConfig = build.options.scalaJsOptions.linkerConfig
    linkJs(build, destPath, Some(mainClass), addTestInitializer = false, linkerConfig)
  }

  private def buildNative(
    inputs: Inputs,
    build: Build.Successful,
    destPath: os.Path,
    mainClass: String,
    logger: Logger
  ): Unit = {
    val config = build.options.scalaNativeOptions.config.getOrElse(???)
    val workDir =
      build.options.scalaNativeOptions.nativeWorkDir(inputs.workspace, inputs.projectName)

    buildNative(build, mainClass, destPath, config, workDir, logger.scalaNativeLogger)
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
        case (url, artifactPath) =>
          if (build.options.packageOptions.isStandalone) {
            val path = os.Path(artifactPath)
            ClassPathEntry.Resource(path.last, os.mtime(path), os.read.bytes(path))
          }
          else {
            ClassPathEntry.Url(url)
          }
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
      .withFiles(build.artifacts.artifacts.map(_._2.toFile))
      .withMainClass(mainClass)
      .withPreamble(preamble)
    alreadyExistsCheck()
    AssemblyGenerator.generate(params, destPath.toNIO)
  }

  def withLibraryJar[T](build: Build.Successful, fileName: String = "library")(f: Path => T): T = {
    val mainJarContent = libraryJar(build)
    val mainJar        = Files.createTempFile(fileName.stripSuffix(".jar"), ".jar")
    try {
      Files.write(mainJar, mainJarContent)
      f(mainJar)
    }
    finally {
      Files.deleteIfExists(mainJar)
    }
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
    finally {
      Files.deleteIfExists(jar)
    }
  }

  def linkJs(
    build: Build.Successful,
    dest: os.Path,
    mainClassOpt: Option[String],
    addTestInitializer: Boolean,
    config: StandardConfig
  ): Unit =
    withLibraryJar(build, dest.last.toString.stripSuffix(".jar")) { mainJar =>
      val classPath = mainJar +: build.artifacts.classPath
      (new ScalaJsLinker).link(
        classPath.toArray,
        mainClassOpt.orNull,
        addTestInitializer,
        new ScalaJsConfig(config),
        dest.toNIO
      )
    }

  def buildNative(
    build: Build.Successful,
    mainClass: String,
    dest: os.Path,
    nativeConfig: sn.NativeConfig,
    nativeWorkDir: os.Path,
    nativeLogger: sn.Logger
  ): Unit = {

    os.makeDir.all(nativeWorkDir)

    withLibraryJar(build, dest.last.stripSuffix(".jar")) { mainJar =>
      val config = sn.Config.empty
        .withCompilerConfig(nativeConfig)
        .withMainClass(mainClass + "$")
        .withClassPath(mainJar +: build.artifacts.classPath)
        .withWorkdir(nativeWorkDir.toNIO)
        .withLogger(nativeLogger)

      Scope { implicit scope =>
        sn.Build.build(config, dest.toNIO)
      }
    }
  }
}
