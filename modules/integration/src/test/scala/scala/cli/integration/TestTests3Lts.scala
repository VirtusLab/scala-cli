package scala.cli.integration

import scala.util.Properties

class TestTests3LtsWithBloop extends TestTestDefinitions with Test3Lts with TestWithBloop

class TestTests3LtsWithoutBloop extends TestTestDefinitions with Test3Lts with TestWithoutBloop {
  override def munitIgnore: Boolean = super.munitIgnore || Properties.isWin
}
