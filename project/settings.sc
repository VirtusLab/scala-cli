import $file.deps, deps.Deps

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

lazy val vcvarsCandidates = Option(System.getenv("VCVARSALL")) ++ Seq(
  """C:\Program Files (x86)\Microsoft Visual Studio\2019\Enterprise\VC\Auxiliary\Build\vcvars64.bat""",
  """C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Auxiliary\Build\vcvars64.bat""",
  """C:\Program Files (x86)\Microsoft Visual Studio\2017\Enterprise\VC\Auxiliary\Build\vcvars64.bat""",
  """C:\Program Files (x86)\Microsoft Visual Studio\2017\Community\VC\Auxiliary\Build\vcvars64.bat"""
)

def vcvarsOpt: Option[os.Path] =
  vcvarsCandidates
    .iterator
    .map(os.Path(_, os.pwd))
    .filter(os.exists(_))
    .toStream
    .headOption

def generateNativeImage(
  graalVmVersion: String,
  classPath: Seq[os.Path],
  mainClass: String,
  dest: os.Path,
  includeResources: Seq[String]
): Unit = {

  val graalVmHome = Option(System.getenv("GRAALVM_HOME")).getOrElse {
    import sys.process._
    Seq(cs, "java-home", "--jvm", s"graalvm-java11:$graalVmVersion", "--jvm-index", jvmIndex).!!.trim
  }

  val ext = if (Properties.isWin) ".cmd" else ""
  val nativeImage = s"$graalVmHome/bin/native-image$ext"

  if (!os.isFile(os.Path(nativeImage))) {
    val ret = os.proc(s"$graalVmHome/bin/gu$ext", "install", "native-image").call(
      stdin = os.Inherit,
      stdout = os.Inherit,
      stderr = os.Inherit
    )
    if (ret.exitCode != 0)
      System.err.println(s"Warning: 'gu install native-image' exited with return code ${ret.exitCode}}")
    if (!os.isFile(os.Path(nativeImage)))
      System.err.println(s"Warning: $nativeImage not found, and not installed by 'gu install native-image'")
  }

  val finalCp =
    if (Properties.isWin) {
      import java.util.jar._
      val manifest = new Manifest
      val attributes = manifest.getMainAttributes
      attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0")
      attributes.put(Attributes.Name.CLASS_PATH, classPath.map(_.toIO.getAbsolutePath).mkString(" "))
      val jarFile = File.createTempFile("classpathJar", ".jar")
      val jos = new JarOutputStream(new java.io.FileOutputStream(jarFile), manifest)
      jos.close()
      jarFile.getAbsolutePath
    } else
      classPath.map(_.toIO.getAbsolutePath).mkString(File.pathSeparator)

  val extraArgs =
    if (Properties.isWin)
      Seq(
        "--no-server",
        "-J-Xmx6g",
        "--verbose"
      )
    else Nil

  val command = Seq(
    nativeImage,
    "--no-fallback"
  ) ++
  extraArgs ++
  Seq(
    "--enable-url-protocols=https",
    "--initialize-at-build-time=scala.Symbol",
    "--initialize-at-build-time=scala.Symbol$",
    "--initialize-at-build-time=scala.Function1",
    "--initialize-at-build-time=scala.Function2",
    "--initialize-at-build-time=scala.runtime.LambdaDeserialize",
    "--initialize-at-build-time=scala.runtime.EmptyMethodCache",
    "--initialize-at-build-time=scala.runtime.StructuralCallSite",
    "--initialize-at-build-time=scala.collection.immutable.VM",
    "--initialize-at-build-time=com.google.common.jimfs.SystemJimfsFileSystemProvider",
    "-H:IncludeResources=amm-dependencies.txt",
    "-H:IncludeResources=bootstrap.*.jar",
    "-H:IncludeResources=coursier/coursier.properties",
    "-H:IncludeResources=coursier/launcher/coursier.properties",
    "-H:IncludeResources=coursier/launcher/.*.bat",
    "-H:IncludeResources=org/scalajs/linker/backend/emitter/.*.sjsir"
  ) ++
  includeResources.map(r => s"-H:IncludeResources=$r") ++
  Seq(
    "--allow-incomplete-classpath",
    "--report-unsupported-elements-at-runtime",
    "-H:+ReportExceptionStackTraces",
    s"-H:Name=${dest.relativeTo(os.pwd)}",
    "-cp",
    finalCp,
    mainClass
  )

  val finalCommand =
    if (Properties.isWin)
      vcvarsOpt match {
        case None =>
          System.err.println(s"Warning: vcvarsall script not found in predefined locations:")
          for (loc <- vcvarsCandidates)
            System.err.println(s"  $loc")
          command
        case Some(vcvars) =>
          // chcp 437 sometimes needed, see https://github.com/oracle/graal/issues/2522
          val escapedCommand = command.map {
            case s if s.contains(" ") => "\"" + s + "\""
            case s => s
          }
          val script =
           s"""chcp 437
              |@call "$vcvars"
              |if %errorlevel% neq 0 exit /b %errorlevel%
              |@call ${escapedCommand.mkString(" ")}
              |""".stripMargin
          val scriptPath = os.temp(script.getBytes, prefix = "run-native-image", suffix = ".bat")
          Seq(scriptPath.toString)
      }
    else
      command

  val res = os.proc(finalCommand.map(x => x: os.Shellable): _*).call(
    stdin = os.Inherit,
    stdout = os.Inherit,
    stderr = os.Inherit
  )
  if (res.exitCode != 0)
    sys.error(s"native-image command exited with ${res.exitCode}")
}

def platformExtension: String =
  if (Properties.isWin) ".exe"
  else ""

def platformSuffix: String = {
  val arch = sys.props("os.arch").toLowerCase(java.util.Locale.ROOT) match {
    case "amd64" => "x86_64"
    case other => other
  }
  val os =
    if (Properties.isWin) "pc-win32"
    else if (Properties.isLinux) "pc-linux"
    else if (Properties.isMac) "apple-darwin"
    else sys.error(s"Unrecognized OS: ${sys.props("os.name")}")
  s"$arch-$os"
}


def localRepoResourcePath = "local-repo.zip"

trait CliLaunchers extends SbtModule {

  def localRepoJar: T[PathRef]
  def graalVmVersion: String

  def transitiveJars: T[Agg[PathRef]] = {

    def allModuleDeps(todo: List[JavaModule]): List[JavaModule] =
      todo match {
        case Nil => Nil
        case h :: t =>
          h :: allModuleDeps(h.moduleDeps.toList ::: t)
      }

    T{
      mill.define.Target.traverse(allModuleDeps(this :: Nil).distinct)(m =>
        T.task{m.jar()}
      )()
    }
  }

  def nativeImage = T{
    val cp = runClasspath().map(_.path)
    val mainClass0 = mainClass().getOrElse(sys.error("Don't know what main class to use"))
    val dest = T.ctx().dest / "scala"
    val actualDest = T.ctx().dest / s"scala$platformExtension"

    val localRepoJar0 = localRepoJar().path

    generateNativeImage(graalVmVersion, cp :+ localRepoJar0, mainClass0, dest, Seq(localRepoResourcePath))

    PathRef(actualDest)
  }

  def runWithAssistedConfig(args: String*) = T.command {
    val cp = jarClassPath().map(_.path).mkString(File.pathSeparator)
    val mainClass0 = mainClass().getOrElse(sys.error("No main class"))
    val graalVmHome = Option(System.getenv("GRAALVM_HOME")).getOrElse {
      import sys.process._
      Seq(cs, "java-home", "--jvm", s"graalvm-java11:$graalVmVersion", "--jvm-index", jvmIndex).!!.trim
    }
    val outputDir = T.ctx().dest / "config"
    val command = Seq(
      s"$graalVmHome/bin/java",
      s"-agentlib:native-image-agent=config-output-dir=$outputDir",
      "-cp", cp,
      mainClass0
    ) ++ args
    os.proc(command.map(x => x: os.Shellable): _*).call(
      stdin = os.Inherit,
      stdout = os.Inherit,
      stderr = os.Inherit
    )
    T.log.outputStream.println(s"Config generated in ${outputDir.relativeTo(os.pwd)}")
  }

  def runFromJars(args: String*) = T.command {
    val cp = jarClassPath().map(_.path).mkString(File.pathSeparator)
    val mainClass0 = mainClass().getOrElse(sys.error("No main class"))
    val command = Seq("java", "-cp", cp, mainClass0) ++ args
    os.proc(command.map(x => x: os.Shellable): _*).call(
      stdin = os.Inherit,
      stdout = os.Inherit,
      stderr = os.Inherit
    )
  }

  def runClasspath = T{
    super.runClasspath() ++ Seq(localRepoJar())
  }

  def jarClassPath = T{
    val cp = runClasspath() ++ transitiveJars()
    cp.filter(ref => os.exists(ref.path) && !os.isDir(ref.path))
  }

  def launcher = T{
    import coursier.launcher.{AssemblyGenerator, BootstrapGenerator, ClassPathEntry, Parameters, Preamble}
    import scala.util.Properties.isWin
    val cp = jarClassPath().map(_.path)
    val mainClass0 = mainClass().getOrElse(sys.error("No main class"))

    val dest = T.ctx().dest / (if (isWin) "launcher.bat" else "launcher")

    val preamble = Preamble()
      .withOsKind(isWin)
      .callsItself(isWin)
    val entries = cp.map(path => ClassPathEntry.Url(path.toNIO.toUri.toASCIIString))
    val loaderContent = coursier.launcher.ClassLoaderContent(entries)
    val params = Parameters.Bootstrap(Seq(loaderContent), mainClass0)
      .withDeterministic(true)
      .withPreamble(preamble)

    BootstrapGenerator.generate(params, dest.toNIO)

    PathRef(dest)
  }

  def standaloneLauncher = T{

    val cachePath = os.Path(coursier.cache.FileCache().location, os.pwd)
    def urlOf(path: os.Path): Option[String] = {
      if (path.startsWith(cachePath)) {
        val segments = path.relativeTo(cachePath).segments
        val url = segments.head + "://" + segments.tail.mkString("/")
        Some(url)
      }
      else None
    }

    import coursier.launcher.{AssemblyGenerator, BootstrapGenerator, ClassPathEntry, Parameters, Preamble}
    import scala.util.Properties.isWin
    val cp = jarClassPath().map(_.path)
    val mainClass0 = mainClass().getOrElse(sys.error("No main class"))

    val dest = T.ctx().dest / (if (isWin) "launcher.bat" else "launcher")

    val preamble = Preamble()
      .withOsKind(isWin)
      .callsItself(isWin)
    val entries = cp.map { path =>
      urlOf(path) match {
        case None =>
          val content = os.read.bytes(path)
          val name = path.last
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
    def forkArgs = super.forkArgs() ++ Seq("-Xmx512m", "-Xms128m")
  }
}

trait PublishLocalNoFluff extends PublishModule {
  def emptyZip = T{
    import java.io._
    import java.util.zip._
    val dest = T.dest / "empty.zip"
    val baos = new ByteArrayOutputStream
    val zos = new ZipOutputStream(baos)
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
      case repo => new LocalIvyPublisher(os.Path(repo.replace("{VERSION}", publishVersion()), os.pwd))
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

  def publishStubs = T{
    val tasks = stubsModules.map(_.publishLocalNoFluff())
    define.Task.sequence(tasks)
  }

  def localRepo = T{
    val repoRoot = os.rel / "out" / "repo" / "{VERSION}"
    val tasks = stubsModules.map(_.publishLocalNoFluff(repoRoot.toString))
    define.Task.sequence(tasks)
  }

  def localRepoZip = T{
    val ver = version()
    val something = localRepo()
    val repoDir = os.pwd / "out" / "repo" / ver
    val destDir = T.dest / ver / "repo.zip"
    val dest = destDir / "repo.zip"

    import java.io._
    import java.util.zip._
    os.makeDir.all(destDir)
    var fos: FileOutputStream = null
    var zos: ZipOutputStream = null
    try {
      fos = new FileOutputStream(dest.toIO)
      zos = new ZipOutputStream(new BufferedOutputStream(fos))
      os.walk(repoDir).filter(_ != repoDir).foreach { p =>
        val isDir = os.isDir(p)
        val name = p.relativeTo(repoDir).toString + (if (isDir) "/" else "")
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
    } finally {
      if (zos != null) zos.close()
      if (fos != null) fos.close()
    }

    PathRef(dest)
  }

  def localRepoJar = T{
    val zip = localRepoZip().path
    val dest = T.dest / "repo.jar"

    import java.io._
    import java.util.zip._
    var fos: FileOutputStream = null
    var zos: ZipOutputStream = null
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
    } finally {
      if (zos != null) zos.close()
      if (fos != null) fos.close()
    }

    PathRef(dest)
  }

}
