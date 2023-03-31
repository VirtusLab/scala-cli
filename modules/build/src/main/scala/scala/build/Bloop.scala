package scala.build

import bloop.rifle.{BloopRifleConfig, BuildServer}
import ch.epfl.scala.bsp4j
import coursier.cache.FileCache
import coursier.util.Task
import dependency.parser.ModuleParser
import dependency.{AnyDependency, DependencyLike, ScalaParameters, ScalaVersion}

import java.io.{File, IOException}

import scala.annotation.tailrec
import scala.build.EitherCps.{either, value}
import scala.build.errors.{BuildException, ModuleFormatError}
import scala.build.internal.CsLoggerUtil._
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._

object Bloop {

  private object BrokenPipeInCauses {
    @tailrec
    def unapply(ex: Throwable): Option[IOException] =
      ex match {
        case null                                                           => None
        case ex: IOException if ex.getMessage == "Broken pipe"              => Some(ex)
        case ex: IOException if ex.getMessage == "Connection reset by peer" => Some(ex)
        case _                                                              => unapply(ex.getCause)
      }
  }

  def compile(
    projectName: String,
    buildServer: BuildServer,
    logger: Logger,
    buildTargetsTimeout: FiniteDuration
  ): Either[Throwable, Boolean] =
    try {
      logger.debug("Listing BSP build targets")
      val results = buildServer.workspaceBuildTargets()
        .get(buildTargetsTimeout.length, buildTargetsTimeout.unit)
      val buildTargetOpt = results.getTargets.asScala.find(_.getDisplayName == projectName)

      val buildTarget = buildTargetOpt.getOrElse {
        throw new Exception(
          s"Expected to find project '$projectName' in build targets (only got ${results.getTargets.asScala.map("'" + _.getDisplayName + "'").mkString(", ")})"
        )
      }

      logger.debug(s"Compiling $projectName with Bloop")
      val compileRes = buildServer.buildTargetCompile(
        new bsp4j.CompileParams(List(buildTarget.getId).asJava)
      ).get()

      val success = compileRes.getStatusCode == bsp4j.StatusCode.OK
      logger.debug(if (success) "Compilation succeeded" else "Compilation failed")
      Right(success)
    }
    catch {
      case ex @ BrokenPipeInCauses(e) =>
        logger.debug(s"Caught $ex while exchanging with Bloop server, assuming Bloop server exited")
        Left(ex)
    }

  def bloopClassPath(
    dep: AnyDependency,
    params: ScalaParameters,
    logger: Logger,
    cache: FileCache[Task]
  ): Either[BuildException, Seq[File]] =
    either {
      val res = value {
        Artifacts.artifacts(
          Positioned.none(Seq(dep)),
          Nil,
          Some(params),
          logger,
          cache.withMessage(s"Downloading compilation server ${dep.version}")
        )
      }
      res.map(_._2.toIO)
    }

  def bloopClassPath(
    logger: Logger,
    cache: FileCache[Task]
  ): Either[BuildException, (Seq[File], Boolean)] =
    bloopClassPath(logger, cache, BloopRifleConfig.defaultVersion)

  def bloopClassPath(
    logger: Logger,
    cache: FileCache[Task],
    bloopVersion: String
  ): Either[BuildException, (Seq[File], Boolean)] = either {
    val moduleStr = BloopRifleConfig.defaultModule
    val mod = value {
      ModuleParser.parse(moduleStr)
        .left.map(err => new ModuleFormatError(moduleStr, err, Some("Bloop")))
    }
    val dep             = DependencyLike(mod, bloopVersion)
    val sv              = BloopRifleConfig.defaultScalaVersion
    val sbv             = ScalaVersion.binary(sv)
    val params          = ScalaParameters(sv, sbv)
    val cp              = value(bloopClassPath(dep, params, logger, cache))
    val isScalaCliBloop = moduleStr.startsWith(BloopRifleConfig.scalaCliBloopOrg + ":")
    (cp, isScalaCliBloop)
  }
}
