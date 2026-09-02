package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class ReplTests3Lts extends ReplTestDefinitions with Test3Lts
    with ReplJShellTestDefinitions {
  import Constants.{scala3LtsPrefix, scala3LegacyLtsPrefix}
  if canRunInRepl then {
    val versionCode = s"""println($retrieveScalaVersionCode)"""

    def replScalaVersion(tag: String)(check: String => Unit): Unit =
      runInRepl(
        versionCode,
        cliOptions = Seq("-S", tag, "--with-compiler"),
        skipScalaVersionArgs = true
      )(r => check(r.out.trim()))

    for {
      (ltsNightlyTag, expectedPrefix) <- List(
        "3.lts.nightly"                       -> scala3LtsPrefix,
        "lts.nightly"                         -> scala3LtsPrefix,
        s"$scala3LegacyLtsPrefix.lts.nightly" -> scala3LegacyLtsPrefix
      ).distinct
    }
      test(
        s"$runInReplPrefix $ltsNightlyTag returns the same Scala version as $expectedPrefix.nightly"
      ) {
        replScalaVersion(ltsNightlyTag) { version1 =>
          System.err.println(s"$ltsNightlyTag returns the following nightly: $version1")
          replScalaVersion(s"$expectedPrefix.nightly") { version2 =>
            expect(version1 == version2)
            expect(version1.startsWith(s"$expectedPrefix."))
            val patch = version1.split('.').take(3).last.takeWhile(_.isDigit).toInt
            if expectedPrefix == "3.3" then expect(patch >= 8) // new nightly repo
          }
        }
      }
  }
}
