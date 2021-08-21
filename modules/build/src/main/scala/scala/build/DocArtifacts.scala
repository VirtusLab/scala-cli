package scala.build

import java.nio.file.Path

import dependency._

final case class DocArtifacts(
  classPath: Seq[Path],
  mainClass: String,
  args: Seq[String],
)

object DocArtifacts {

  def scaladoc3(
      scalaParams: ScalaParameters,
      dependencies: Seq[AnyDependency],
      logger: Logger,
      directories: Directories,
    ): DocArtifacts = {
      val localRepoOpt = LocalRepo.localRepo(directories.localRepoDir)
      def cp(deps: Seq[AnyDependency]): Seq[Path] =
        Artifacts.artifacts(deps, localRepoOpt.toSeq, scalaParams, logger).map(_._2)

      val scaladocDep = Seq(dep"org.scala-lang::scaladoc:${scalaParams.scalaVersion}")
      val args = Seq( "-classpath", cp(dependencies).mkString(":"))

      DocArtifacts(cp(scaladocDep) , "dotty.tools.scaladoc.Main", args)
    }

  // def scaladoc2()
}
