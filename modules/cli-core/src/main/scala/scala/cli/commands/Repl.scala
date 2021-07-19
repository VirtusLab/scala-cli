package scala.cli.commands

import caseapp._
import scala.build.{Build, Inputs, Os, ReplArtifacts}
import scala.build.internal.Runner

object Repl extends ScalaCommand[ReplOptions] {
  override def group = "Main"
  override def names = List(
    List("console"),
    List("repl")
  )
  override def sharedOptions(options: ReplOptions) = Some(options.shared)
  def run(options: ReplOptions, args: RemainingArgs): Unit = {

    val inputs = options.shared.inputsOrExit(args, defaultInputs = Some(Inputs.default()))

    // TODO Add watch support?

    val buildOptions = options.buildOptions
    val bloopRifleConfig = options.shared.bloopRifleConfig()

    val build = Build.build(inputs, buildOptions, bloopRifleConfig, options.shared.logger)

    val successfulBuild = build.successfulOpt.getOrElse {
      System.err.println("Compilation failed")
      sys.exit(1)
    }

    val replArtifacts = ReplArtifacts(
      build.artifacts.params,
      options.ammoniteVersion,
      build.artifacts.dependencies,
      options.shared.logger,
      options.shared.directories.directories
    )

    // TODO Warn if some entries of build.artifacts.classPath were evicted in replArtifacts.replClassPath
    //      (should be artifacts whose version was bumped by Ammonite).

    // TODO Find the common namespace of all user classes, and import it all in the Ammonite session.

    // TODO Allow to disable printing the welcome banner and the "Loading..." message in Ammonite.

    // FIXME Seems Ammonite isn't fully fine with directories as class path (these are passed to the interactive
    //       compiler for completion, but not to the main compiler for actual compilation).

    lazy val rootClasses = os.list(successfulBuild.output)
      .filter(_.last.endsWith(".class"))
      .filter(os.isFile(_)) // just in case
      .map(_.last.stripSuffix(".class"))
      .sorted
    if (options.shared.logging.verbosity >= 0 && rootClasses.nonEmpty)
      System.err.println(s"Warning: found classes defined in the root package (${rootClasses.mkString(", ")}). These will not be accessible from the REPL.")
    Runner.run(
      successfulBuild.options.javaCommand(),
      successfulBuild.options.javaOptions.javaOpts,
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
