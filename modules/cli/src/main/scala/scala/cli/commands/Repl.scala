package scala.cli.commands

import caseapp.core.app.CaseApp
import caseapp.core.RemainingArgs
import scala.cli.{Build, Inputs, Os, ReplArtifacts, Runner}

object Repl extends CaseApp[ReplOptions] {
  def run(options: ReplOptions, args: RemainingArgs): Unit = {

    val inputs = Inputs(args.all, Os.pwd, defaultInputs = Some(Inputs.default())) match {
      case Left(message) =>
        System.err.println(message)
        sys.exit(1)
      case Right(i) => i
    }

    // TODO Add watch support?

    val build = Build.build(inputs, options.shared.buildOptions, options.shared.logger, Os.pwd)

    val successfulBuild = build.successfulOpt.getOrElse {
      System.err.println("Compilation failed")
      sys.exit(1)
    }

    val replArtifacts = ReplArtifacts(
      options.shared.scalaVersion,
      options.ammoniteVersion,
      build.artifacts.dependencies,
      options.shared.logger
    )

    // TODO Warn if some entries of build.artifacts.classPath were evicted in replArtifacts.replClassPath
    //      (should be artifacts whose version was bumped by Ammonite).

    // TODO Find the common namespace of all user classes, and import it all in the Ammonite session.

    // TODO Allow to disable printing the welcome banner and the "Loading..." message in Ammonite.

    // FIXME Seems Ammonite isn't fully fine with directories as class path (these are passed to the interactive
    //       compiler for completion, but not to the main compiler for actual compilation).

    Runner.run(
      build.artifacts.javaHome.toIO,
      options.sharedJava.allJavaOpts,
      successfulBuild.output.toIO +: replArtifacts.replClassPath.map(_.toFile),
      ammoniteMainClass,
      Nil,
      options.shared.logger,
      allowExecve = true
    )
  }

  private def ammoniteMainClass: String =
    "ammonite.Main"
}
