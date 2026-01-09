package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class ReplTestsDefault extends ReplTestDefinitions
    with ReplAmmoniteTestDefinitions
    with ReplAmmoniteTests3StableDefinitions
    with TestDefault {
  if canRunInRepl then
    for { nightlyTag <- List("3.nightly", "nightly") }
      test(
        s"$runInReplPrefix $nightlyTag returns the same Scala version as <latest-minor>.nightly"
      ) {
        val code = """println(scala.util.Properties.versionNumberString)"""
        runInRepl(code, cliOptions = Seq("-S", nightlyTag)) { r1 =>
          val version1 = r1.out.trim()
          System.err.println(s"$nightlyTag returns the following nightly: $version1")
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
