package scala.cli.integration

class ExportMillTestsDefault extends ExportMillTestDefinitions with TestDefault {
  override lazy val majorScalaVersion: String = "3" // todo: is it correct?
}
