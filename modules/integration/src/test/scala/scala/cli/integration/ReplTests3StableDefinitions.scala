package scala.cli.integration

trait ReplTests3StableDefinitions { _: ReplTestDefinitions =>
  if (!actualScalaVersion.equals(actualMaxAmmoniteScalaVersion)) {
    lazy val defaultScalaVersionString =
      s" with Scala $actualScalaVersion (the default version, may downgrade)"
    test(s"ammonite$defaultScalaVersionString") {
      ammoniteTest(useMaxAmmoniteScalaVersion = false)
    }

    test(s"ammonite scalapy$defaultScalaVersionString") {
      ammoniteScalapyTest(useMaxAmmoniteScalaVersion = false)
    }

    test(s"ammonite with test scope sources$defaultScalaVersionString") {
      ammoniteTestScope(useMaxAmmoniteScalaVersion = false)
    }
  }
}
