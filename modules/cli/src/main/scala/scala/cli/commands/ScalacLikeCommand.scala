package scala.cli.commands
import scala.build.compiler.SimpleScalaCompiler
import scala.build.options.{BuildOptions, Scope}
import scala.cli.commands.util.SharedOptionsUtil._

/** Trait to mixin for commands which are intended to mimic `scalac` behaviour, allowing for
  * printing scalac help & other options, even without passing input sources. Meant as an extension
  * of [[scala.cli.commands.ScalaCommand]]
  *
  * @tparam T
  *   command options type
  */
trait ScalacLikeCommand[T] { self: ScalaCommand[T] =>
  def buildOptions(t: T): BuildOptions

  /** Print `scalac` output if passed options imply no inputs are necessary and raw `scalac` output
    * is required instead. (i.e. `--scalac-option -help`)
    * @param options
    *   command options
    */
  def maybePrintSimpleScalacOutput(options: T): Unit =
    for {
      shared <- sharedOptions(options)
      build         = buildOptions(options)
      scalacOptions = shared.scalac.scalacOption.toSeq
      updatedScalacOptions =
        if (shared.scalacExtra.scalacHelp && !scalacOptions.contains("-help"))
          scalacOptions.appended("-help")
        else scalacOptions
      if (updatedScalacOptions intersect ScalacOptions.ScalacPrintOptions.toSeq).nonEmpty
      logger = shared.logger
      artifacts      <- build.artifacts(logger, Scope.Main).toOption
      scalaArtifacts <- artifacts.scalaOpt
      compilerClassPath   = scalaArtifacts.compilerClassPath
      scalaVersion        = scalaArtifacts.params.scalaVersion
      compileClassPath    = artifacts.compileClassPath
      simpleScalaCompiler = SimpleScalaCompiler("java", Nil, scaladoc = false)
      javacOptions        = build.javaOptions.javacOptions
      javaHome            = build.javaHomeLocation().value
    } {
      val exitCode = simpleScalaCompiler.runSimpleScalacLike(
        scalaVersion,
        Option(javaHome),
        javacOptions,
        updatedScalacOptions,
        compileClassPath,
        compilerClassPath,
        logger
      )
      sys.exit(exitCode)
    }
}
