package scala.cli.integration

class FixTests213 extends FixTestDefinitions with Test213 {
  override val scalafixUnusedRuleOption: String = "-Wunused"
}
