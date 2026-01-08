package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class ReplTestsDefault extends ReplTestDefinitions
    with ReplAmmoniteTestDefinitions
    with ReplAmmoniteTests3StableDefinitions
    with TestDefault {
  if canRunInRepl then
    test(
      s"$runInReplPrefix 3.nightly returns the same Scala version as <latest-minor>.nightly"
    ) {
      val code = """println(scala.util.Properties.versionNumberString)"""
      runInRepl(code, cliOptions = Seq("-S", "3.nightly")) { r1 =>
        val version1 = r1.out.trim()
        System.err.println(s"3.nightly returns the following nightly: $version1")
        val nightlyPrefix = version1.split('.').take(2).mkString(".")
        runInRepl(code, cliOptions = Seq("-S", s"$nightlyPrefix.nightly")) { r2 =>
          val version2 = r2.out.trim()
          System.err.println(s"$nightlyPrefix.nightly returns the following nightly: $version2")
          expect(version1 == version2)
          val major = version1.split('.').take(1).head.toInt
          expect(major == 3)
          val minor = version1.split('.').take(2).last.toInt
          expect(minor >= Constants.scala3NextPrefix.split('.').last.toInt)
        }
      }
    }
}
