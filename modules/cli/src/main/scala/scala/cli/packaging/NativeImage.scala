package scala.cli.packaging

import java.io.File

import scala.build.internal.{ManifestJar, Runner}
import scala.build.internals.ConsoleUtils.ScalaCliConsole.warnPrefix
import scala.build.internals.MsvcEnvironment
import scala.build.internals.MsvcEnvironment.*
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
    if (Properties.isWin && currentHome.toString.length >= 180) {
      val (driveLetter, newHome) = getShortenedPath(currentHome, logger)
      val savedCodepage: String  = getCodePage(logger)
      val result                 =
        try
          f(newHome)
        finally {
          unaliasDriveLetter(driveLetter)
          setCodePage(savedCodepage)
        }
      result
    }
    else
      f(currentHome)

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
                MsvcEnvironment.msvcNativeImageProcess(
                  command = command,
                  workingDir = nativeImageWorkDir,
                  logger = logger
                )
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
