package scala.cli.commands

import caseapp._
import scala.build.{Build, Inputs, Os, ReplArtifacts, Runner}

object Repl extends ScalaCommand[ReplOptions] {
  override def group = "Main"
  override def names = List(
    List("console"),
    List("repl")
  )
  def run(options: ReplOptions, args: RemainingArgs): Unit = {

    val directories = options.shared.directories.directories
    val inputs = Inputs(args.all, Os.pwd, directories, defaultInputs = Some(Inputs.default())) match {
      case Left(message) =>
        System.err.println(message)
        sys.exit(1)
      case Right(i) => i
    }

    // TODO Add watch support?

    val buildOptions = options.buildOptions
    val bloopgunConfig = options.shared.bloopgunConfig()

    val javaCommand = options.shared.javaCommand()

    val build = Build.build(inputs, buildOptions, bloopgunConfig, options.shared.logger, Os.pwd)

    val successfulBuild = build.successfulOpt.getOrElse {
      System.err.println("Compilation failed")
      sys.exit(1)
    }

    val replArtifacts = ReplArtifacts(
      build.artifacts.params,
      options.ammoniteVersion,
      build.artifacts.dependencies,
      options.shared.logger,
      directories
    )

    // TODO Warn if some entries of build.artifacts.classPath were evicted in replArtifacts.replClassPath
    //      (should be artifacts whose version was bumped by Ammonite).

    // TODO Find the common namespace of all user classes, and import it all in the Ammonite session.

    // TODO Allow to disable printing the welcome banner and the "Loading..." message in Ammonite.

    // FIXME Seems Ammonite isn't fully fine with directories as class path (these are passed to the interactive
    //       compiler for completion, but not to the main compiler for actual compilation).

    Runner.run(
      javaCommand,
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
