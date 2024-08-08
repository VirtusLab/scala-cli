package scala.build.options

import scala.build.Positioned
import scala.build.errors.{BuildException, MalformedInputError}

final case class GeneratorConfig(
  inputDir: String,
  glob: List[String],
  commandFilePath: List[String],
  outputPath: os.SubPath
)

object GeneratorConfig {

  def parse(input: Positioned[String], output: os.SubPath): Either[BuildException, GeneratorConfig] =
    input.value.split("\\|", 3) match {
      case Array(inputDir, glob, commandFilePath) =>
        val commandFileList = commandFilePath.split(" ").toList
        Right(GeneratorConfig(inputDir, List(glob), commandFileList, output))
      case _ =>
        Left(
          new MalformedInputError(
            "sourceGenerator",
            input.value,
            "inputDir|glob|commandFilePath",
            input.positions
          )
        )
    }
}
