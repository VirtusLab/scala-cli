package scala.cli.integration

class FixTests3Lts extends FixTestDefinitions with Test3Lts {
  override val scalafixUnusedRuleOption: String = "-Wunused:all"
}
