package scala.cli.commands

import caseapp.core.app.CaseApp
import caseapp.core.RemainingArgs
import coursier.launcher.{AssemblyGenerator, BootstrapGenerator, ClassPathEntry, Parameters, Preamble}
import scala.cli.{Build, Inputs}
import scala.scalanative.{build => sn}
import scala.scalanative.util.Scope

import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.file.{Files, Path, Paths}
import java.nio.file.attribute.FileTime
import java.util.jar.{Attributes => JarAttributes, JarOutputStream}
import java.util.zip.{ZipEntry, ZipOutputStream}

import scala.util.Properties
import scala.cli.internal.ScalaJsLinker

object Package extends CaseApp[PackageOptions] {
  def run(options: PackageOptions, args: RemainingArgs): Unit = {

    val inputs = Inputs(args.all, os.pwd) match {
      case Left(message) =>
        System.err.println(message)
        sys.exit(1)
      case Right(i) => i
    }

    // FIXME mainClass encoding has issues with special chars, such as '-'

    val build = Build.build(inputs, options.shared.buildOptions, options.shared.logger, os.pwd)

    // TODO When possible, call alreadyExistsCheck() before compiling stuff

    def extension =
      if (options.packageType == PackageOptions.PackageType.LibraryJar) ".jar"
      else if (options.packageType == PackageOptions.PackageType.Js) ".js"
      else if (Properties.isWin) (if (options.packageType == PackageOptions.PackageType.Native) ".exe" else ".bat")
      else ""
    val dest = options.output
      .filter(_.nonEmpty)
      .orElse(build.sources.mainClass.map(n => n.drop(n.lastIndexOf('.') + 1) + extension))
      .getOrElse("package.jar")
    val destPath = Paths.get(dest)

    def alreadyExistsCheck(): Unit =
      if (!options.force && Files.exists(destPath)) {
        System.err.println(s"Error: $dest already exists. Pass -f or --force to force erasing it.")
        sys.exit(1)
      }

    alreadyExistsCheck()

    lazy val mainClassOpt =
      options.mainClass.filter(_.nonEmpty) // trim it too?
        .orElse(build.retainedMainClassOpt(warnIfSeveral = true))
    def mainClass() = mainClassOpt.getOrElse(sys.error("No main class"))

    options.packageType match {
      case PackageOptions.PackageType.Bootstrap =>
        bootstrap(build, destPath, mainClass(), () => alreadyExistsCheck())
      case PackageOptions.PackageType.LibraryJar =>
        val content = libraryJar(build)
        alreadyExistsCheck()
        val destPath0 = os.Path(destPath.toAbsolutePath)
        if (options.force) os.write.over(destPath0, content)
        else os.write(destPath0, content)

      case PackageOptions.PackageType.Js =>
        linkJs(build, destPath, Some(mainClass()), addTestInitializer = false)

      case PackageOptions.PackageType.Native =>
        val nativeOptions = options.shared.scalaNativeOptionsIKnowWhatImDoing
        val workDir = options.shared.nativeWorkDir(inputs.workspace, inputs.projectName)
        val logger = options.shared.scalaNativeLogger

        buildNative(build, mainClass(), destPath, nativeOptions, workDir, logger)
    }

    if (options.shared.logging.verbosity >= 0)
      System.err.println(s"Wrote $dest")
  }

  private def libraryJar(build: Build): Array[Byte] = {

    val outputDir = os.Path(build.output)

    val baos = new ByteArrayOutputStream

    val manifest = new java.util.jar.Manifest
    manifest.getMainAttributes.put(JarAttributes.Name.MANIFEST_VERSION, "1.0")
    for (mainClass <- build.sources.mainClass)
      manifest.getMainAttributes.put(JarAttributes.Name.MAIN_CLASS, mainClass)

    var zos: ZipOutputStream = null

    try {
      zos = new JarOutputStream(baos, manifest)
      for (path <- os.walk(outputDir) if os.isFile(path)) {
        val name = path.relativeTo(outputDir).toString
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

  private def bootstrap(build: Build, destPath: Path, mainClass: String, alreadyExistsCheck: () => Unit): Unit = {
    val outputDir = os.Path(build.output)
    val byteCodeZipEntries = os.walk(outputDir)
      .filter(os.isFile(_))
      .map { path =>
        val name = path.relativeTo(outputDir).toString
        val content = os.read.bytes(path)
        val lastModified = os.mtime(path)
        val ent = new ZipEntry(name)
        ent.setLastModifiedTime(FileTime.fromMillis(lastModified))
        ent.setSize(content.length)
        (ent, content)
      }

    // TODO Generate that in memory
    val tmpJar = Files.createTempFile(destPath.getFileName.toString.stripSuffix(".jar"), ".jar")
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
    val byteCodeEntry = ClassPathEntry.Resource(s"${destPath.getFileName}-content.jar", 0L, tmpJarContent)

    val allEntries = Seq(byteCodeEntry) ++ dependencyEntries
    val loaderContent = coursier.launcher.ClassLoaderContent(allEntries)
    val preamble = Preamble()
      .withOsKind(Properties.isWin)
      .callsItself(Properties.isWin)
    val params = Parameters.Bootstrap(Seq(loaderContent), mainClass)
      .withDeterministic(true)
      .withPreamble(preamble)

    alreadyExistsCheck()
    BootstrapGenerator.generate(params, destPath)
  }

  def withLibraryJar[T](build: Build, fileName: String = "library")(f: Path => T): T = {
    val mainJarContent = libraryJar(build)
    val mainJar = Files.createTempFile(fileName.stripSuffix(".jar"), ".jar")
    try {
      Files.write(mainJar, mainJarContent)
      f(mainJar)
    } finally {
      Files.deleteIfExists(mainJar)
    }
  }

  def linkJs(build: Build, dest: Path, mainClassOpt: Option[String], addTestInitializer: Boolean): Unit =
    withLibraryJar(build, dest.getFileName.toString.stripSuffix(".jar")) { mainJar =>
      val classPath = mainJar +: build.artifacts.classPath
      (new ScalaJsLinker).link(classPath.toArray, mainClassOpt, addTestInitializer, dest)
    }

  def buildNative(
    build: Build,
    mainClass: String,
    dest: Path,
    nativeOptions: Build.ScalaNativeOptions,
    nativeWorkDir: os.Path,
    nativeLogger: sn.Logger
  ): Unit = {

    val nativeConfig = sn.NativeConfig.empty
      .withGC(if (nativeOptions.config.gc == "default") sn.GC.default else sn.GC(nativeOptions.config.gc))
      .withMode(sn.Mode(nativeOptions.config.mode))
      .withLinkStubs(false)
      .withClang(nativeOptions.config.clang)
      .withClangPP(nativeOptions.config.clangpp)
      .withLinkingOptions(nativeOptions.config.linkingOptions)
      .withCompileOptions(nativeOptions.config.compileOptions)

    os.makeDir.all(nativeWorkDir)

    withLibraryJar(build, dest.getFileName.toString.stripSuffix(".jar")) { mainJar =>
      val config = sn.Config.empty
        .withCompilerConfig(nativeConfig)
        .withMainClass(mainClass + "$")
        .withClassPath(mainJar +: build.artifacts.classPath)
        .withWorkdir(nativeWorkDir.toNIO)
        .withLogger(nativeLogger)

      Scope { implicit scope =>
        sn.Build.build(config, dest)
      }
    }
  }
}
