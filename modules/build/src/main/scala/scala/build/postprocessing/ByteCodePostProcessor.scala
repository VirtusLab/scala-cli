package scala.build.postprocessing

import scala.build.options.BuildOptions
import scala.build.{GeneratedSource, Logger}
import scala.util.{Either, Right}

case object ByteCodePostProcessor extends PostProcessor {
  def postProcess(
    generatedSources: Seq[GeneratedSource],
    mappings: Map[String, (String, Int)],
    workspace: os.Path,
    output: os.Path,
    logger: Logger,
    scalaVersion: String,
    buildOptions: BuildOptions
  ): Either[String, Unit] =
    Right(AsmPositionUpdater.postProcess(mappings, output, logger))
}
