package scala.cli.integration

class FixTests33Lts extends FixTestDefinitions with Test33Lts {
  override val scalafixUnusedRuleOption: String = "-Wunused:all"
}
