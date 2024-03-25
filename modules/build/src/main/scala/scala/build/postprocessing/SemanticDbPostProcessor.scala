package scala.build.postprocessing

import java.nio.file.FileSystemException

import scala.annotation.tailrec
import scala.build.options.BuildOptions
import scala.build.postprocessing.LineConversion.scalaLineToScLine
import scala.build.{GeneratedSource, Logger}
import scala.util.{Either, Right, Try}

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
      val actual = originalSource.relativeTo(semDbSourceRoot)

      val generatedSourceParent = os.Path(source.generated.toNIO.getParent)
      val potentialSemDbFile    = generatedSourceParent / (source.generated.last + ".semanticdb")
      Some(potentialSemDbFile)
        .filter(os.exists)
        .orElse(Some(semDbTargetRoot / potentialSemDbFile.relativeTo(semDbSourceRoot)))
        .filter(os.exists)
        .foreach { semDbFile =>
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
          Try(semDbTargetRoot -> semDbFile.relativeTo(semDbTargetRoot).asSubPath).toOption
            .foreach { (base, subPath) =>
              deleteSubPathIfEmpty(base, subPath / os.up, logger)
            }
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
