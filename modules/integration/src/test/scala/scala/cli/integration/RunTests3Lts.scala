package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class RunTests3Lts extends RunTestDefinitions with Test3Lts {
  import Constants.scala3LtsPrefix
  for (ltsNightlyAlias <- List("lts.nightly", "3.lts.nightly"))
    test(s"Scala $ltsNightlyAlias & $scala3LtsPrefix.nightly point to the same version") {
      TestInputs.empty.fromRoot { root =>
        val version1      = getScalaVersion(ltsNightlyAlias, root)
        val nightlyPrefix = version1.split('.').take(2).mkString(".")
        expect(nightlyPrefix == Constants.scala3LtsPrefix)
        val nightlyPatch = version1.split('.').take(3).last.takeWhile(_.isDigit).toInt
        if nightlyPrefix == "3.3" then expect(nightlyPatch >= 8) // new nightly repo
        val version2 = getScalaVersion(s"${Constants.scala3LtsPrefix}.nightly", root)
        expect(version1 == version2)
      }
    }
}
