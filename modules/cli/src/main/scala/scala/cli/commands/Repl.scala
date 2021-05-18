package scala.cli.commands

import caseapp.core.app.CaseApp
import caseapp.core.RemainingArgs
import scala.cli.{Build, Inputs, ReplArtifacts, Runner}

object Repl extends CaseApp[ReplOptions] {
  def run(options: ReplOptions, args: RemainingArgs): Unit = {

    val inputs = Inputs(args.all, os.pwd, defaultInputs = Some(Inputs.default())) match {
      case Left(message) =>
        System.err.println(message)
        sys.exit(1)
      case Right(i) => i
    }

    val build = Build.build(inputs, options.shared.buildOptions, options.shared.logger, os.pwd)

    val replArtifacts = ReplArtifacts(
      options.shared.scalaVersion,
      options.ammoniteVersion,
      build.artifacts.dependencies
    )

    // TODO Warn if some entries of build.artifacts.classPath were evicted in replArtifacts.replClassPath
    //      (should be artifacts whose version was bumped by Ammonite).

    // TODO Find the common namespace of all user classes, and import it all in the Ammonite session.

    // TODO Allow to disable printing the welcome banner and the "Loading..." message in Ammonite.

    // FIXME Seems Ammonite isn't fully fine with directories as class path (these are passed to the interactive
    //       compiler for completion, but not to the main compiler for actual compilation).

    Runner.run(
      build.artifacts.javaHome.toIO,
      build.output.toIO +: replArtifacts.replClassPath.map(_.toFile),
      ammoniteMainClass,
      Nil,
      options.shared.logger,
      allowExecve = true
    )
  }

  private def ammoniteMainClass: String =
    "ammonite.Main"
}
