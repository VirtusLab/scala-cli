package scala.cli.packaging

import java.io.File

import scala.annotation.tailrec
import scala.build.internal.{ManifestJar, Runner}
import scala.build.{Build, Logger, Positioned}
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

  private def vcVersions = Seq("2022", "2019", "2017")
  private def vcEditions = Seq("Enterprise", "Community", "BuildTools")
  lazy val vcvarsCandidates = Option(System.getenv("VCVARSALL")) ++ {
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
    vcvarsCandidates
      .iterator
      .map(os.Path(_, os.pwd))
      .filter(os.exists(_))
      .take(1)
      .toList
      .headOption

  private def runFromVcvarsBat(
    command: Seq[String],
    vcvars: os.Path,
    workingDir: os.Path,
    logger: Logger
  ): Int = {
    logger.debug(s"Using vcvars script $vcvars")
    val escapedCommand = command.map {
      case s if s.contains(" ") => "\"" + s + "\""
      case s                    => s
    }
    // chcp 437 sometimes needed, see https://github.com/oracle/graal/issues/2522
    val script =
      s"""chcp 437
         |@call "$vcvars"
         |if %errorlevel% neq 0 exit /b %errorlevel%
         |@call ${escapedCommand.mkString(" ")}
         |""".stripMargin
    logger.debug(s"Native image script: '$script'")
    val scriptPath = workingDir / "run-native-image.bat"
    logger.debug(s"Writing native image script at $scriptPath")
    os.write.over(scriptPath, script.getBytes, createFolders = true)

    val finalCommand = Seq("cmd", "/c", scriptPath.toString)
    logger.debug(s"Running $finalCommand")
    val res = os.proc(finalCommand).call(
      cwd = os.pwd,
      check = false,
      stdin = os.Inherit,
      stdout = os.Inherit
    )
    logger.debug(s"Command $finalCommand exited with exit code ${res.exitCode}")

    res.exitCode
  }

  private def availableDriveLetter(): Char = {

    @tailrec
    def helper(from: Char): Char =
      if (from > 'Z') sys.error("Cannot find free drive letter")
      else {
        val p = os.Path(s"$from:" + "\\")
        if (os.exists(p)) helper((from + 1).toChar)
        else from
      }

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
      logger.debug(s"Aliasing $from to $drivePath")
      val setupCommand  = s"""subst $driveLetter: "$from""""
      val disableScript = s"""subst $driveLetter: /d"""

      os.proc("cmd", "/c", setupCommand).call(stdin = os.Inherit, stdout = os.Inherit)
      try f(newHome)
      finally {
        val res = os.proc("cmd", "/c", disableScript).call(
          stdin = os.Inherit,
          stdout = os.Inherit,
          check = false
        )
        if (res.exitCode == 0)
          logger.debug(s"Unaliased $drivePath")
        else if (os.exists(drivePath)) {
          // ignore errors?
          logger.debug(s"Unaliasing attempt exited with exit code ${res.exitCode}")
          throw new os.SubprocessException(res)
        }
        else
          logger.debug(
            s"Failed to unalias $drivePath which seems not to exist anymore, ignoring it"
          )
      }
    }
    else
      f(currentHome)

  def buildNativeImage(
    build: Build.Successful,
    mainClass: String,
    dest: os.Path,
    nativeImageWorkDir: os.Path,
    extraOptions: Seq[String],
    logger: Logger
  ): Unit = {

    os.makeDir.all(nativeImageWorkDir)

    val jvmId = build.options.notForBloopOptions.packageOptions.nativeImageOptions.jvmId
    val options = build.options.copy(
      javaOptions = build.options.javaOptions.copy(
        jvmIdOpt = Some(Positioned.none(jvmId))
      )
    )

    val javaHome = options.javaHome().value
    val nativeImageArgs =
      options.notForBloopOptions.packageOptions.nativeImageOptions.graalvmArgs.map(_.value)

    val cacheData = CachedBinary.getCacheData(
      build,
      s"--java-home=${javaHome.javaHome.toString}" :: "--" :: extraOptions.toList ++ nativeImageArgs,
      dest,
      nativeImageWorkDir
    )

    if (cacheData.changed)
      Library.withLibraryJar(build, dest.last.stripSuffix(".jar")) { mainJar =>

        val originalClassPath = mainJar +: build.dependencyClassPath
        ManifestJar.maybeWithManifestClassPath(
          createManifest = Properties.isWin,
          classPath = originalClassPath,
          // seems native-image doesn't correctly parse paths in manifests - this is especially a problem on Windows
          wrongSimplePathsInManifest = true
        ) { processedClassPath =>
          val needsProcessing = build.scalaParams.exists(_.scalaVersion.startsWith("3."))
          val (classPath, toClean, scala3extraOptions) =
            if (needsProcessing) {
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
            else
              (processedClassPath, Seq[os.Path](), Seq[String]())

          try {
            val args = extraOptions ++ scala3extraOptions ++ Seq(
              s"-H:Path=${dest / os.up}",
              s"-H:Name=${dest.last.stripSuffix(".exe")}", // FIXME Case-insensitive strip suffix?
              "-cp",
              classPath.map(_.toString).mkString(File.pathSeparator),
              mainClass
            ) ++ nativeImageArgs

            maybeWithShorterGraalvmHome(javaHome.javaHome, logger) { graalVMHome =>

              val nativeImageCommand = ensureHasNativeImageCommand(graalVMHome, logger)
              val command            = nativeImageCommand.toString +: args

              val exitCode =
                if (Properties.isWin)
                  vcvarsOpt match {
                    case Some(vcvars) =>
                      runFromVcvarsBat(command, vcvars, nativeImageWorkDir, logger)
                    case None =>
                      Runner.run(command, logger).waitFor()
                  }
                else
                  Runner.run(command, logger).waitFor()
              if (exitCode == 0) {
                val actualDest =
                  if (Properties.isWin)
                    if (dest.last.endsWith(".exe")) dest
                    else dest / os.up / s"${dest.last}.exe"
                  else
                    dest
                CachedBinary.updateProjectAndOutputSha(
                  actualDest,
                  nativeImageWorkDir,
                  cacheData.projectSha
                )
              }
              else
                throw new GraalVMNativeImageError
            }
          }
          finally util.Try(toClean.foreach(os.remove.all))
        }
      }
    else
      logger.message("Found cached native image binary.")
  }
}
