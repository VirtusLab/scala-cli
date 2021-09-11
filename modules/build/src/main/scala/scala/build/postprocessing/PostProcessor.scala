package scala.build.postprocessing

import scala.build.{GeneratedSource, Logger}

trait PostProcessor {
  def postProcess(
    generatedSources: Seq[GeneratedSource],
    mappings: Map[String, (String, Int)],
    workspace: os.Path,
    output: os.Path,
    logger: Logger
  ): Unit
}
