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

  def run(options: DocOptions, remaningArgs: RemainingArgs): Unit = {
    Compile.runCompile(options, remaningArgs){ 
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
            buildOps.scalaOptions.scalacOptions ++ // apply compiler options
            remaningArgs.unparsed ++ // provided ad-hoc args
            defaultScaladocArgs ++ // default args
            Seq(build.output.toIO.getAbsolutePath()) // class diretctory to get .tasty files

        Runner.runJvm(
          build.options.javaCommand(),
          build.options.javaOptions.javaOpts,
          artrifacts.classPath.map(_.toFile),
          artrifacts.mainClass,
          args,
          options.shared.logger
        )
        println(s"Documetnation generated in $dest")
        
      case _ =>
        println("Compiation failed!")      
    }
  }

  def defaultScaladocArgs = Seq(
    "-snippet-compiler:compile", 
    "-Ygenerate-inkuire",
    "-external-mappings:" +
        ".*scala.*::scaladoc3::https://scala-lang.org/api/3.x/," +
        ".*java.*::javadoc::https://docs.oracle.com/javase/8/docs/api/",
    "-author",
    "-groups"
  )
}
