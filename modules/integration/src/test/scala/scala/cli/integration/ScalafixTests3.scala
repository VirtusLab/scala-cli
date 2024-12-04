package scala.cli.integration

class ScalafixTests3 extends ScalafixTestDefinitions with TestDefault {
  override val unusedRuleOption: String = "-Wunused:all"
}
