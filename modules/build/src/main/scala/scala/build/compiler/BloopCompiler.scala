package scala.build.compiler

import scala.build.{Bloop, Logger, Position, Positioned, Project, bloop}
import scala.concurrent.duration.FiniteDuration

final class BloopCompiler(
  val bloopServer: bloop.BloopServer,
  buildTargetsTimeout: FiniteDuration,
  strictBloopJsonCheck: Boolean
) extends ScalaCompiler {
  def jvmVersion: Option[Positioned[Int]] =
    Some(
      Positioned(
        List(Position.Bloop(bloopServer.bloopInfo.javaHome)),
        bloopServer.bloopInfo.jvmVersion
      )
    )

  def prepareProject(
    project: Project,
    logger: Logger
  ): Boolean =
    project.writeBloopFile(strictBloopJsonCheck, logger)

  def compile(
    project: Project,
    logger: Logger
  ): Boolean =
    Bloop.compile(project.projectName, bloopServer, logger, buildTargetsTimeout)

  def shutdown(): Unit =
    bloopServer.shutdown()
}
