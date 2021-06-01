package scala.build

import java.nio.file.Path

import ch.epfl.scala.bsp4j
import org.eclipse.lsp4j.jsonrpc

import scala.cli.internal.Constants
import scala.collection.JavaConverters._

object Bloop {

  def compile(
    workspace: os.Path,
    classesDir: os.Path,
    projectName: String,
    buildClient: bsp4j.BuildClient,
    threads: bloop.BloopThreads,
    logger: Logger,
    bloopVersion: String = Constants.bloopVersion
  ): Boolean = {

    val bloopServer = bloop.BloopServer.buildServer(
      "scala-cli",
      Constants.version,
      workspace.toNIO,
      classesDir.toNIO,
      buildClient,
      threads,
      logger.bloopgunLogger
    )

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

    bloopServer.shutdown()

    val success = compileRes.getStatusCode == bsp4j.StatusCode.OK
    logger.debug(if (success) "Compilation succeeded" else "Compilation failed")
    success
  }

}
