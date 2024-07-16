package scala.cli.integration

class ExportSbtTestsDefault extends ExportSbtTestDefinitions with TestDefault {
  override lazy val majorScalaVersion: String = "3" // todo: is it correct
}
