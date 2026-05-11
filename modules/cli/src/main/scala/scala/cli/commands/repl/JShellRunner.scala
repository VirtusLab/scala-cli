package scala.cli.commands.repl

import java.io.File

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.internal.Runner
import scala.build.options.BuildOptions
import scala.util.Properties

object JShellRunner {

  final case class Command(
    jshellCommand: String,
    args: Seq[String],
    extraEnv: Map[String, String]
  ) {
    def processCommand: Seq[String]   = Seq(jshellCommand) ++ args
    def displayedCommand: Seq[String] = Runner.envCommand(extraEnv) ++ processCommand
  }

  final class JShellUnavailable(message0: String)
      extends BuildException(message0)
  final class ReplInitScriptError(message0: String, cause0: Throwable = null)
      extends BuildException(message0, cause = cause0)

  final case class ParsedReplArgs(
    initScriptOpt: Option[String],
    quitAfterInit: Boolean,
    remainingArgs: Seq[String]
  )

  private def executableExt(isWindows: Boolean): String =
    if (isWindows) ".exe"
    else ""

  def readInitScriptFile(file: String): Either[BuildException, String] = {
    val pathEither: Either[BuildException, os.Path] =
      try Right(os.Path(file, os.pwd))
      catch {
        case e: IllegalArgumentException =>
          Left(ReplInitScriptError(s"Invalid REPL init script file path: $file", e))
      }
    pathEither.flatMap { path =>
      if (!os.exists(path))
        Left(ReplInitScriptError(s"REPL init script file not found: $path"))
      else if (os.isDir(path))
        Left(ReplInitScriptError(s"REPL init script file is a directory: $path"))
      else
        try Right(os.read(path))
        catch {
          case e: Exception =>
            Left(ReplInitScriptError(
              s"Error reading REPL init script file $path: ${e.getMessage}",
              e
            ))
        }
    }
  }

  def parseReplArgs(args: Seq[String]): Either[BuildException, ParsedReplArgs] = {
    val b                                 = Seq.newBuilder[String]
    var initScriptOpt: Option[String]     = None
    var initScriptFileOpt: Option[String] = None
    var quitAfterInit                     = false
    var idx                               = 0
    while (idx < args.length) {
      val arg = args(idx)
      if (arg == "--repl-init-script" || arg == "-repl-init-script")
        if (idx + 1 < args.length) {
          initScriptOpt = Some(args(idx + 1))
          idx += 1
        }
        else b += arg
      else if (arg.startsWith("--repl-init-script:") || arg.startsWith("-repl-init-script:"))
        initScriptOpt = Some(arg.dropWhile(_ != ':').drop(1))
      else if (arg == "--repl-init-script-file" || arg == "-repl-init-script-file")
        if (idx + 1 < args.length) {
          initScriptFileOpt = Some(args(idx + 1))
          idx += 1
        }
        else b += arg
      else if (
        arg.startsWith("--repl-init-script-file:") || arg.startsWith("-repl-init-script-file:")
      )
        initScriptFileOpt = Some(arg.dropWhile(_ != ':').drop(1))
      else if (arg == "--repl-quit-after-init" || arg == "-repl-quit-after-init")
        quitAfterInit = true
      else b += arg
      idx += 1
    }
    if (initScriptOpt.nonEmpty && initScriptFileOpt.nonEmpty)
      Left(ReplInitScriptError(
        "--repl-init-script cannot be used together with --repl-init-script-file"
      ))
    else {
      val resolvedInitScriptOpt =
        initScriptOpt match {
          case some @ Some(_) => Right(some)
          case None           => initScriptFileOpt match {
              case Some(file) => readInitScriptFile(file).map(Some(_))
              case None       => Right(None)
            }
        }
      resolvedInitScriptOpt.map { resolvedInitScriptOpt =>
        ParsedReplArgs(
          initScriptOpt = resolvedInitScriptOpt,
          quitAfterInit = quitAfterInit,
          remainingArgs = b.result()
        )
      }
    }
  }

  def commandFor(
    javaHomeInfo: BuildOptions.JavaHomeInfo,
    javaOpts: Seq[String],
    classPath: Seq[os.Path],
    programArgs: Seq[String],
    initScriptOpt: Option[String],
    quitAfterInit: Boolean,
    currentEnv: Map[String, String],
    isWindows: Boolean = Properties.isWin
  ): Either[BuildException, Command] =
    if (javaHomeInfo.version < 9)
      Left(
        JShellUnavailable(
          s"JShell requires JDK >= 9, but the selected JDK is ${javaHomeInfo.version}. Consider using --jvm 17."
        )
      )
    else {
      val jshellPath    = javaHomeInfo.javaHome / "bin" / s"jshell${executableExt(isWindows)}"
      val jshellCommand = jshellPath.toString
      if (!os.exists(jshellPath))
        Left(
          JShellUnavailable(
            s"JShell executable not found at $jshellCommand. Ensure the selected JVM is a full JDK (for example with --jvm 17)."
          )
        )
      else {
        val classPathArg = classPath.map(_.toString).distinct.mkString(File.pathSeparator)
        val startupArgs  = initScriptOpt.toSeq.flatMap { script =>
          val scriptFile = os.temp(
            prefix = "scala-cli-jshell-init-",
            suffix = ".jsh",
            deleteOnExit = false
          )
          os.write.over(scriptFile, script + System.lineSeparator())
          Seq("--startup", "DEFAULT", "--startup", scriptFile.toString)
        }
        val quitAfterInitArgs =
          if (quitAfterInit) {
            val exitFile = os.temp(
              prefix = "scala-cli-jshell-exit-",
              suffix = ".jsh",
              deleteOnExit = false
            )
            os.write.over(exitFile, "/exit" + System.lineSeparator())
            Seq(exitFile.toString)
          }
          else Nil
        val vmArgs = javaOpts.map(opt => s"-J$opt")
        val args   = Seq("--class-path", classPathArg) ++
          vmArgs ++
          startupArgs ++
          programArgs ++
          quitAfterInitArgs
        val extraEnv = javaHomeInfo.envUpdates(currentEnv)
        Right(Command(jshellCommand, args, extraEnv))
      }
    }

  def run(
    command: Command,
    logger: Logger,
    allowExecve: Boolean,
    dryRun: Boolean
  ): Either[BuildException, Unit] = {
    logger.log(
      s"Running ${command.displayedCommand.mkString(" ")}",
      "  Running" + System.lineSeparator() +
        command.displayedCommand.iterator.map(_ + System.lineSeparator()).mkString
    )
    if (dryRun) {
      logger.message(s"JShell command: ${command.processCommand.mkString(" ")}")
      logger.message("Dry run, not running REPL.")
      Right(())
    }
    else {
      val process =
        if (allowExecve)
          Runner.maybeExec("jshell", command.processCommand, logger, extraEnv = command.extraEnv)
        else
          Runner.run(command.processCommand, logger, extraEnv = command.extraEnv)
      val retCode = process.waitFor()
      if (retCode == 0) Right(())
      else Left(new Repl.ReplError(retCode))
    }
  }
}
