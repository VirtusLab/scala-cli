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
      val localRepoOpt = LocalRepo.localRepo(directories.localRepoDir)
      def cp(deps: Seq[AnyDependency]): Seq[Path] =
        Artifacts.artifacts(deps, localRepoOpt.toSeq, options.scalaParams, logger).map(_._2)

      val (compilerName, compilerMain) = 
        if (options.scalaParams.scalaVersion.startsWith("2."))
          ("scala-compiler", "scala.tools.nsc.Main") 
        else 
          ("scala3-compiler", "dotty.tools.dotc.Main")

      val scaladocDep = Seq(dep"org.scala-lang::$compilerName:${options.scalaParams.scalaVersion}")


      CompilerArtifacts(cp(scaladocDep), compilerMain)
    }
}
