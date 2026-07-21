package scala.cli.integration

class FixTestsDefault extends FixTestDefinitions with FixSlothTestDefinitions with TestDefault {
  override val scalafixUnusedRuleOption: String = "-Wunused:all"
}
