package scala.cli.integration

class FixTests3NextRc extends FixTestDefinitions with Test3NextRc {
  override val scalafixUnusedRuleOption: String = "-Wunused:all"
}
