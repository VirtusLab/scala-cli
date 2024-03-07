package scala.build.postprocessing

import java.nio.file.FileSystemException

import scala.annotation.tailrec
import scala.build.options.BuildOptions
import scala.build.postprocessing.LineConversion.scalaLineToScLine
import scala.build.{GeneratedSource, Logger}
import scala.util.{Either, Right}

case object SemanticDbPostProcessor extends PostProcessor {
  def postProcess(
    generatedSources: Seq[GeneratedSource],
    mappings: Map[String, (String, Int)],
    workspace: os.Path,
    output: os.Path,
    logger: Logger,
    scalaVersion: String,
    buildOptions: BuildOptions
  ): Either[String, Unit] = Right {
    logger.debug("Moving semantic DBs around")
    val semanticDbOptions = buildOptions.scalaOptions.semanticDbOptions
    val semDbSourceRoot   = semanticDbOptions.semanticDbSourceRoot.getOrElse(workspace)
    val semDbTargetRoot =
      semanticDbOptions.semanticDbTargetRoot.getOrElse(output) / "META-INF" / "semanticdb"
    for (source <- generatedSources; originalSource <- source.reportingPath) {
      val fromSourceRoot = source.generated.relativeTo(semDbSourceRoot)
      val actual         = originalSource.relativeTo(semDbSourceRoot)

      val semDbSubPath = {
        val dirSegments = fromSourceRoot.segments.dropRight(1)
        os.sub / dirSegments / s"${fromSourceRoot.last}.semanticdb"
      }
      val semDbFile = semDbTargetRoot / semDbSubPath
      if (os.exists(semDbFile)) {
        val finalSemDbFile = {
          val dirSegments = actual.segments.dropRight(1)
          semDbTargetRoot / dirSegments / s"${actual.last}.semanticdb"
        }
        SemanticdbProcessor.postProcess(
          os.read(originalSource),
          originalSource.relativeTo(semDbSourceRoot),
          scalaLine => scalaLineToScLine(scalaLine, source.wrapperParamsOpt),
          semDbFile,
          finalSemDbFile
        )
        try os.remove(semDbFile)
        catch {
          case ex: FileSystemException =>
            logger.debug(s"Ignoring $ex while removing $semDbFile")
        }
        deleteSubPathIfEmpty(semDbTargetRoot, semDbSubPath / os.up, logger)
      }
    }
  }

  @tailrec
  private def deleteSubPathIfEmpty(base: os.Path, subPath: os.SubPath, logger: Logger): Unit =
    if (subPath.segments.nonEmpty) {
      val p = base / subPath
      if (os.isDir(p) && os.list.stream(p).headOption.isEmpty) {
        try os.remove(p)
        catch {
          case e: FileSystemException =>
            logger.debug(s"Ignoring $e while cleaning up $p")
        }
        deleteSubPathIfEmpty(base, subPath / os.up, logger)
      }
    }
}
