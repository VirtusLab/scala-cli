package scala.build.postprocessing

import java.nio.file.FileSystemException

import scala.annotation.tailrec
import scala.build.{GeneratedSource, Logger}

case object SemanticDbPostProcessor extends PostProcessor {
  def postProcess(
    generatedSources: Seq[GeneratedSource],
    mappings: Map[String, (String, Int)],
    workspace: os.Path,
    output: os.Path,
    logger: Logger
  ): Unit = {
    logger.debug("Moving semantic DBs around")
    val semDbRoot = output / "META-INF" / "semanticdb"
    for (source <- generatedSources; originalSource <- source.reportingPath) {
      val fromSourceRoot = source.generated.relativeTo(workspace)
      val actual         = originalSource.relativeTo(workspace)

      val semDbSubPath = {
        val dirSegments = fromSourceRoot.segments.dropRight(1)
        os.sub / dirSegments / s"${fromSourceRoot.last}.semanticdb"
      }
      val semDbFile = semDbRoot / semDbSubPath
      if (os.exists(semDbFile)) {
        val finalSemDbFile = {
          val dirSegments = actual.segments.dropRight(1)
          semDbRoot / dirSegments / s"${actual.last}.semanticdb"
        }
        SemanticdbProcessor.postProcess(
          os.read(originalSource),
          originalSource.relativeTo(workspace),
          None,
          if (source.topWrapperLen == 0) n => Some(n)
          else
            LineConversion.scalaLineToScLine(
              os.read(originalSource),
              os.read(source.generated),
              source.topWrapperLen
            ),
          semDbFile,
          finalSemDbFile
        )
        try os.remove(semDbFile)
        catch {
          case ex: FileSystemException =>
            logger.debug(s"Ignoring $ex while removing $semDbFile")
        }
        deleteSubPathIfEmpty(semDbRoot, semDbSubPath / os.up, logger)
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
