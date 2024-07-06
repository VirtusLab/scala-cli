package scala.build.options

import scala.build.Positioned
import scala.build.errors.{BuildException, MalformedInputError}

final case class GeneratorConfig(
  inputDir: String,
  glob: List[String],
  commandFilePath: String
)

object GeneratorConfig {

  def parse(input: Positioned[String]): Either[BuildException, GeneratorConfig] =
    input.value.split("\\|", 3) match {
      case Array(inputDir, glob, commandFilePath) =>
        Right(GeneratorConfig(inputDir, List(glob), commandFilePath))
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

  // def formatPath(
  //   inputDir: String,
  //   glob: String,
  //   commandFilePath: String,
  // ): GeneratorConfig = {

  // }
}
