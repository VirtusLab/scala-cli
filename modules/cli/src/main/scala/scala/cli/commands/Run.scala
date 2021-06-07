package scala.cli.commands

import java.io.{ByteArrayOutputStream, InputStream}
import java.nio.file.{Files, Path}

import caseapp._
import scala.build.{Build, Inputs, Logger, Os, Runner}
import scala.build.internal.Constants
import scala.scalanative.{build => sn}

import scala.util.Properties

object Run extends Command[RunOptions] {

  def run(options: RunOptions, args: RemainingArgs): Unit =
    run(options, args, Some(Inputs.default()))

  def run(options: RunOptions, args: RemainingArgs, defaultInputs: Option[Inputs]): Unit = {

    val pwd = Os.pwd

    val inputs = Inputs(args.remaining, pwd, defaultInputs = defaultInputs, stdinOpt = readStdin(logger = options.shared.logger), acceptFds = !Properties.isWin) match {
      case Left(message) =>
        System.err.println(message)
        sys.exit(1)
      case Right(i) => i
    }

    def maybeRun(build: Build.Successful, allowTerminate: Boolean): Unit =
      maybeRunOnce(
        options,
        inputs.workspace,
        inputs.projectName,
        build,
        args.unparsed,
        allowExecve = allowTerminate,
        exitOnError = allowTerminate,
        jvmRunner = build.options.addRunnerDependency
      )

    if (options.shared.watch) {
      val watcher = Build.watch(inputs, options.shared.buildOptions, options.shared.logger, pwd, postAction = () => WatchUtil.printWatchMessage()) {
        case s: Build.Successful =>
          maybeRun(s, allowTerminate = false)
        case f: Build.Failed =>
          System.err.println("Compilation failed")
      }
      try WatchUtil.waitForCtrlC()
      finally watcher.dispose()
    } else {
      val build = Build.build(inputs, options.shared.buildOptions, options.shared.logger, pwd)
      build match {
        case s: Build.Successful =>
          maybeRun(s, allowTerminate = true)
        case f: Build.Failed =>
          System.err.println("Compilation failed")
          sys.exit(1)
      }
    }
  }

  def maybeRunOnce(
    options: RunOptions,
    root: os.Path,
    projectName: String,
    build: Build.Successful,
    args: Seq[String],
    allowExecve: Boolean,
    exitOnError: Boolean,
    jvmRunner: Boolean
  ): Unit = {

    val mainClassOpt = options.retainedMainClass.filter(_.nonEmpty) // trim it too?
      .orElse(build.retainedMainClassOpt(warnIfSeveral = true))

    for (mainClass <- mainClassOpt) {
      val (finalMainClass, finalArgs) =
        if (jvmRunner) (Constants.runnerMainClass, mainClass +: args)
        else (mainClass, args)
      runOnce(
        options,
        root,
        projectName,
        build,
        finalMainClass,
        finalArgs,
        allowExecve,
        exitOnError
      )
    }
  }

  def runOnce(
    options: RunOptions,
    root: os.Path,
    projectName: String,
    build: Build.Successful,
    mainClass: String,
    args: Seq[String],
    allowExecve: Boolean,
    exitOnError: Boolean
  ): Boolean = {

    val retCode =
      if (options.shared.js)
        withLinkedJs(build, Some(mainClass), addTestInitializer = false) { js =>
          Runner.runJs(
            js.toIO,
            args,
            options.shared.logger,
            allowExecve = allowExecve
          )
        }
      else if (options.shared.native)
        withNativeLauncher(
          build,
          mainClass,
          options.shared.scalaNativeOptionsIKnowWhatImDoing,
          options.shared.nativeWorkDir(root, projectName),
          options.shared.scalaNativeLogger
        ) { launcher =>
          Runner.runNative(
            launcher.toIO,
            args,
            options.shared.logger,
            allowExecve = allowExecve
          )
        }
      else
        Runner.run(
          build.artifacts.javaHome.toIO,
          options.sharedJava.allJavaOpts,
          build.fullClassPath.map(_.toFile),
          mainClass,
          args,
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

    retCode == 0
  }


  def withLinkedJs[T](
    build: Build.Successful,
    mainClassOpt: Option[String],
    addTestInitializer: Boolean
  )(f: os.Path => T): T = {
    val dest = os.temp(prefix = "main", suffix = ".js")
    try {
      Package.linkJs(build, dest, mainClassOpt, addTestInitializer)
      f(dest)
    } finally {
      if (os.exists(dest))
        os.remove(dest)
    }
  }

  def withNativeLauncher[T](
    build: Build.Successful,
    mainClass: String,
    options: Build.ScalaNativeOptions,
    workDir: os.Path,
    logger: sn.Logger
  )(f: os.Path => T): T = {
    val dest = os.temp(prefix = "main", suffix = ".js")
    try {
      Package.buildNative(build, mainClass, dest, options, workDir, logger)
      f(dest)
    } finally {
      if (os.exists(dest))
        os.remove(dest)
    }
  }

  def readStdin(in: InputStream = System.in, logger: Logger): Option[Array[Byte]] =
    if (in == null) {
      logger.debug("No stdin available")
      None
    } else {
      logger.debug("Reading stdin")
      val baos = new ByteArrayOutputStream
      val buf = Array.ofDim[Byte](16*1024)
      var read = -1
      while ({
        read = in.read(buf)
        read >= 0
      }) {
        if (read > 0)
          baos.write(buf, 0, read)
      }
      val result = baos.toByteArray
      logger.debug(s"Done reading stdin (${result.length} B)")
      Some(result)
    }

}
