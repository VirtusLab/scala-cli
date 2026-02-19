package scala.cli.integration

class ReplTests213 extends ReplTestDefinitions with ReplAmmoniteTestDefinitions with Test213 {
  for {
    withExplicitScala2SnapshotRepo <- Seq(true, false)
    nightlyVersion      = "2.13.nightly"
    scalaVersionOptions = Seq("--scala", nightlyVersion)
    repoOptions         =
      if withExplicitScala2SnapshotRepo then
        Seq(
          "--repository",
          "https://scala-ci.typesafe.com/artifactory/scala-2.13-snapshots"
        )
      else
        Seq.empty
    repoString = if withExplicitScala2SnapshotRepo then " with Scala 2 snapshot repo" else ""
  }
    test(s"$dryRunPrefix repl Scala 2 snapshots: $nightlyVersion$repoString") {
      dryRun(
        cliOptions = scalaVersionOptions ++ repoOptions,
        useExtraOptions = false
      )
    }
}
