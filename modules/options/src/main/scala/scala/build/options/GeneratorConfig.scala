package scala.build.options

import scala.build.Positioned
import scala.build.errors.{BuildException, MalformedInputError}

final case class GeneratorConfig(
  inputDir: String,
  glob: List[String],
  commandFilePath: String,
  outputPath: os.SubPath
)
