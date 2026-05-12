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

  private def executableExt(isWindows: Boolean): String = if isWindows then ".exe" else ""

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
    if javaHomeInfo.version < 9 then
      Left(
        JShellUnavailable(
          s"JShell requires JDK >= 9, but the selected JDK is ${javaHomeInfo.version}. Consider using --jvm 17."
        )
      )
    else {
      val jshellPath    = javaHomeInfo.javaHome / "bin" / s"jshell${executableExt(isWindows)}"
      val jshellCommand = jshellPath.toString
      if !os.exists(jshellPath) then
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
          if quitAfterInit then {
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
    if dryRun then {
      logger.message(s"JShell command: ${command.processCommand.mkString(" ")}")
      logger.message("Dry run, not running REPL.")
      Right(())
    }
    else {
      val process =
        if allowExecve then
          Runner.maybeExec("jshell", command.processCommand, logger, extraEnv = command.extraEnv)
        else Runner.run(command.processCommand, logger, extraEnv = command.extraEnv)
      val retCode = process.waitFor()
      if retCode == 0 then Right(())
      else Left(new Repl.ReplError(retCode))
    }
  }
}
