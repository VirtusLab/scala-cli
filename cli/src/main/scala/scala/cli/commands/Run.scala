package scala.cli.commands

import caseapp.core.app.CaseApp
import caseapp.core.RemainingArgs
import scala.cli.{Build, Inputs, Runner}
import scala.scalanative.{build => sn}

import java.nio.file.{Files, Path}

object Run extends CaseApp[RunOptions] {
  def run(options: RunOptions, args: RemainingArgs): Unit = {

    val inputs = Inputs(args.all) match {
      case Left(message) =>
        System.err.println(message)
        sys.exit(1)
      case Right(i) => i
    }

    if (options.shared.watch) {
      val watcher = Build.watch(inputs, options.shared.buildOptions, options.shared.logger, postAction = () => WatchUtil.printWatchMessage()) { build =>
        runOnce(options, build, allowExecve = false, exitOnError = false)
      }
      try WatchUtil.waitForCtrlC()
      finally watcher.dispose()
    } else {
      val build = Build.build(inputs, options.shared.buildOptions, options.shared.logger)
      runOnce(options, build, allowExecve = true, exitOnError = true)
    }
  }

  private def runOnce(
    options: RunOptions,
    build: Build,
    allowExecve: Boolean,
    exitOnError: Boolean
  ): Unit = {

    val mainClassOpt = {
      val foundMainClasses = build.foundMainClasses()
      val defaultMainClassOpt = build.sources.mainClass
        .filter(name => foundMainClasses.contains(name))
      def foundMainClassOpt =
        if (foundMainClasses.length == 1) foundMainClasses.headOption
        else {
          System.err.println("Found several main classes:")
          for (name <- foundMainClasses)
            System.err.println(s"  $name")
          System.err.println("Please specify which one to use with --main-class")
          None
        }
      def fromOptions = options.mainClass.filter(_.nonEmpty) // trim it too?

      defaultMainClassOpt
        .orElse(fromOptions)
        .orElse(foundMainClassOpt)
    }

    for (mainClass <- mainClassOpt) {
      val retCode =
        if (options.shared.js)
          withLinkedJs(build, mainClass) { js =>
            Runner.runJs(
              js.toFile,
              options.shared.logger,
              allowExecve = allowExecve
            )
          }
        else if (options.shared.native)
          withNativeLauncher(
            build,
            mainClass,
            options.shared.scalaNativeOptionsIKnowWhatImDoing,
            options.shared.nativeWorkDir,
            options.shared.scalaNativeLogger
          ) { launcher =>
            Runner.runNative(
              launcher.toFile,
              options.shared.logger,
              allowExecve = allowExecve
            )
          }
        else
          Runner.run(
            build.artifacts.javaHome,
            build.fullClassPath.map(_.toFile),
            mainClass,
            options.shared.logger,
            allowExecve = allowExecve
          )
      if (retCode != 0) {
        if (exitOnError)
          sys.exit(retCode)
        else {
          val red = Console.RED
          val lightRed = "\u001b[91m"
          val reset = Console.RESET
          System.err.println(s"${red}Program exited with return code $lightRed$retCode$red.$reset")
        }
      }
    }
  }


  private def withLinkedJs[T](build: Build, mainClass: String)(f: Path => T): T = {
    val dest = Files.createTempFile("main", ".js")
    try {
      Package.linkJs(build, mainClass, dest)
      f(dest)
    } finally {
      Files.deleteIfExists(dest)
    }
  }

  private def withNativeLauncher[T](
    build: Build,
    mainClass: String,
    options: Build.ScalaNativeOptions,
    workDir: os.Path,
    logger: sn.Logger
  )(f: Path => T): T = {
    val dest = Files.createTempFile("main", ".js")
    try {
      Package.buildNative(build, mainClass, dest, options, workDir, logger)
      f(dest)
    } finally {
      Files.deleteIfExists(dest)
    }
  }

}
