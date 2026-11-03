package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

trait ReplLtsTagTestDefinitions { this: ReplTestDefinitions =>
  import Constants.{scala3LtsPrefix, scala3LegacyLtsPrefix}
  if canRunInRepl then {
    // the tags resolve to versions unrelated to the suite's own Scala version
    // returns None if the version could not be fetched (i.e. when nightlies are mid-release)
    def replScalaVersion(tag: String, expectedPrefix: String): Option[String] =
      runInRepl(
        s"""println(${retrieveScalaVersionCode(expectedPrefix)})""",
        cliOptions = Seq("-S", tag, "--with-compiler"),
        skipScalaVersionArgs = true,
        check = false
      ) { r =>
        if r.exitCode == 0 then Some(TestUtil.stableStdout(r))
        else {
          System.err.println(
            s"Could not fetch Scala version for $tag (exit code ${r.exitCode}), skipping comparison"
          )
          None
        }
      }

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
        for {
          version1 <- replScalaVersion(ltsNightlyTag, expectedPrefix)
          _ = System.err.println(s"$ltsNightlyTag returns the following nightly: $version1")
          version2 <- replScalaVersion(s"$expectedPrefix.nightly", expectedPrefix)
        } do {
          expect(version1 == version2)
          expect(version1.startsWith(s"$expectedPrefix."))
          val patch = version1.split('.').take(3).last.takeWhile(_.isDigit).toInt
          if expectedPrefix == "3.3" then expect(patch >= 8) // new nightly repo
        }
      }
  }
}
