package scala.build.compiler

import ch.epfl.scala.bsp4j

import scala.annotation.tailrec
import scala.build.{Bloop, Logger, Position, Positioned, Project}
import scala.concurrent.duration.FiniteDuration

final class BloopCompiler(
  createServer: () => bloop.rifle.BloopServer,
  buildTargetsTimeout: FiniteDuration,
  strictBloopJsonCheck: Boolean
) extends ScalaCompiler {
  private var currentBloopServer: bloop.rifle.BloopServer =
    createServer()
  def bloopServer: bloop.rifle.BloopServer =
    currentBloopServer

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
  ): Boolean = {
    @tailrec
    def helper(remainingAttempts: Int): Boolean =
      Bloop.compile(project.projectName, bloopServer.server, logger, buildTargetsTimeout) match {
        case Right(res) => res
        case Left(ex) =>
          if (remainingAttempts > 1) {
            logger.debug(s"Seems Bloop server exited (got $ex), trying to restart one")
            currentBloopServer = createServer()
            helper(remainingAttempts - 1)
          }
          else
            throw new Exception(
              "Seems compilation server exited, and wasn't able to restart one",
              ex
            )
      }

    helper(2)
  }

  def shutdown(): Unit =
    bloopServer.shutdown()
}
