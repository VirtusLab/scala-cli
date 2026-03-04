package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class ReplTests3Lts extends ReplTestDefinitions with Test3Lts
    with ReplAmmoniteTestDefinitions
    with ReplAmmoniteTests3StableDefinitions {
  import Constants.scala3LtsPrefix
  if canRunInRepl then
    for { ltsNightlyTag <- List("3.lts.nightly", "lts.nightly") }
      test(
        s"$runInReplPrefix $ltsNightlyTag returns the same Scala version as $scala3LtsPrefix.nightly"
      ) {
        val code = s"""println($retrieveScalaVersionCode)"""
        runInRepl(
          code,
          cliOptions = Seq("-S", ltsNightlyTag, "--with-compiler"),
          skipScalaVersionArgs = true
        ) { r1 =>
          val version1 = r1.out.trim()
          System.err.println(s"$ltsNightlyTag returns the following nightly: $version1")
          runInRepl(
            code,
            cliOptions = Seq("-S", s"$scala3LtsPrefix.nightly", "--with-compiler"),
            skipScalaVersionArgs = true
          ) { r2 =>
            val version2 = r2.out.trim()
            expect(version1 == version2)
            val major = version1.split('.').take(1).head.toInt
            expect(major == 3)
            val minor = version1.split('.').take(2).last.toInt
            expect(minor >= scala3LtsPrefix.split('.').last.toInt)
            val patch = version1.split('.').take(3).last.takeWhile(_.isDigit).toInt
            if minor == 3 then expect(patch >= 8) // new nightly repo
          }
        }
      }
}
