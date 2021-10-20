package scala.build.postprocessing

import scala.build.internal.Constants
import scala.build.tastylib.{TastyData, TastyVersions}
import scala.build.{GeneratedSource, Logger}

case object TastyPostProcessor extends PostProcessor {

  def postProcess(
    generatedSources: Seq[GeneratedSource],
    mappings: Map[String, (String, Int)],
    workspace: os.Path,
    output: os.Path,
    logger: Logger,
    scalaVersion: String
  ): Either[String, Unit] = {

    def updatedPaths = generatedSources
      .flatMap { source =>
        source.reportingPath.toOption.toSeq.map { originalSource =>
          val fromSourceRoot = source.generated.relativeTo(workspace)
          val actual         = originalSource.relativeTo(workspace)
          fromSourceRoot.toString -> actual.toString
        }
      }
      .toMap

    TastyVersions.shouldRunPreprocessor(scalaVersion, Constants.version) match {
      case Right(false) => Right(())
      case Left(msg)    => if (updatedPaths.isEmpty) Right(()) else Left(msg)
      case Right(true) =>
        val paths = updatedPaths
        if (paths.isEmpty) Right(())
        else Right(
          os.walk(output)
            .filter(os.isFile(_))
            .filter(_.last.endsWith(".tasty")) // make that case-insensitive just in case?
            .foreach(updateTastyFile(logger, paths))
        )
    }
  }

  private def updateTastyFile(
    logger: Logger,
    updatedPaths: Map[String, String]
  )(f: os.Path): Unit = {
    logger.debug(s"Reading TASTy file $f")
    val content = os.read.bytes(f)
    val data    = TastyData.read(content)
    logger.debug(s"Parsed TASTy file $f")
    var updatedOne = false
    val updatedData = data.mapNames { n =>
      updatedPaths.get(n) match {
        case Some(newName) =>
          updatedOne = true
          newName
        case None =>
          n
      }
    }
    if (updatedOne) {
      logger.debug(
        s"Overwriting ${if (f.startsWith(os.pwd)) f.relativeTo(os.pwd) else f}"
      )
      val updatedContent = TastyData.write(updatedData)
      os.write.over(f, updatedContent)
    }
  }
}
