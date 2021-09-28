package scala.build

import ch.epfl.scala.bsp4j
import dependency.{AnyDependency, DependencyLike, ScalaParameters, ScalaVersion}
import dependency.parser.ModuleParser

import java.io.File

import scala.build.blooprifle.BloopRifleConfig
import scala.build.EitherCps.{either, value}
import scala.build.errors.ModuleFormatError
import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration
import scala.util.Properties
import scala.build.errors.BuildException

object Bloop {

  def compile(
    projectName: String,
    bloopServer: bloop.BloopServer,
    logger: Logger,
    buildTargetsTimeout: FiniteDuration
  ): Boolean = {

    logger.debug("Listing BSP build targets")
    val results = bloopServer.server.workspaceBuildTargets()
      .get(buildTargetsTimeout.length, buildTargetsTimeout.unit)
    val buildTargetOpt = results.getTargets.asScala.find(_.getDisplayName == projectName)

    val buildTarget = buildTargetOpt.getOrElse {
      throw new Exception(
        s"Expected to find project '$projectName' in build targets (only got ${results.getTargets.asScala.map("'" + _.getDisplayName + "'").mkString(", ")})"
      )
    }

    logger.debug(s"Compiling $projectName with Bloop")
    val compileRes = bloopServer.server.buildTargetCompile(
      new bsp4j.CompileParams(List(buildTarget.getId).asJava)
    ).get()

    val success = compileRes.getStatusCode == bsp4j.StatusCode.OK
    logger.debug(if (success) "Compilation succeeded" else "Compilation failed")
    success
  }

  def bloopClassPath(
    dep: AnyDependency,
    params: ScalaParameters,
    logger: Logger
  ): Either[BuildException, Seq[File]] =
    either {
      value(Artifacts.artifacts(Seq(dep), Nil, params, logger))
        .map(_._2.toFile)
    }

  def bloopClassPath(logger: Logger): Either[BuildException, Seq[File]] = either {
    val moduleStr = BloopRifleConfig.defaultModule
    val mod = value {
      ModuleParser.parse(moduleStr)
        .left.map(err => new ModuleFormatError(moduleStr, err, Some("Bloop")))
    }
    val dep    = DependencyLike(mod, BloopRifleConfig.defaultVersion)
    val sv     = Properties.versionNumberString
    val sbv    = ScalaVersion.binary(sv)
    val params = ScalaParameters(sv, sbv)
    value(bloopClassPath(dep, params, logger))
  }
}
