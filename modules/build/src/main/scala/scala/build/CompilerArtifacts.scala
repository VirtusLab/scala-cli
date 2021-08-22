package scala.build

import java.nio.file.Path

import dependency._
import scala.build.options.BuildOptions

final case class CompilerArtifacts(
  classPath: Seq[Path],
  mainClass: String
)

object CompilerArtifacts {

  def resolve(
      inputs: Inputs, options: BuildOptions, logger: Logger, directories: Directories
    ): CompilerArtifacts = {
      val localRepoOpt = LocalRepo.localRepo(directories.localRepoDir).toSeq

      val (scalacDef, compilerMain) = 
        if (options.scalaParams.scalaVersion.startsWith("2."))
          (dep"org.scala-lang:scala-compiler:${options.scalaParams.scalaVersion}", "scala.tools.nsc.Main") 
        else 
          (dep"org.scala-lang::scala3-compiler:${options.scalaParams.scalaVersion}", "dotty.tools.dotc.Main")

      val scalacDep = Artifacts.artifacts(Seq(scalacDef), localRepoOpt, options.scalaParams, logger)
        
      CompilerArtifacts(scalacDep.map(_._2), compilerMain)
    }
}
