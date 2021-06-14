package scala.cli.commands

import caseapp._
import coursier.launcher.{AssemblyGenerator, BootstrapGenerator, ClassPathEntry, Parameters, Preamble}
import packager.BuildSettings
import packager.mac.dmg.DmgPackage
import packager.mac.pkg.PkgPackage
import packager.deb.DebianPackage
import packager.rpm.RedHatPackage
import packager.windows.WindowsPackage
import org.scalajs.linker.interface.StandardConfig
import scala.build.{Build, Inputs, Os}
import scala.build.internal.{ScalaJsConfig, ScalaJsLinker}
import scala.scalanative.{build => sn}
import scala.scalanative.util.Scope

import java.io.{ByteArrayOutputStream, File}
import java.nio.file.attribute.FileTime
import java.nio.file.{Files, Path}
import java.util.jar.{JarOutputStream, Attributes => JarAttributes}
import java.util.zip.{ZipEntry, ZipOutputStream}

import scala.util.Properties

object Package extends ScalaCommand[PackageOptions] {
  override def group = "Main"
  def run(options: PackageOptions, args: RemainingArgs): Unit = {

    val pwd = Os.pwd

    val inputs = Inputs(args.all, pwd, options.shared.directories.directories, defaultInputs = Some(Inputs.default())) match {
      case Left(message) =>
        System.err.println(message)
        sys.exit(1)
      case Right(i) => i
    }

    // FIXME mainClass encoding has issues with special chars, such as '-'

    // TODO Add watch mode

    val buildOptions = options.buildOptions

    val build = Build.build(inputs, buildOptions, options.shared.logger, pwd)

    val successfulBuild = build.successfulOpt.getOrElse {
      System.err.println("Compilation failed")
      sys.exit(1)
    }

    // TODO When possible, call alreadyExistsCheck() before compiling stuff

    def extension =
      if (options.packageType == PackageOptions.PackageType.LibraryJar) ".jar"
      else if (options.packageType == PackageOptions.PackageType.Js) ".js"
      else if (options.packageType == PackageOptions.PackageType.Debian) ".deb"
      else if (options.packageType == PackageOptions.PackageType.Dmg) ".dmg"
      else if (options.packageType == PackageOptions.PackageType.Pkg) ".pkg"
      else if (options.packageType == PackageOptions.PackageType.Rpm) ".rpm"
      else if (options.packageType == PackageOptions.PackageType.Msi) ".msi"
      else if (Properties.isWin) (if (options.packageType == PackageOptions.PackageType.Native) ".exe" else ".bat")
      else ""
    def defaultName =
      if (options.packageType == PackageOptions.PackageType.LibraryJar) "library.jar"
      else if (options.packageType == PackageOptions.PackageType.Js) "app.js"
      else if (options.packageType == PackageOptions.PackageType.Debian) "app.deb"
      else if (options.packageType == PackageOptions.PackageType.Dmg) "app.dmg"
      else if (options.packageType == PackageOptions.PackageType.Pkg) "app.pkg"
      else if (options.packageType == PackageOptions.PackageType.Rpm) "app.rpm"
      else if (options.packageType == PackageOptions.PackageType.Msi) "app.msi"
      else if (Properties.isWin) (if (options.packageType == PackageOptions.PackageType.Native) "app.exe" else "app.bat")
      else "app"
    val dest = options.output
      .filter(_.nonEmpty)
      .orElse(build.sources.mainClass.map(n => n.drop(n.lastIndexOf('.') + 1) + extension))
      .getOrElse(defaultName)
    val destPath = os.Path(dest, pwd)
    val printableDest =
      if (destPath.startsWith(pwd)) "." + File.separator + destPath.relativeTo(pwd).toString
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

    options.packageType match {
      case PackageOptions.PackageType.Bootstrap =>
        bootstrap(successfulBuild, destPath, mainClass(), () => alreadyExistsCheck())
      case PackageOptions.PackageType.LibraryJar =>
        val content = libraryJar(successfulBuild)
        alreadyExistsCheck()
        if (options.force) os.write.over(destPath, content)
        else os.write(destPath, content)

      case PackageOptions.PackageType.Js =>
        linkJs(successfulBuild, destPath, Some(mainClass()), addTestInitializer = false, options.shared.js.config)

      case PackageOptions.PackageType.Native =>
        val config = options.shared.native.config
        val workDir = options.shared.nativeWorkDir(inputs.workspace, inputs.projectName)
        val logger = options.shared.scalaNativeLogger

        buildNative(successfulBuild, mainClass(), destPath, config, workDir, logger)

      case nativePackagerType: PackageOptions.NativePackagerType =>
        val bootstrapPath = os.temp.dir(prefix = "scala-packager") / "app"
        bootstrap(successfulBuild, bootstrapPath, mainClass(), () => alreadyExistsCheck())
        nativePackagerType match {
          case PackageOptions.PackageType.Debian =>
            DebianPackage(bootstrapPath, BuildSettings(force = options.force, outputPath = destPath)).build()
          case PackageOptions.PackageType.Dmg =>
            DmgPackage(bootstrapPath, BuildSettings(force = options.force, outputPath = destPath)).build()
          case PackageOptions.PackageType.Pkg =>
            PkgPackage(bootstrapPath, BuildSettings(force = options.force, outputPath = destPath)).build()
          case PackageOptions.PackageType.Rpm =>
            RedHatPackage(bootstrapPath, BuildSettings(force = options.force, outputPath = destPath)).build()
          case PackageOptions.PackageType.Msi =>
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

  private def sourceJar(build: Build.Successful): Array[Byte] = {

    val baos = new ByteArrayOutputStream
    var zos: ZipOutputStream = null

    def paths = build.sources.paths.iterator ++ build.sources.inMemory.iterator.map(t => (t._1, t._2))

    try {
      zos = new ZipOutputStream(baos)
      for ((path, relPath) <- paths) {
        val name = relPath.toString
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

  def withSourceJar[T](build: Build.Successful, fileName: String = "library")(f: Path => T): T = {
    val jarContent = sourceJar(build)
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
