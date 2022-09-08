import $ivy.`com.goyeau::mill-scalafix::0.2.8`
import $ivy.`io.github.alexarchambault.mill::mill-native-image::0.1.19`
import $ivy.`io.github.alexarchambault.mill::mill-scala-cli::0.1.0`

import $file.deps, deps.{BuildDeps, Deps, Docker, alpineVersion, buildCsVersion, libsodiumVersion}
import $file.scalafixthings, scalafixthings.ScalafixModule

import de.tobiasroeser.mill.vcs.version.VcsVersion
import io.github.alexarchambault.millnativeimage.NativeImage
import java.io.{ByteArrayOutputStream, File, InputStream}
import java.nio.charset.StandardCharsets
import java.util.Locale
import mill._, scalalib._
import scala.collection.JavaConverters._
import scala.util.Properties
import upickle.default._

private def isCI = System.getenv("CI") != null

def fromPath(name: String): String =
  if (Properties.isWin) {
    val pathExt = Option(System.getenv("PATHEXT"))
      .toSeq
      .flatMap(_.split(File.pathSeparator).toSeq)
    val path = Seq(new File("").getAbsoluteFile) ++
      Option(System.getenv("PATH"))
        .toSeq
        .flatMap(_.split(File.pathSeparator))
        .map(new File(_))

    def candidates =
      for {
        dir <- path.iterator
        ext <- pathExt.iterator
      } yield new File(dir, name + ext)

    candidates
      .filter(_.canExecute)
      .toStream
      .headOption
      .map(_.getAbsolutePath)
      .getOrElse {
        System.err.println(s"Warning: could not find $name in PATH.")
        name
      }
  }
  else
    name

def cs: T[String] = T.persistent {

  val ext  = if (Properties.isWin) ".exe" else ""
  val dest = T.dest / s"cs-$buildCsVersion$ext"

  def downloadOpt(): Option[String] = {
    val arch = sys.props.getOrElse("os.arch", "").toLowerCase(Locale.ROOT)
    val urlOpt = arch match {
      case "x86_64" | "amd64" =>
        if (Properties.isWin)
          Some(
            s"https://github.com/coursier/coursier/releases/download/v$buildCsVersion/cs-x86_64-pc-win32.zip"
          )
        else if (Properties.isMac)
          Some(
            s"https://github.com/coursier/coursier/releases/download/v$buildCsVersion/cs-x86_64-apple-darwin.gz"
          )
        else if (Properties.isLinux)
          Some(
            s"https://github.com/coursier/coursier/releases/download/v$buildCsVersion/cs-x86_64-pc-linux.gz"
          )
        else None
      case "aarch64" =>
        if (Properties.isLinux)
          Some(
            s"https://github.com/coursier/coursier/releases/download/v$buildCsVersion/cs-aarch64-pc-linux.gz"
          )
        else None
      case _ =>
        None
    }

    urlOpt.map { url =>
      val cache        = coursier.cache.FileCache()
      val archiveCache = coursier.cache.ArchiveCache().withCache(cache)
      val task         = cache.logger.using(archiveCache.get(coursier.util.Artifact(url)))
      val maybeFile =
        try task.unsafeRun()(cache.ec)
        catch {
          case t: Throwable =>
            throw new Exception(s"Error getting and extracting $url", t)
        }
      val f = maybeFile.fold(ex => throw new Exception(ex), os.Path(_, os.pwd))
      val exec =
        if (Properties.isWin && os.isDir(f) && f.last.endsWith(".zip"))
          os.list(f).find(_.last.endsWith(".exe")).getOrElse(
            sys.error(s"No .exe found under $f")
          )
        else
          f

      if (!Properties.isWin)
        exec.toIO.setExecutable(true)

      exec.toString
    }
  }

  if (os.isFile(dest))
    dest.toString
  else
    downloadOpt().getOrElse(fromPath("cs")): String
}

def platformExtension: String =
  if (Properties.isWin) ".exe"
  else ""

def platformExecutableJarExtension: String =
  if (Properties.isWin) ".bat"
  else ""

lazy val arch = sys.props("os.arch").toLowerCase(java.util.Locale.ROOT) match {
  case "amd64" => "x86_64"
  case other   => other
}
def platformSuffix: String = {
  val os =
    if (Properties.isWin) "pc-win32"
    else if (Properties.isLinux) "pc-linux"
    else if (Properties.isMac) "apple-darwin"
    else sys.error(s"Unrecognized OS: ${sys.props("os.name")}")
  s"$arch-$os"
}

def localRepoResourcePath = "local-repo.zip"

def getGhToken(): String =
  Option(System.getenv("UPLOAD_GH_TOKEN"))
    .getOrElse {
      sys.error("UPLOAD_GH_TOKEN not set")
    }

trait CliLaunchers extends SbtModule { self =>

  def launcherTypeResourcePath = os.rel / "scala" / "cli" / "internal" / "launcher-type.txt"
  def defaultFilesResourcePath = os.rel / "scala" / "cli" / "commands" / "publish"

  trait CliNativeImage extends NativeImage {
    def launcherKind: String
    def nativeImageCsCommand    = Seq(cs())
    def nativeImagePersist      = System.getenv("CI") != null
    def nativeImageGraalVmJvmId = deps.graalVmJvmId
    def nativeImageOptions = T {
      val usesDocker = nativeImageDockerParams().nonEmpty
      val cLibPath =
        if (usesDocker) s"/data/$staticLibDirName"
        else staticLibDir().path.toString
      super.nativeImageOptions() ++ Seq(
        s"-H:IncludeResources=$localRepoResourcePath",
        s"-H:IncludeResources=$launcherTypeResourcePath",
        s"-H:IncludeResources=$defaultFilesResourcePath/.*",
        "-H:-ParseRuntimeOptions",
        s"-H:CLibraryPath=$cLibPath"
      )
    }
    def nativeImageName = "scala-cli"
    def nativeImageClassPath = T {
      val launcherKindResourceDir = T.dest / "resources"
      os.write(
        launcherKindResourceDir / launcherTypeResourcePath,
        launcherKind,
        createFolders = true
      )
      PathRef(launcherKindResourceDir) +: self.nativeImageClassPath()
    }
    def nativeImageMainClass = self.nativeImageMainClass()

    private def staticLibDirName = "native-libs"

    private def copyCsjniutilTo(cs: String, destDir: os.Path): Unit = {
      val jniUtilsVersion = Deps.jniUtils.dep.version
      val libRes = os.proc(
        cs,
        "fetch",
        "--intransitive",
        s"io.get-coursier.jniutils:windows-jni-utils:$jniUtilsVersion,classifier=x86_64-pc-win32,ext=lib,type=lib",
        "-A",
        "lib"
      ).call()
      val libPath = os.Path(libRes.out.text().trim, os.pwd)
      os.copy.over(libPath, destDir / "csjniutils.lib")
    }
    private def copyLibsodiumjniTo(cs: String, destDir: os.Path): Unit = {
      val libsodiumjniVersion = Deps.libsodiumjni.dep.version
      val (classifier, ext) = sys.props.get("os.arch") match {
        case Some("x86_64" | "amd64") =>
          if (Properties.isWin) ("x86_64-pc-win32", "lib")
          else if (Properties.isLinux) ("x86_64-pc-linux", "a")
          else if (Properties.isMac) ("x86_64-apple-darwin", "a")
          else sys.error(s"Unsupported OS for x86_64 platform: ${sys.props("os.name")}")
        case Some("aarch64") =>
          if (Properties.isLinux) ("aarch64-pc-linux", "a")
          else sys.error(s"Unsupported OS for aarch64 platform: ${sys.props("os.name")}")
        case Some(arch) =>
          sys.error(s"Unsupported architecture: $arch")
        case None =>
          sys.error("Cannot determine CPU architecture")
      }
      val libRes = os.proc(
        cs,
        "fetch",
        "--intransitive",
        s"io.github.alexarchambault.tmp.libsodiumjni:libsodiumjni:$libsodiumjniVersion,classifier=$classifier,ext=$ext,type=$ext",
        "-A",
        ext
      ).call()
      val libPath = os.Path(libRes.out.text().trim, os.pwd)
      val prefix =
        if (Properties.isWin) ""
        else "lib"
      os.copy.over(libPath, destDir / s"${prefix}sodiumjni.$ext")
    }
    private def copyLibsodiumStaticTo(cs: String, destDir: os.Path): Unit = {
      val dirRes = os.proc(
        cs,
        "get",
        "--archive",
        "https://download.libsodium.org/libsodium/releases/libsodium-1.0.18-stable-msvc.zip"
      ).call()
      val dir = os.Path(dirRes.out.text().trim, os.pwd)
      os.copy.over(
        dir / "libsodium" / "x64" / "Release" / "v143" / "static" / "libsodium.lib",
        destDir / "sodium.lib"
      )
    }
    private def copyAlpineLibsodiumTo(cs: String, destDir: os.Path): Unit = {
      val arcPath = os.proc(
        cs,
        "get",
        s"https://dl-cdn.alpinelinux.org/alpine/v$alpineVersion/main/x86_64/libsodium-static-$libsodiumVersion-r0.apk"
      ).call().out.text().trim
      val tmpDir = os.temp.dir(prefix = "libsodium-static")
      try {
        os.proc("tar", "-zxf", os.Path(arcPath, os.pwd))
          .call(cwd = tmpDir, stdout = os.Inherit)
        os.copy.over(tmpDir / "usr" / "lib" / "libsodium.a", destDir / "libsodium.a")
      }
      finally
        os.remove.all(tmpDir)

      // The static libsodium has a symbol that conflicts with one from a native-image-injected
      // library ('initialize'). It seems libsodium is making some effort to namespace its symbols,
      // (jedisct1/libsodium#839) so I'm not sure why this one ends up here.
      // It seems to be an internal thing, so we use objcopy to rename it and work around the conflict
      // (see https://stackoverflow.com/questions/678254/what-should-i-do-if-two-libraries-provide-a-function-with-the-same-name-generati/678375#678375).
      val proc = os.proc(
        "objcopy",
        "--redefine-sym",
        "initialize=__sodium_thing_initialize",
        destDir / "libsodium.a"
      )
      System.err.println(s"Calling ${proc.command.flatMap(_.value).mkString(" ")}")
      proc.call(stdin = os.Inherit, stdout = os.Inherit)
    }
    def staticLibDir = T {
      val dir = nativeImageDockerWorkingDir() / staticLibDirName
      os.makeDir.all(dir)

      if (Properties.isWin) {
        copyLibsodiumStaticTo(cs(), dir)
        copyLibsodiumjniTo(cs(), dir)
        copyCsjniutilTo(cs(), dir)
      }

      if (launcherKind == "static") {
        copyAlpineLibsodiumTo(cs(), dir)
        copyLibsodiumjniTo(cs(), dir)
      }

      PathRef(dir)
    }
  }

  object `base-image` extends CliNativeImage {
    def launcherKind = "default"
  }

  private def maybePassNativeImageJpmsOption =
    Option(System.getenv("USE_NATIVE_IMAGE_JAVA_PLATFORM_MODULE_SYSTEM"))
      .fold("") { value =>
        "export USE_NATIVE_IMAGE_JAVA_PLATFORM_MODULE_SYSTEM=" + value + System.lineSeparator()
      }

  object `linux-docker-image` extends CliNativeImage {
    def launcherKind = `base-image`.launcherKind
    def nativeImageDockerParams = Some(
      NativeImage.DockerParams(
        imageName = "ubuntu:18.04",
        prepareCommand =
          maybePassNativeImageJpmsOption +
            """apt-get update -q -y &&\
              |apt-get install -q -y build-essential libz-dev locales
              |locale-gen en_US.UTF-8
              |export LANG=en_US.UTF-8
              |export LANGUAGE=en_US:en
              |export LC_ALL=en_US.UTF-8""".stripMargin,
        csUrl =
          s"https://github.com/coursier/coursier/releases/download/v${deps.csDockerVersion}/cs-x86_64-pc-linux.gz",
        extraNativeImageArgs = Nil
      )
    )
  }

  private def setupLocaleAndOptions(params: NativeImage.DockerParams): NativeImage.DockerParams =
    params.copy(
      prepareCommand = maybePassNativeImageJpmsOption +
        params.prepareCommand +
        """
          |set -v
          |apt-get update
          |apt-get install -q -y locales
          |locale-gen en_US.UTF-8
          |export LANG=en_US.UTF-8
          |export LANGUAGE=en_US:en
          |export LC_ALL=en_US.UTF-8""".stripMargin
    )

  object `static-image` extends CliNativeImage {
    def launcherKind = "static"
    def nativeImageOptions = T {
      super.nativeImageOptions() ++ Seq(
        "-J-Dscala-cli.static-launcher=true"
      )
    }
    def nativeImageDockerParams = T {
      val baseDockerParams = NativeImage.linuxStaticParams(
        Docker.muslBuilder,
        s"https://github.com/coursier/coursier/releases/download/v${deps.csDockerVersion}/cs-x86_64-pc-linux.gz"
      )
      val dockerParams = setupLocaleAndOptions(baseDockerParams)
      buildHelperImage()
      Some(dockerParams)
    }
    def buildHelperImage = T {
      os.proc("docker", "build", "-t", Docker.customMuslBuilderImageName, ".")
        .call(cwd = os.pwd / "project" / "musl-image", stdout = os.Inherit)
      ()
    }
    def writeNativeImageScript(scriptDest: String, imageDest: String = "") = T.command {
      buildHelperImage()
      super.writeNativeImageScript(scriptDest, imageDest)()
    }
  }

  object `mostly-static-image` extends CliNativeImage {
    def launcherKind = "mostly-static"
    def nativeImageDockerParams = T {
      val baseDockerParams = NativeImage.linuxMostlyStaticParams(
        "ubuntu:18.04", // TODO Pin that
        s"https://github.com/coursier/coursier/releases/download/v${deps.csDockerVersion}/cs-x86_64-pc-linux.gz"
      )
      val dockerParams = setupLocaleAndOptions(baseDockerParams)
      Some(dockerParams)
    }
  }

  def localRepoJar: T[PathRef]

  def nativeImageMainClass = T {
    mainClass().getOrElse(sys.error("Don't know what main class to use"))
  }

  def transitiveJars: T[Agg[PathRef]] = {

    def allModuleDeps(todo: List[JavaModule]): List[JavaModule] =
      todo match {
        case Nil => Nil
        case h :: t =>
          h :: allModuleDeps(h.moduleDeps.toList ::: t)
      }

    T {
      mill.define.Target.traverse(allModuleDeps(this :: Nil).distinct)(m =>
        T.task(m.jar())
      )()
    }
  }

  def nativeImageClassPath = T {
    val localRepoJar0 = localRepoJar()
    runClasspath() :+ localRepoJar0 // isn't localRepoJar already there?
  }

  def nativeImage =
    if (Properties.isLinux && arch == "x86_64" && isCI)
      `linux-docker-image`.nativeImage
    else
      `base-image`.nativeImage

  def nativeImageStatic =
    `static-image`.nativeImage
  def nativeImageMostlyStatic =
    `mostly-static-image`.nativeImage

  def runWithAssistedConfig(args: String*) = T.command {
    val cp         = jarClassPath().map(_.path).mkString(File.pathSeparator)
    val mainClass0 = mainClass().getOrElse(sys.error("No main class"))
    val graalVmHome = Option(System.getenv("GRAALVM_HOME")).getOrElse {
      import sys.process._
      Seq(cs(), "java-home", "--jvm", deps.graalVmJvmId).!!.trim
    }
    val outputDir = T.ctx().dest / "config"
    // format: off
    val command = Seq(
      s"$graalVmHome/bin/java",
      s"-agentlib:native-image-agent=config-output-dir=$outputDir",
      "-cp", cp,
      mainClass0
    ) ++ args
    // format: on
    os.proc(command.map(x => x: os.Shellable): _*).call(
      stdin = os.Inherit,
      stdout = os.Inherit
    )
    T.log.outputStream.println(s"Config generated in ${outputDir.relativeTo(os.pwd)}")
  }

  def runFromJars(args: String*) = T.command {
    val cp         = jarClassPath().map(_.path).mkString(File.pathSeparator)
    val mainClass0 = mainClass().getOrElse(sys.error("No main class"))
    val command    = Seq("java", "-cp", cp, mainClass0) ++ args
    os.proc(command.map(x => x: os.Shellable): _*).call(
      stdin = os.Inherit,
      stdout = os.Inherit
    )
  }

  def runClasspath = T {
    super.runClasspath() ++ Seq(localRepoJar())
  }

  def jarClassPath = T {
    val cp = runClasspath() ++ transitiveJars()
    cp.filter(ref => os.exists(ref.path) && !os.isDir(ref.path))
  }

  def launcher = T {
    import coursier.launcher.{
      AssemblyGenerator,
      BootstrapGenerator,
      ClassPathEntry,
      Parameters,
      Preamble
    }
    import scala.util.Properties.isWin
    val cp         = jarClassPath().map(_.path)
    val mainClass0 = mainClass().getOrElse(sys.error("No main class"))

    val dest = T.ctx().dest / (if (isWin) "launcher.bat" else "launcher")

    val preamble = Preamble()
      .withOsKind(isWin)
      .callsItself(isWin)
    val entries       = cp.map(path => ClassPathEntry.Url(path.toNIO.toUri.toASCIIString))
    val loaderContent = coursier.launcher.ClassLoaderContent(entries)
    val params = Parameters.Bootstrap(Seq(loaderContent), mainClass0)
      .withDeterministic(true)
      .withPreamble(preamble)

    BootstrapGenerator.generate(params, dest.toNIO)

    PathRef(dest)
  }

  def standaloneLauncher = T {

    val cachePath = os.Path(coursier.cache.FileCache().location, os.pwd)
    def urlOf(path: os.Path): Option[String] =
      if (path.startsWith(cachePath)) {
        val segments = path.relativeTo(cachePath).segments
        val url      = segments.head + "://" + segments.tail.mkString("/")
        Some(url)
      }
      else None

    import coursier.launcher.{
      AssemblyGenerator,
      BootstrapGenerator,
      ClassPathEntry,
      Parameters,
      Preamble
    }
    import scala.util.Properties.isWin
    val cp         = jarClassPath().map(_.path)
    val mainClass0 = mainClass().getOrElse(sys.error("No main class"))

    val dest = T.ctx().dest / (if (isWin) "launcher.bat" else "launcher")

    val preamble = Preamble()
      .withOsKind(isWin)
      .callsItself(isWin)
    val entries = cp.map { path =>
      urlOf(path) match {
        case None =>
          val content = os.read.bytes(path)
          val name    = path.last
          ClassPathEntry.Resource(name, os.mtime(path), content)
        case Some(url) => ClassPathEntry.Url(url)
      }
    }
    val loaderContent = coursier.launcher.ClassLoaderContent(entries)
    val params = Parameters.Bootstrap(Seq(loaderContent), mainClass0)
      .withDeterministic(true)
      .withPreamble(preamble)

    BootstrapGenerator.generate(params, dest.toNIO)

    PathRef(dest)
  }

}

trait HasTests extends SbtModule {
  trait Tests extends super.Tests with ScalaCliCompile {
    def ivyDeps = super.ivyDeps() ++ Agg(
      Deps.expecty,
      Deps.munit
    )
    def testFramework = "munit.Framework"
    def forkArgs      = super.forkArgs() ++ Seq("-Xmx512m", "-Xms128m")

    def repositoriesTask =
      T.task(super.repositoriesTask() ++ deps.customRepositories)
  }
}

trait PublishLocalNoFluff extends PublishModule {
  def emptyZip = T {
    import java.io._
    import java.util.zip._
    val dest = T.dest / "empty.zip"
    val baos = new ByteArrayOutputStream
    val zos  = new ZipOutputStream(baos)
    zos.finish()
    zos.close()
    os.write(dest, baos.toByteArray)
    PathRef(dest)
  }
  // adapted from https://github.com/com-lihaoyi/mill/blob/fea79f0515dda1def83500f0f49993e93338c3de/scalalib/src/PublishModule.scala#L70-L85
  // writes empty zips as source and doc JARs
  def publishLocalNoFluff(localIvyRepo: String = null): define.Command[PathRef] = T.command {

    import mill.scalalib.publish.LocalIvyPublisher
    val publisher = localIvyRepo match {
      case null => LocalIvyPublisher
      case repo =>
        new LocalIvyPublisher(os.Path(repo.replace("{VERSION}", publishVersion()), os.pwd))
    }

    publisher.publish(
      jar = jar().path,
      sourcesJar = emptyZip().path,
      docJar = emptyZip().path,
      pom = pom().path,
      ivy = ivy().path,
      artifact = artifactMetadata(),
      extras = extraPublish()
    )

    jar()
  }
}

trait LocalRepo extends Module {

  def stubsModules: Seq[PublishLocalNoFluff]
  def version: T[String]

  def publishStubs = T {
    val tasks = stubsModules.map(_.publishLocalNoFluff())
    define.Target.sequence(tasks)
  }

  def localRepo = T {
    val repoRoot = os.rel / "out" / "repo" / "{VERSION}"
    val tasks    = stubsModules.map(_.publishLocalNoFluff(repoRoot.toString))
    define.Target.sequence(tasks)
  }

  private def vcsState =
    if (isCI)
      T.persistent {
        VcsVersion.vcsState()
      }
    else
      T {
        VcsVersion.vcsState()
      }
  def localRepoZip = T {
    val repoVer   = vcsState().format()
    val ver       = version()
    val something = localRepo()
    val repoDir   = os.pwd / "out" / "repo" / ver
    val destDir   = T.dest / ver / "repo.zip"
    val dest      = destDir / "repo.zip"

    import java.io._
    import java.util.zip._
    os.makeDir.all(destDir)
    var fos: FileOutputStream = null
    var zos: ZipOutputStream  = null
    try {
      fos = new FileOutputStream(dest.toIO)
      zos = new ZipOutputStream(new BufferedOutputStream(fos))

      val versionEntry = new ZipEntry("version")
      versionEntry.setTime(0L)
      zos.putNextEntry(versionEntry)
      zos.write(repoVer.getBytes(StandardCharsets.UTF_8))
      zos.flush()

      os.walk(repoDir).filter(_ != repoDir).foreach { p =>
        val isDir = os.isDir(p)
        val name  = p.relativeTo(repoDir).toString + (if (isDir) "/" else "")
        val entry = new ZipEntry(name)
        entry.setTime(os.mtime(p))
        zos.putNextEntry(entry)
        if (!isDir) {
          zos.write(os.read.bytes(p))
          zos.flush()
        }
        zos.closeEntry()
      }
      zos.finish()
    }
    finally {
      if (zos != null) zos.close()
      if (fos != null) fos.close()
    }

    PathRef(dest)
  }

  def localRepoJar = T {
    val zip  = localRepoZip().path
    val dest = T.dest / "repo.jar"

    import java.io._
    import java.util.zip._
    var fos: FileOutputStream = null
    var zos: ZipOutputStream  = null
    try {
      fos = new FileOutputStream(dest.toIO)
      zos = new ZipOutputStream(new BufferedOutputStream(fos))

      val entry = new ZipEntry(localRepoResourcePath)
      entry.setTime(os.mtime(zip))
      zos.putNextEntry(entry)
      zos.write(os.read.bytes(zip))
      zos.flush()
      zos.closeEntry()

      zos.finish()
    }
    finally {
      if (zos != null) zos.close()
      if (fos != null) fos.close()
    }

    PathRef(dest)
  }

}

trait HasMacroAnnotations extends ScalaModule {
  def scalacOptions = T {
    val sv = scalaVersion()
    val extra =
      if (sv.startsWith("2."))
        if (sv.startsWith("2.13.")) Seq("-Ymacro-annotations")
        else Nil
      else Nil
    super.scalacOptions() ++ extra
  }
  def scalacPluginIvyDeps = T {
    val sv = scalaVersion()
    val extra =
      if (sv.startsWith("2.") && !sv.startsWith("2.13.")) Agg(Deps.macroParadise)
      else Agg.empty[Dep]
    super.scalacPluginIvyDeps() ++ extra
  }
}

private def doFormatNativeImageConf(dir: os.Path, format: Boolean): List[os.Path] = {
  val sortByName = Set("jni-config.json", "reflect-config.json")
  val files = Seq(
    "jni-config.json",
    "proxy-config.json",
    "reflect-config.json",
    "resource-config.json"
  )
  var needsFormatting = List.empty[os.Path]
  for (name <- files) {
    val file = dir / name
    if (os.isFile(file)) {
      val content = os.read(file)
      val json    = ujson.read(content)
      val updatedJson =
        if (name == "reflect-config.json")
          json.arrOpt.fold(json) { arr =>
            val values =
              arr.toVector.groupBy(_("name").str).toVector.sortBy(_._1).map(_._2).map { t =>
                val entries = t.map(_.obj).reduce(_ ++ _)
                if (entries.get("allDeclaredFields") == Some(ujson.Bool(true)))
                  entries -= "fields"
                if (entries.get("allDeclaredMethods") == Some(ujson.Bool(true)))
                  entries -= "methods"
                ujson.Obj(entries)
              }
            ujson.Arr(values: _*)
          }
        else if (sortByName(name))
          json.arrOpt.fold(json) { arr =>
            val values = arr.toVector.sortBy(_("name").str)
            ujson.Arr(values: _*)
          }
        else
          json
      val updatedContent = updatedJson.render(indent = 2)
        .linesIterator
        .filter(_.trim.nonEmpty)
        .map(_ + "\n")
        .mkString
      if (updatedContent != content) {
        needsFormatting = file :: needsFormatting
        if (format)
          os.write.over(file, updatedContent)
      }
    }
  }
  needsFormatting
}

trait FormatNativeImageConf extends JavaModule {
  def nativeImageConfDirs = T {
    resources()
      .map(_.path / "META-INF" / "native-image")
      .filter(os.exists(_))
      .flatMap { path =>
        os.walk(path)
          .filter(_.last.endsWith("-config.json"))
          .filter(os.isFile(_))
          .map(_ / os.up)
      }
      .distinct
  }
  def checkNativeImageConfFormat() = T.command {
    var needsFormatting = List.empty[os.Path]
    for (dir <- nativeImageConfDirs())
      needsFormatting = doFormatNativeImageConf(dir, format = false) ::: needsFormatting
    if (needsFormatting.nonEmpty) {
      val msg = s"Error: ${needsFormatting.length} file(s) needs formatting"
      System.err.println(msg)
      for (f <- needsFormatting)
        System.err.println(
          s"  ${if (f.startsWith(os.pwd)) f.relativeTo(os.pwd).toString else f.toString}"
        )
      System.err.println(
        """Run
          |  ./mill -i __.formatNativeImageConf
          |to format them.""".stripMargin
      )
      sys.error(msg)
    }
    ()
  }
  def formatNativeImageConf() = T.command {
    var formattedCount = 0
    for (dir <- nativeImageConfDirs())
      formattedCount += doFormatNativeImageConf(dir, format = true).length
    System.err.println(s"Formatted $formattedCount file(s).")
    ()
  }
}

trait ScalaCliCompile extends scala.cli.mill.ScalaCliCompile {
  def scalaCliVersion = BuildDeps.scalaCliVersion
  def enableScalaCli  = false

  def scalacOptions = T {
    val sv         = scalaVersion()
    val isScala213 = sv.startsWith("2.13.")
    val extraOptions =
      if (isScala213) Seq("-Xsource:3", "-Ytasty-reader")
      else Nil
    super.scalacOptions() ++ extraOptions
  }

  def mandatoryIvyDeps = T {
    super.mandatoryIvyDeps().map { dep =>
      val isScala3Lib =
        dep.dep.module.organization.value == "org.scala-lang" &&
        dep.dep.module.name.value == "scala3-library" &&
        (dep.cross match {
          case _: CrossVersion.Binary => true
          case _                      => false
        })
      if (isScala3Lib)
        dep.copy(
          dep = dep.dep.withModule(
            dep.dep.module.withName(
              coursier.ModuleName(dep.dep.module.name.value + "_3")
            )
          ),
          cross = CrossVersion.empty(dep.cross.platformed)
        )
      else dep
    }
  }
  def transitiveIvyDeps = T {
    super.transitiveIvyDeps().map { dep =>
      val isScala3Lib =
        dep.dep.module.organization.value == "org.scala-lang" &&
        dep.dep.module.name.value == "scala3-library" &&
        (dep.cross match {
          case _: CrossVersion.Binary => true
          case _                      => false
        })
      if (isScala3Lib)
        dep.copy(
          dep = dep.dep.withModule(
            dep.dep.module.withName(
              coursier.ModuleName(dep.dep.module.name.value + "_3")
            )
          ),
          cross = CrossVersion.empty(dep.cross.platformed)
        )
      else dep
    }
  }
}

trait ScalaCliScalafixModule extends ScalafixModule with ScalaCliCompile {
  def scalafixConfig = T {
    if (scalaVersion().startsWith("2.")) super.scalafixConfig()
    else Some(os.pwd / ".scalafix3.conf")
  }
  def scalafixIvyDeps = super.scalafixIvyDeps() ++ Seq(
    Deps.organizeImports
  )
  def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ {
    if (scalaVersion().startsWith("2.")) Seq(Deps.semanticDbScalac)
    else Nil
  }
  def scalacOptions = T {
    val sv       = scalaVersion()
    val isScala2 = sv.startsWith("2.")
    val sourceFiles = allSources()
      .map(_.path)
      .filter(os.exists(_))
    val sourceRoot = sourceFiles.find(_.last == "scala")
      .orElse(sourceFiles.headOption)
      .getOrElse(millSourcePath)
    val parentOptions = {
      val l = super.scalacOptions()
      if (isScala2) l.filterNot(_.startsWith("-P:semanticdb:sourceroot:"))
      else {
        val len = l.length
        val idx = l.indexWhere(_.startsWith("-sourceroot"))
        if (idx < len - 1) l.take(idx) ++ l.drop(idx + 2)
        else l
      }
    }
    val semDbOptions =
      if (isScala2) Seq(s"-P:semanticdb:sourceroot:$sourceRoot")
      else Seq(s"-sourceroot", sourceRoot.toString)
    parentOptions ++ semDbOptions
  }
}

trait ScalaCliCrossSbtModule extends CrossSbtModule with ScalaCliModule
trait ScalaCliSbtModule      extends SbtModule with ScalaCliModule

trait ScalaCliTests extends TestModule with ScalaCliModule

trait ScalaCliModule extends ScalaModule {
  def javacOptions = super.javacOptions() ++ Seq(
    "--release",
    "16"
  )
  def scalacOptions = T {
    val sv         = scalaVersion()
    val isScala213 = sv.startsWith("2.13.")
    val extraOptions =
      if (isScala213) Seq("-Xsource:3", "-Ytasty-reader")
      else Nil
    super.scalacOptions() ++ Seq("-feature", "-deprecation") ++ extraOptions
  }
}

def workspaceDirName = ".scala-build"

final case class License(licenseId: String, name: String, reference: String)
object License {
  implicit val rw: ReadWriter[License] = macroRW
}
final case class Licenses(licenses: List[License])
object Licenses {
  implicit val rw: ReadWriter[Licenses] = macroRW
}

def updateLicensesFile() = {
  val url             = "https://github.com/spdx/license-list-data/raw/master/json/licenses.json"
  var is: InputStream = null
  val b =
    try {
      is = new java.net.URL(url).openStream()
      is.readAllBytes()
    }
    finally if (is != null) is.close()
  val content = new String(b, "UTF-8")

  val licenses = read[Licenses](content).licenses

  System.err.println(s"Found ${licenses.length} licenses")

  val licensesCode = licenses
    .sortBy(_.licenseId)
    .map { license =>
      s"""    License("${license.licenseId}", "${license.name.replace(
          "\"",
          "\\\""
        )}", "${license.reference}")"""
    }
    .mkString(",\n")

  val genSource =
    s"""package scala.build.internal
       |
       |object Licenses {
       |  // format: off
       |  val list = Seq(
       |$licensesCode
       |  )
       |  // format: on
       |
       |  lazy val map = list.map(l => l.id -> l).toMap
       |}
       |""".stripMargin

  val dest =
    os.rel / "modules" / "build" / "src" / "main" / "scala" / "scala" / "build" / "internal" / "Licenses.scala"
  os.write.over(os.pwd / dest, genSource)

  System.err.println(s"Wrote $dest")
}
