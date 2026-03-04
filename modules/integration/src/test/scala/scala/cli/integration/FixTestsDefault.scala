package scala.cli.integration

class FixTestsDefault extends FixTestDefinitions with TestDefault {
  override val scalafixUnusedRuleOption: String = "-Wunused:all"
}
