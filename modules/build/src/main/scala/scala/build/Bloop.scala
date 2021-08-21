package scala.build

import ch.epfl.scala.bsp4j
import dependency.{AnyDependency, Dependency, DependencyLike, ScalaParameters, ScalaVersion}
import dependency.parser.ModuleParser

import java.io.File
import java.nio.file.Path

import scala.build.blooprifle.BloopRifleConfig
import scala.build.internal.Util.ScalaDependencyOps
import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration
import scala.util.Properties
import java.nio.file.FileSystemException
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.build.options.BuildOptions
import scala.build.internal.Constants

trait CompilerFactory {
  def mkDriver[T]( 
    inputs: Inputs,
    options: BuildOptions,
    logger: Logger)(
    op: CompileDriver => T): T
}

class BloopDriverFactory(
    bloopConfig: BloopRifleConfig,
    threads: BuildThreads
  ) extends CompilerFactory {

  def mkDriver[T]( 
    inputs: Inputs,
    options: BuildOptions,
    logger: Logger)(
    op: CompileDriver => T): T = {
    val buildClient = BloopBuildClient.create(
      logger,
      keepDiagnostics = options.internal.keepDiagnostics
    )
    val classesDir0 = Build.classesDir(inputs.workspace, inputs.projectName)
    bloop.BloopServer.withBuildServer(
      bloopConfig,
      "scala-cli",
      Constants.version,
      inputs.workspace.toNIO,
      classesDir0.toNIO,
      buildClient,
      threads.bloop,
      logger.bloopRifleLogger
    )(bloopServer => op(BloopDriver(buildClient, bloopServer, this)))
  }
}

case class CompilationResult(success: Boolean, diags: Option[Seq[(Either[String, os.Path], bsp4j.Diagnostic)]])

trait CompileDriver {
  def runBuild(inputs: Inputs, logger: Logger, classesDir0: os.Path, artifacts: Artifacts, project: Project): CompilationResult
  def shutdown(): Unit
  def compilerFactory: CompilerFactory
  def prepareBuild(logger: Logger, classesDir0: os.Path, project: Project): Boolean
}
case class BloopDriver(
    buildClient: BloopBuildClient,
    bloopServer: bloop.BloopServer,
    compilerFactory: CompilerFactory
) extends CompileDriver {

  def shutdown(): Unit = bloopServer.shutdown()


  def prepareBuild(logger: Logger, classesDir0: os.Path, project: Project): Boolean = {
    buildClient.setProjectParams(project.projectParams)
        val updatedBloopConfig = project.writeBloopFile(logger)

        if (updatedBloopConfig && os.isDir(classesDir0)) {
          logger.debug(s"Clearing $classesDir0")
          os.list(classesDir0).foreach { p =>
            logger.debug(s"Removing $p")
            try os.remove.all(p)
            catch {
              case ex: FileSystemException =>
                logger.debug(s"Ignoring $ex while cleaning up $p")
            }
          }
        }
        updatedBloopConfig
  }

  def runBuild(inputs: Inputs, logger: Logger, classesDir0: os.Path, artifacts: Artifacts, project: Project): CompilationResult = {
    import project._

   

    buildClient.clear()
    buildClient.setGeneratedSources(generatedSources)

    val success = Bloop.compile(
      inputs.projectName,
      buildClient,
      bloopServer,
      logger,
      buildTargetsTimeout = 20.seconds
    )
    CompilationResult(success, buildClient.diagnostics)
  }
}

object Bloop {

  def compile(
    projectName: String,
    buildClient: bsp4j.BuildClient,
    bloopServer: bloop.BloopServer,
    logger: Logger,
    buildTargetsTimeout: FiniteDuration
  ): Boolean = {

    logger.debug("Listing BSP build targets")
    val results = bloopServer.server.workspaceBuildTargets().get(buildTargetsTimeout.length, buildTargetsTimeout.unit)
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
    val moduleStr = BloopRifleConfig.defaultModule
    val mod = ModuleParser.parse(moduleStr) match {
      case Left(err) => sys.error(s"Error parsing default bloop module '$moduleStr'")
      case Right(mod) => mod
    }
    val dep = DependencyLike(mod, BloopRifleConfig.defaultVersion)
    val sv = Properties.versionNumberString
    val sbv = ScalaVersion.binary(sv)
    val params = ScalaParameters(sv, sbv)
    bloopClassPath(dep, params, logger)
  }
}
