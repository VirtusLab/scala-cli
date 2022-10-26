package scala.cli.doc

import caseapp.*

final case class InternalDocOptions(
  outputDir: String = "website/docs/reference",
  check: Boolean = false
) {
  lazy val outputPath: os.Path =
    os.Path(outputDir, os.pwd)
}

object InternalDocOptions {
  implicit lazy val parser: Parser[InternalDocOptions] = Parser.derive
  implicit lazy val help: Help[InternalDocOptions]     = Help.derive
}
