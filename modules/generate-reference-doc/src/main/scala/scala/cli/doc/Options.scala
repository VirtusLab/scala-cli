package scala.cli.doc

import caseapp._

final case class Options(
  outputDir: String = "docs/reference",
  check: Boolean = false
) {
  lazy val outputPath: os.Path =
    os.Path(outputDir, os.pwd)
}
