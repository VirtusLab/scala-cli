package scala.build

import ch.epfl.scala.bsp4j

import scala.collection.JavaConverters._
import java.io.File
import java.nio.file.Path
import scala.build.bloop.bloopgun.BloopgunConfig
import dependency.parser.ModuleParser
import dependency.Dependency
import dependency.DependencyLike
import dependency.ScalaParameters
import scala.build.internal.Util.ScalaDependencyOps
import scala.util.Properties
import dependency.AnyDependency
import dependency.ScalaVersion

object Bloop {

  def compile(
    projectName: String,
    buildClient: bsp4j.BuildClient,
    bloopServer: bloop.BloopServer,
    logger: Logger
  ): Boolean = {

    logger.debug("Listing BSP build targets")
    val results = bloopServer.server.workspaceBuildTargets().get()
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
  ): Seq[File] =
    Artifacts.artifacts(Seq(dep), Nil, params, logger).map(_._2.toFile)

  def bloopClassPath(logger: Logger): Seq[File] = {
    val moduleStr = BloopgunConfig.defaultModule
    val mod = ModuleParser.parse(moduleStr) match {
      case Left(err) => sys.error(s"Error parsing default bloop module '$moduleStr'")
      case Right(mod) => mod
    }
    val dep = DependencyLike(mod, BloopgunConfig.defaultVersion)
    val sv = Properties.versionNumberString
    val sbv = ScalaVersion.binary(sv)
    val params = ScalaParameters(sv, sbv)
    bloopClassPath(dep, params, logger)
  }
}
