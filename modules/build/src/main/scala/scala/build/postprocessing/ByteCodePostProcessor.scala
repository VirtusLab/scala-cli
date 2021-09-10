package scala.build.postprocessing

import scala.build.{GeneratedSource, Logger}

case object ByteCodePostProcessor extends PostProcessor {
  def postProcess(
    generatedSources: Seq[GeneratedSource],
    mappings: Map[String, (String, Int)],
    workspace: os.Path,
    output: os.Path,
    logger: Logger
  ): Unit =
    AsmPositionUpdater.postProcess(mappings, output, logger)
}
