package scala.cli.commands
import scala.build.compiler.SimpleScalaCompiler
import scala.build.options.{BuildOptions, Scope}
import scala.cli.commands.util.SharedOptionsUtil._

trait ScalacLikeCommand[T] { self: ScalaCommand[T] =>
  def buildOptions(t: T): BuildOptions

  def maybePrintSimpleScalacOutput(options: T): Unit =
    for {
      shared <- sharedOptions(options)
      build         = buildOptions(options)
      scalacOptions = shared.scalac.scalacOption.toSeq
      if (scalacOptions intersect ScalacOptions.ScalacPrintOptions.toSeq).nonEmpty
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
        scalacOptions,
        compileClassPath,
        compilerClassPath,
        logger
      )
      sys.exit(exitCode)
    }
}
