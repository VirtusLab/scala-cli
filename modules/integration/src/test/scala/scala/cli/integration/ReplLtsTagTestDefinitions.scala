package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

trait ReplLtsTagTestDefinitions { this: ReplTestDefinitions =>
  import Constants.{scala3LtsPrefix, scala3LegacyLtsPrefix}
  if canRunInRepl then {
    // the tags resolve to versions unrelated to the suite's own Scala version
    def replScalaVersion(tag: String, expectedPrefix: String)(check: String => Unit): Unit =
      runInRepl(
        s"""println(${retrieveScalaVersionCode(expectedPrefix)})""",
        cliOptions = Seq("-S", tag, "--with-compiler"),
        skipScalaVersionArgs = true
      )(r => check(TestUtil.stableStdout(r)))

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
        replScalaVersion(ltsNightlyTag, expectedPrefix) { version1 =>
          System.err.println(s"$ltsNightlyTag returns the following nightly: $version1")
          replScalaVersion(s"$expectedPrefix.nightly", expectedPrefix) { version2 =>
            expect(version1 == version2)
            expect(version1.startsWith(s"$expectedPrefix."))
            val patch = version1.split('.').take(3).last.takeWhile(_.isDigit).toInt
            if expectedPrefix == "3.3" then expect(patch >= 8) // new nightly repo
          }
        }
      }
  }
}
