package scala.cli.integration

class FixTests212 extends FixTestDefinitions with Test212 {
  override val scalafixUnusedRuleOption: String = "-Ywarn-unused"
}
