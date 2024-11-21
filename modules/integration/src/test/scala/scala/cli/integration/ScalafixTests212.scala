package scala.cli.integration

class ScalafixTests212 extends ScalafixTestDefinitions with Test212 {
  override val unusedRuleOption: String = "-Ywarn-unused"
}
