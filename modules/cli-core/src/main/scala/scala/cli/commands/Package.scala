package scala.cli.commands

import caseapp._
import coursier.launcher.{AssemblyGenerator, BootstrapGenerator, ClassPathEntry, Parameters, Preamble}
import packager.config.BuildSettings
import packager.mac.dmg.DmgPackage
import packager.mac.pkg.PkgPackage
import packager.deb.DebianPackage
import packager.rpm.RedHatPackage
import packager.windows.WindowsPackage
import org.scalajs.linker.interface.StandardConfig
import scala.build.{Build, Inputs, Os}
import scala.build.internal.ScalaJsConfig
import scala.build.options.PackageType
import scala.cli.internal.ScalaJsLinker
import scala.scalanative.{build => sn}
import scala.scalanative.util.Scope

import java.io.{ByteArrayOutputStream, File}
import java.nio.charset.StandardCharsets
import java.nio.file.attribute.FileTime
import java.nio.file.{Files, Path}
import java.util.jar.{JarOutputStream, Attributes => JarAttributes}
import java.util.zip.{ZipEntry, ZipOutputStream}

import scala.util.Properties

object Package extends ScalaCommand[PackageOptions] {
  override def group = "Main"
  override def sharedOptions(options: PackageOptions) = Some(options.shared)
  def run(options: PackageOptions, args: RemainingArgs): Unit = {

    val inputs = options.shared.inputsOrExit(args, defaultInputs = Some(Inputs.default()))

    // FIXME mainClass encoding has issues with special chars, such as '-'

    // TODO Add watch mode

    val buildOptions = options.buildOptions
    val bloopRifleConfig = options.shared.bloopRifleConfig()

    val build = Build.build(inputs, buildOptions, bloopRifleConfig, options.shared.logger)

    val successfulBuild = build.successfulOpt.getOrElse {
      System.err.println("Compilation failed")
      sys.exit(1)
    }

    val packageType = options.packageTypeOpt.getOrElse(build.options.defaultPackageType)

    // TODO When possible, call alreadyExistsCheck() before compiling stuff

    def extension = packageType match {
      case PackageType.LibraryJar => ".jar"
      case PackageType.Assembly => ".jar"
      case PackageType.Js => ".js"
      case PackageType.Debian => ".deb"
      case PackageType.Dmg => ".dmg"
      case PackageType.Pkg => ".pkg"
      case PackageType.Rpm => ".rpm"
      case PackageType.Msi => ".msi"
      case PackageType.Native if Properties.isWin => ".exe"
      case _ if Properties.isWin => ".bat"
      case _ => ""
    }
    def defaultName = packageType match {
      case PackageType.LibraryJar => "library.jar"
      case PackageType.Assembly => "app.jar"
      case PackageType.Js => "app.js"
      case PackageType.Debian => "app.deb"
      case PackageType.Dmg => "app.dmg"
      case PackageType.Pkg => "app.pkg"
      case PackageType.Rpm => "app.rpm"
      case PackageType.Msi => "app.msi"
      case PackageType.Native if Properties.isWin => "app.exe"
      case _ if Properties.isWin => "app.bat"
      case _ => "app"
    }

    val dest = options.output
      .filter(_.nonEmpty)
      .orElse(build.sources.mainClass.map(n => n.drop(n.lastIndexOf('.') + 1) + extension))
      .getOrElse(defaultName)
    val destPath = os.Path(dest, Os.pwd)
    val printableDest =
      if (destPath.startsWith(Os.pwd)) "." + File.separator + destPath.relativeTo(Os.pwd).toString
      else destPath.toString

    def alreadyExistsCheck(): Unit =
      if (!options.force && os.exists(destPath)) {
        System.err.println(s"Error: $printableDest already exists. Pass -f or --force to force erasing it.")
        sys.exit(1)
      }

    alreadyExistsCheck()

    lazy val mainClassOpt =
      options.mainClass.filter(_.nonEmpty) // trim it too?
        .orElse(successfulBuild.retainedMainClassOpt(warnIfSeveral = true))
    def mainClass() = mainClassOpt.getOrElse(sys.error("No main class"))

    packageType match {
      case PackageType.Bootstrap =>
        bootstrap(successfulBuild, destPath, mainClass(), () => alreadyExistsCheck())
      case PackageType.LibraryJar =>
        val content = libraryJar(successfulBuild)
        alreadyExistsCheck()
        if (options.force) os.write.over(destPath, content)
        else os.write(destPath, content)
      case PackageType.Assembly =>
        assembly(successfulBuild, destPath, mainClass(), () => alreadyExistsCheck())

      case PackageType.Js =>
        val linkerConfig = successfulBuild.options.scalaJsOptions.linkerConfig
        linkJs(successfulBuild, destPath, Some(mainClass()), addTestInitializer = false, linkerConfig)

      case PackageType.Native =>
        val config = successfulBuild.options.scalaNativeOptions.config.getOrElse(???)
        val workDir = options.shared.nativeWorkDir(inputs.workspace, inputs.projectName)
        val logger = options.shared.logger.scalaNativeLogger

        buildNative(successfulBuild, mainClass(), destPath, config, workDir, logger)

      case nativePackagerType: PackageType.NativePackagerType =>
        val bootstrapPath = os.temp.dir(prefix = "scala-packager") / "app"
        bootstrap(successfulBuild, bootstrapPath, mainClass(), () => alreadyExistsCheck())
        nativePackagerType match {
          case PackageType.Debian =>
            DebianPackage(bootstrapPath, BuildSettings(force = options.force, outputPath = destPath)).build()
          case PackageType.Dmg =>
            DmgPackage(bootstrapPath, BuildSettings(force = options.force, outputPath = destPath)).build()
          case PackageType.Pkg =>
            PkgPackage(bootstrapPath, BuildSettings(force = options.force, outputPath = destPath)).build()
          case PackageType.Rpm =>
            RedHatPackage(bootstrapPath, BuildSettings(force = options.force, outputPath = destPath)).build()
          case PackageType.Msi =>
            WindowsPackage(bootstrapPath, BuildSettings(force = options.force, outputPath = destPath)).build()
        }
    }

    if (options.shared.logging.verbosity >= 0)
      System.err.println(s"Wrote $dest")
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
        val name = path.relativeTo(build.output).toString
        val lastModified = os.mtime(path)
        val ent = new ZipEntry(name)
        ent.setLastModifiedTime(FileTime.fromMillis(lastModified))

        val content = os.read.bytes(path)
        ent.setSize(content.length)

        zos.putNextEntry(ent)
        zos.write(content)
        zos.closeEntry()
      }
    } finally {
      if (zos != null)
        zos.close()
    }

    baos.toByteArray
  }

  private def sourceJar(build: Build.Successful, defaultLastModified: Long): Array[Byte] = {

    val baos = new ByteArrayOutputStream
    var zos: ZipOutputStream = null

    def fromSimpleSources = build.sources.paths.iterator.map {
      case (path, relPath) =>
        val lastModified = os.mtime(path)
        val content = os.read.bytes(path)
        (relPath, content, lastModified)
    }

    def fromGeneratedSources = build.sources.inMemory.iterator.map {
      case (Right(path), relPath, _, _) =>
        val lastModified = os.mtime(path)
        val content = os.read.bytes(path)
        (relPath, content, lastModified)
      case (Left(_), relPath, content, _) =>
        (relPath, content.getBytes(StandardCharsets.UTF_8), defaultLastModified)
    }

    def paths = fromSimpleSources ++ fromGeneratedSources

    try {
      zos = new ZipOutputStream(baos)
      for ((relPath, content, lastModified) <- paths) {
        val name = relPath.toString
        val ent = new ZipEntry(name)
        ent.setLastModifiedTime(FileTime.fromMillis(lastModified))
        ent.setSize(content.length)

        zos.putNextEntry(ent)
        zos.write(content)
        zos.closeEntry()
      }
    } finally {
      if (zos != null)
        zos.close()
    }

    baos.toByteArray
  }

  private def bootstrap(build: Build.Successful, destPath: os.Path, mainClass: String, alreadyExistsCheck: () => Unit): Unit = {
    val byteCodeZipEntries = os.walk(build.output)
      .filter(os.isFile(_))
      .map { path =>
        val name = path.relativeTo(build.output).toString
        val content = os.read.bytes(path)
        val lastModified = os.mtime(path)
        val ent = new ZipEntry(name)
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

    def dependencyEntries = build.artifacts.artifacts.map {
      case (url, _) =>
        ClassPathEntry.Url(url)
    }
    val byteCodeEntry = ClassPathEntry.Resource(s"${destPath.last}-content.jar", 0L, tmpJarContent)

    val allEntries = Seq(byteCodeEntry) ++ dependencyEntries
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

  private def assembly(build: Build.Successful, destPath: os.Path, mainClass: String, alreadyExistsCheck: () => Unit): Unit = {
    val byteCodeZipEntries = os.walk(build.output)
      .filter(os.isFile(_))
      .map { path =>
        val name = path.relativeTo(build.output).toString
        val content = os.read.bytes(path)
        val lastModified = os.mtime(path)
        val ent = new ZipEntry(name)
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
    val mainJar = Files.createTempFile(fileName.stripSuffix(".jar"), ".jar")
    try {
      Files.write(mainJar, mainJarContent)
      f(mainJar)
    } finally {
      Files.deleteIfExists(mainJar)
    }
  }

  def withSourceJar[T](build: Build.Successful, defaultLastModified: Long, fileName: String = "library")(f: Path => T): T = {
    val jarContent = sourceJar(build, defaultLastModified)
    val jar = Files.createTempFile(fileName.stripSuffix(".jar"), "-sources.jar")
    try {
      Files.write(jar, jarContent)
      f(jar)
    } finally {
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
      (new ScalaJsLinker).link(classPath.toArray, mainClassOpt.orNull, addTestInitializer, new ScalaJsConfig(config), dest.toNIO)
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
