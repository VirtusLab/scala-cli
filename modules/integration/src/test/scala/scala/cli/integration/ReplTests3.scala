package scala.cli.integration

// format: off
class ReplTests3 extends ReplTestDefinitions(
  scalaVersionOpt = Some(Constants.scala3)
) {
  // format: on
  override protected def versionNumberString: String =
    "2.13.6"
}
