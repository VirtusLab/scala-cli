package scala.cli.integration

class FixTests39Lts extends FixTestDefinitions with Test39Lts {
  override val scalafixUnusedRuleOption: String = "-Wunused:all"
}
