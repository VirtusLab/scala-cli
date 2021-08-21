package scala.cli.commands

import caseapp._

import scala.build.{Inputs, Os}
import dependency._
import scala.build.LocalRepo
import scala.build.Artifacts
import scala.build.DocArtifacts
import scala.build.Build
import scala.build.internal.Runner

object Doc extends ScalaCommand[DocOptions] {

  override def group = "Main"

  def run(options: DocOptions, args: RemainingArgs): Unit = {
    Compile.runCompile(options, args){ 
      case build: Build.Successful =>
        val buildOps = options.buildOptions
        val directories = options.shared.directories.directories
        val artrifacts =  DocArtifacts.scaladoc(
          buildOps.scalaParams, 
          build.artifacts.dependencies, 
          options.shared.logger, 
          directories
        )
        val dest = build.output / os.up / "doc"
        os.makeDir.all(dest)

        val args = 
          Seq( // scaladoc configuration
            "-d", dest.toNIO.toString()
          ) ++ artrifacts.args ++ // scaladoc classpath
          Seq(build.output.toIO.getAbsolutePath()) // class diretctory to get .tasty files

        Runner.runJvm(
          build.options.javaCommand(),
          build.options.javaOptions.javaOpts,
          artrifacts.classPath.map(_.toFile),
          artrifacts.mainClass,
          args,
          options.shared.logger
        )
        
      case _ =>
        println("Compiation failed!")      
    }
  }
}
