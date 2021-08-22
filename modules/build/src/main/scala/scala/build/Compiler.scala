package scala.build

import ch.epfl.scala.bsp4j
import java.nio.file.FileSystemException
import scala.build.internal.Constants
import scala.build.options.BuildOptions
import scala.build.blooprifle.BloopRifleConfig
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import os.Path
import scala.build.options.JavaOptions
import scala.build.internal.Runner

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

class BareScalacFactory(directories: Directories) extends CompilerFactory{
  def mkDriver[T](inputs: Inputs, options: BuildOptions, logger: Logger)(op: CompileDriver => T): T = {
    val artifacts = CompilerArtifacts.resolve(inputs, options, logger, directories)
    op(BareScalacDriver(artifacts, this, options.javaCommand(), options.javaOptions))
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

case class BareScalacDriver(
  artrifacts: CompilerArtifacts, 
  factory: CompilerFactory,
  javaCommand: String,
  javaOptions: JavaOptions
  ) extends CompileDriver {
    def runBuild(
      inputs: Inputs, 
      logger: Logger, 
      classesDir0: Path, 
      artifacts: Artifacts, 
      project: Project): CompilationResult = {

        val args = 
          project.scalaCompiler.scalacOptions ++
          Seq("-d", project.classesDir.toIO.getAbsolutePath(), "-cp", project.classPath.mkString(":")) ++
            project.sources.map(_.toIO.getAbsolutePath()) ++ 
            project.generatedSources.map(_.generated.toIO.getAbsolutePath())
            

        os.remove.all(project.classesDir)
        os.makeDir.all(project.classesDir)

        val res = Runner.runJvm(
          javaCommand,
          javaOptions.javaOpts,
          project.scalaCompiler.compilerClassPath.map(_.toFile),
          artrifacts.mainClass,
          args,
          logger
        )
        CompilationResult(res == 0, None)
      }
    
    def shutdown(): Unit = ()
    
    def compilerFactory: CompilerFactory = factory
    
    def prepareBuild(logger: Logger, classesDir0: Path, project: Project): Boolean = false
}