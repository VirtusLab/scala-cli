import $ivy.`io.github.alexarchambault.mill::mill-native-image_mill0.9:0.1.9`
import $file.deps, deps.{Deps, Docker}

import io.github.alexarchambault.millnativeimage.NativeImage
import java.io.File
import mill._, scalalib._
import scala.util.Properties

lazy val cs: String =
  if (Properties.isWin) {
    val pathExt = Option(System.getenv("PATHEXT"))
      .toSeq
      .flatMap(_.split(File.pathSeparator).toSeq)
    val path = Option(System.getenv("PATH"))
      .toSeq
      .flatMap(_.split(File.pathSeparator))
      .map(new File(_))

    def candidates =
      for {
        dir <- path.iterator
        ext <- pathExt.iterator
      } yield new File(dir, s"cs$ext")

    candidates
      .filter(_.canExecute)
      .toStream
      .headOption
      .map(_.getAbsolutePath)
      .getOrElse {
        System.err.println("Warning: could not find cs in PATH.")
        "cs"
      }
  }
  else
    "cs"

// should be the default index in the upcoming coursier release (> 2.0.16)
def jvmIndex = "https://github.com/coursier/jvm-index/raw/master/index.json"

def platformExtension: String =
  if (Properties.isWin) ".exe"
  else ""

def platformExecutableJarExtension: String =
  if (Properties.isWin) ".bat"
  else ""

def platformSuffix: String = {
  val arch = sys.props("os.arch").toLowerCase(java.util.Locale.ROOT) match {
    case "amd64" => "x86_64"
    case other   => other
  }
  val os =
    if (Properties.isWin) "pc-win32"
    else if (Properties.isLinux) "pc-linux"
    else if (Properties.isMac) "apple-darwin"
    else sys.error(s"Unrecognized OS: ${sys.props("os.name")}")
  s"$arch-$os"
}

def localRepoResourcePath = "local-repo.zip"

trait CliLaunchers extends SbtModule { self =>

  trait CliNativeImage extends NativeImage {
    def nativeImageCsCommand    = Seq(cs)
    def nativeImagePersist      = System.getenv("CI") != null
    def nativeImageGraalVmJvmId = s"graalvm-java11:${deps.graalVmVersion}"
    def nativeImageOptions = Seq(
      s"-H:IncludeResources=$localRepoResourcePath"
    )
    def nativeImageName      = "scala-cli"
    def nativeImageClassPath = self.nativeImageClassPath()
    def nativeImageMainClass = self.nativeImageMainClass()
  }

  object `base-image` extends CliNativeImage

  object `linux-docker-image` extends CliNativeImage {
    def nativeImageDockerParams = Some(
      NativeImage.DockerParams(
        imageName = "ubuntu:18.04",
        prepareCommand = "apt-get update -q -y && apt-get install -q -y build-essential libz-dev",
        csUrl =
          s"https://github.com/coursier/coursier/releases/download/v${deps.csDockerVersion}/cs-x86_64-pc-linux",
        extraNativeImageArgs = Nil
      )
    )
  }

  object `static-image` extends CliNativeImage {
    def nativeImageDockerParams = Some(
      NativeImage.linuxStaticParams(
        Docker.muslBuilder,
        s"https://github.com/coursier/coursier/releases/download/v${deps.csDockerVersion}/cs-x86_64-pc-linux"
      )
    )
  }

  object `mostly-static-image` extends CliNativeImage {
    def nativeImageDockerParams = Some(
      NativeImage.linuxMostlyStaticParams(
        "ubuntu:18.04", // TODO Pin that
        s"https://github.com/coursier/coursier/releases/download/v${deps.csDockerVersion}/cs-x86_64-pc-linux"
      )
    )
  }

  def localRepoJar: T[PathRef]
  def graalVmVersion: String

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
        T.task { m.jar() }
      )()
    }
  }

  def nativeImageClassPath = T {
    val localRepoJar0 = localRepoJar()
    runClasspath() :+ localRepoJar0 // isn't localRepoJar already there?
  }

  def nativeImage =
    if (Properties.isLinux)
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
      // format: off
      Seq(
        cs, "java-home",
        "--jvm", s"graalvm-java11:$graalVmVersion",
        "--jvm-index", jvmIndex
      ).!!.trim
      // format: on
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
    def urlOf(path: os.Path): Option[String] = {
      if (path.startsWith(cachePath)) {
        val segments = path.relativeTo(cachePath).segments
        val url      = segments.head + "://" + segments.tail.mkString("/")
        Some(url)
      }
      else None
    }

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
  trait Tests extends super.Tests {
    def ivyDeps = super.ivyDeps() ++ Agg(
      Deps.expecty,
      Deps.munit
    )
    def testFramework = "munit.Framework"
    def forkArgs      = super.forkArgs() ++ Seq("-Xmx512m", "-Xms128m")
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
    define.Task.sequence(tasks)
  }

  def localRepo = T {
    val repoRoot = os.rel / "out" / "repo" / "{VERSION}"
    val tasks    = stubsModules.map(_.publishLocalNoFluff(repoRoot.toString))
    define.Task.sequence(tasks)
  }

  def localRepoZip = T {
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
      if (sv.startsWith("2.") && !sv.startsWith("2.13.")) Nil
      else Seq("-Ymacro-annotations")
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
      .map(_.path)
      .filter(os.exists(_))
      .flatMap { path =>
        os.walk(path / "META-INF" / "native-image")
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
      System.err.println(s"Error: ${needsFormatting.length} file(s) needs formatting:")
      for (f <- needsFormatting)
        System.err.println(
          s"  ${if (f.startsWith(os.pwd)) f.relativeTo(os.pwd).toString else f.toString}"
        )
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
