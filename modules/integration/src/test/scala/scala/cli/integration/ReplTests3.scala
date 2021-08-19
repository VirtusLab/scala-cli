package scala.cli.integration

class ReplTests3 extends ReplTestDefinitions(
  scalaVersionOpt = Some(Constants.scala3)
) {
  override protected def versionNumberString: String =
    "2.13.6"
}
