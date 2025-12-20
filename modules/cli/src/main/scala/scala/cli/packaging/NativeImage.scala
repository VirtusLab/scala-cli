package scala.cli.packaging

import java.io.File

import scala.annotation.tailrec
import scala.build.internal.{ManifestJar, Runner}
import scala.build.internals.ConsoleUtils.ScalaCliConsole.warnPrefix
import scala.build.internals.{EnvVar, MsvcEnvironment, WindowsProcessLauncher}
import scala.build.{Build, Logger, Positioned, coursierVersion}
import scala.cli.errors.GraalVMNativeImageError
import scala.cli.graal.{BytecodeProcessor, TempCache}
import scala.cli.internal.CachedBinary
import scala.util.Properties

object NativeImage {

  private def ensureHasNativeImageCommand(
    graalVMHome: os.Path,
    logger: Logger
  ): os.Path = {

    val ext         = if (Properties.isWin) ".cmd" else ""
    val nativeImage = graalVMHome / "bin" / s"native-image$ext"

    if (os.exists(nativeImage))
      logger.debug(s"$nativeImage found")
    else {
      val proc = os.proc(graalVMHome / "bin" / s"gu$ext", "install", "native-image")
      logger.debug(s"$nativeImage not found, running ${proc.command.flatMap(_.value)}")
      proc.call(stdin = os.Inherit, stdout = os.Inherit)
      if (!os.exists(nativeImage))
        logger.message(
          s"Seems gu install command didn't install $nativeImage, trying to run it anyway"
        )
    }

    nativeImage
  }

  private def vcVersions                              = Seq("2022", "2019", "2017")
  private def vcEditions                              = Seq("Enterprise", "Community", "BuildTools")
  private lazy val vcVarsCandidates: Iterable[String] =
    EnvVar.Misc.vcVarsAll.valueOpt ++ {
      for {
        isX86   <- Seq(false, true)
        version <- vcVersions
        edition <- vcEditions
      } yield {
        val programFiles = if (isX86) "Program Files (x86)" else "Program Files"
        """C:\""" + programFiles + """\Microsoft Visual Studio\""" + version + "\\" + edition + """\VC\Auxiliary\Build\vcvars64.bat"""
      }
    }

  private def vcvarsOpt: Option[os.Path] =
    vcVarsCandidates
      .iterator
      .map(os.Path(_, os.pwd))
      .filter(os.exists(_))
      .take(1)
      .toList
      .headOption

  private def runNativeImage(
    command: Seq[String],
    vcvars: os.Path,
    workingDir: os.Path,
    logger: Logger
  ): Int = {
    logger.debug(s"Using vcvars script $vcvars")

    val msvcEnv = MsvcEnvironment.captureVcvarsEnv(Seq("x64"))
    val baseEnv = sys.env

    val mergedPath =
      msvcEnv.getOrElse("PATH", "") + ";" + baseEnv.getOrElse("PATH", "")

    val mergedEnv =
      baseEnv ++ msvcEnv +
        ("PATH"                                 -> mergedPath) +
        ("GRAALVM_ARGUMENT_VECTOR_PROGRAM_NAME" -> "native-image")

    logger.debug(s"Launching native-image.exe with args: $command")

    // GraalVM code page 437 requirement, run, restore CP
    val exitCode =
      WindowsProcessLauncher.runInNewGroup(
        command = command,
        cwd = workingDir,
        env = mergedEnv
      )
    exitCode
  }

  private lazy val mountedDrives: String = {
    val str         = "HKEY_LOCAL_MACHINE/SYSTEM/MountedDevices".replace('/', '\\')
    val queryDrives = s"reg query $str"
    val lines       = os.proc("cmd", "/c", queryDrives).call().out.lines()
    val dosDevices  = lines.filter { s =>
      s.contains("DosDevices")
    }.map { s =>
      s.replaceAll(".DosDevices.", "").replaceAll(":.*", "")
    }
    dosDevices.mkString
  }

  private def availableDriveLetter(): Char = {
    // if a drive letter has already been mapped by SUBST, it isn't free
    val substDrives: Set[Char] =
      os.proc("cmd", "/c", "subst").call().out.text()
        .linesIterator
        .flatMap { line =>
          // lines look like: "I:\: => C:\path"
          if (line.length >= 2 && line(1) == ':') Some(line(0))
          else None
        }
        .toSet

    @tailrec
    def helper(from: Char): Char =
      if (from > 'Z') sys.error("Cannot find free drive letter")
      else if (mountedDrives.contains(from) || substDrives.contains(from))
        helper((from + 1).toChar)
      else from

    helper('D')
  }

  /** Alias currentHome to the root of a drive, so that its files can be accessed with shorter paths
    * (hopefully not going above the ~260 char limit of some Windows apps, such as cl.exe).
    *
    * Couldn't manage to make
    * https://docs.microsoft.com/en-us/windows/win32/fileio/maximum-file-path-limitation?tabs=powershell#enable-long-paths-in-windows-10-version-1607-and-later
    * work, so I went down the path here.
    */
  private def maybeWithShorterGraalvmHome[T](
    currentHome: os.Path,
    logger: Logger
  )(
    f: os.Path => T
  ): T =
    // not sure about the 180 limit, we might need to lower it
    if (Properties.isWin && currentHome.toString.length >= 180) {
      val driveLetter = availableDriveLetter()
      // aliasing the parent dir, as it seems GraalVM native-image (as of 22.0.0)
      // isn't fine with being put at the root of a drive - it tries to look for
      // things like 'D:lib' (missing '\') at some point.
      val from      = currentHome / os.up
      val drivePath = os.Path(s"$driveLetter:" + "\\")
      val newHome   = drivePath / currentHome.last

      val savedCodepage: String = getCodePage(logger) // before visual studio sets code page to 437

      val cleanupLock                  = new Object()
      var shutdownHook: Option[Thread] = None

      def atexitCleanup(): Unit = {
        cleanupLock.synchronized {
          shutdownHook.foreach { hook =>
            shutdownHook = None
            try
              Runtime.getRuntime.removeShutdownHook(hook)
            catch {
              case _: IllegalStateException => // Already shutting down, that's fine
            }

            try {
              setCodePage(savedCodepage)
              val exit = unaliasDriveLetter(driveLetter)
              if (exit == 0)
                logger.debug(s"Unaliased $drivePath")
              else if (os.exists(drivePath))
                logger.error(s"Unaliasing attempt exited with exit code $exit")
              else
                logger.debug(s"Failed to unalias $drivePath (not aliased, ignoring it)")
            }
            catch {
              case e: Throwable =>
                logger.error(s"Cleanup failed: ${e.getMessage}")
            }
          }
        }
      }

      // Create and register cleanup hook
      shutdownHook = Some(new Thread(() => atexitCleanup()))
      Runtime.getRuntime.addShutdownHook(shutdownHook.get)

      logger.debug(s"Aliasing $from to $drivePath")
      aliasDriveLetter(driveLetter, from.toString)
      try
        f(newHome)
      finally
        atexitCleanup()
    }
    else
      f(currentHome)

  def setCodePage(cp: String): Int =
    execWindowsCmd("cmd", "/c", s"chcp $cp")._1

  def getCodePage: String = {
    val out = execWindowsCmd("cmd", "/c", "chcp")._2
    out.split(":").lastOption.map(_.trim).getOrElse("") // Extract the number
  }

  def aliasDriveLetter(driveLetter: Char, from: String): Unit =
    execWindowsCmd("cmd", "/c", s"subst $driveLetter: \"$from\"")

  def unaliasDriveLetter(driveLetter: Char): Int =
    execWindowsCmd("cmd", "/c", s"subst $driveLetter: /d")._1

  private def execWindowsCmd(cmd: String*): (Int, String) =
    val pb = new ProcessBuilder(cmd*)
    pb.redirectInput(ProcessBuilder.Redirect.INHERIT)
    pb.redirectError(ProcessBuilder.Redirect.INHERIT)
    pb.redirectOutput(ProcessBuilder.Redirect.PIPE)
    val p = pb.start()
    // read stdout fully
    val output   = scala.io.Source.fromInputStream(p.getInputStream, "UTF-8").mkString
    val exitCode = p.waitFor()
    (exitCode, output)

  private def getCodePage(logger: Logger): String =
    try {
      val out = os.proc("cmd", "/c", "chcp").call().out.text().trim
      out.split(":").lastOption.map(_.trim).getOrElse("") // Extract the number
    }
    catch {
      case e: Exception =>
        logger.debug(s"unable to get initial code page: ${e.getMessage}")
        ""
    }

  def buildNativeImage(
    builds: Seq[Build.Successful],
    mainClass: String,
    dest: os.Path,
    nativeImageWorkDir: os.Path,
    extraOptions: Seq[String],
    logger: Logger
  ): Unit = {

    os.makeDir.all(nativeImageWorkDir)

    val jvmId   = builds.head.options.notForBloopOptions.packageOptions.nativeImageOptions.jvmId
    val options = builds.head.options.copy(
      javaOptions = builds.head.options.javaOptions.copy(
        jvmIdOpt = Some(Positioned.none(jvmId))
      )
    )

    val javaHome        = options.javaHome().value
    val nativeImageArgs =
      options.notForBloopOptions.packageOptions.nativeImageOptions.graalvmArgs.map(_.value)

    val cacheData = CachedBinary.getCacheData(
      builds,
      s"--java-home=${javaHome.javaHome.toString}" :: "--" :: extraOptions.toList ++ nativeImageArgs,
      dest,
      nativeImageWorkDir
    )

    if cacheData.changed then {
      val mainJar           = Library.libraryJar(builds)
      val originalClassPath = mainJar +: builds.flatMap(_.dependencyClassPath).distinct

      ManifestJar.maybeWithManifestClassPath(
        createManifest = Properties.isWin,
        classPath = originalClassPath,
        // seems native-image doesn't correctly parse paths in manifests - this is especially a problem on Windows
        wrongSimplePathsInManifest = true
      ) { processedClassPath =>
        val needsProcessing =
          builds.head.scalaParams.map(_.scalaVersion.coursierVersion)
            .exists(sv => (sv >= "3.0.0".coursierVersion) && (sv < "3.3.0".coursierVersion))
        if needsProcessing then
          logger.message(
            s"""$warnPrefix building native images with Scala 3 older than 3.3.0 is deprecated.
               |$warnPrefix support will be dropped in a future Scala CLI version.
               |$warnPrefix it is advised to upgrade to a more recent Scala version.""".stripMargin
          )
        val (classPath, toClean, scala3extraOptions) =
          if needsProcessing then {
            val cpString         = processedClassPath.mkString(File.pathSeparator)
            val processed        = BytecodeProcessor.processClassPath(cpString, TempCache).toSeq
            val nativeConfigFile = os.temp(suffix = ".json")
            os.write.over(
              nativeConfigFile,
              """[
                |  {
                |    "name": "sun.misc.Unsafe",
                |    "allDeclaredConstructors": true,
                |    "allPublicConstructors": true,
                |    "allDeclaredMethods": true,
                |    "allDeclaredFields": true
                |  }
                |]
                |""".stripMargin
            )
            val cp      = processed.map(_.path)
            val options = Seq(s"-H:ReflectionConfigurationFiles=$nativeConfigFile")

            (cp, nativeConfigFile +: BytecodeProcessor.toClean(processed), options)
          }
          else (processedClassPath, Seq[os.Path](), Seq[String]())

        def stripSuffixIgnoreCase(s: String, suffix: String): String =
          if (s.toLowerCase.endsWith(suffix.toLowerCase))
            s.substring(0, s.length - suffix.length)
          else
            s

        try {
          val args = extraOptions ++ scala3extraOptions ++ Seq(
            s"-H:Path=${dest / os.up}",
            s"-H:Name=${stripSuffixIgnoreCase(dest.last, ".exe")}", // Case-insensitive strip suffix
            "-cp",
            classPath.map(_.toString).mkString(File.pathSeparator),
            mainClass
          ) ++ nativeImageArgs

          maybeWithShorterGraalvmHome(javaHome.javaHome, logger) { graalVMHome =>

            val nativeImageCommand = ensureHasNativeImageCommand(graalVMHome, logger)
            val command            = nativeImageCommand.toString +: args

            val exitCode =
              if Properties.isWin then
                vcvarsOpt match {
                  case Some(vcvars) => runNativeImage(command, vcvars, nativeImageWorkDir, logger)
                  case None         => Runner.run(command, logger).waitFor()
                }
              else Runner.run(command, logger).waitFor()
            if exitCode == 0 then {
              val actualDest =
                if Properties.isWin then
                  if dest.last.endsWith(".exe") then dest
                  else dest / os.up / s"${dest.last}.exe"
                else dest
              CachedBinary.updateProjectAndOutputSha(
                actualDest,
                nativeImageWorkDir,
                cacheData.projectSha
              )
            }
            else throw new GraalVMNativeImageError
          }
        }
        finally util.Try(toClean.foreach(os.remove.all))
      }
    }
    else
      logger.message("Found cached native image binary.")
  }
}
