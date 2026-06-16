package scala.cli.integration

import scala.util.Properties

class TestTests3NextRcWithBloop extends TestTestDefinitions with Test3NextRc with TestWithBloop

class TestTests3NextRcWithoutBloop extends TestTestDefinitions with Test3NextRc
    with TestWithoutBloop {
  override def munitIgnore: Boolean = super.munitIgnore || Properties.isWin
}
