package scala.cli

import java.nio.file.Path

final case class ReplArtifacts(
  replArtifacts: Seq[(String, Path)]
) {
  lazy val replClassPath: Seq[Path] =
    replArtifacts.map(_._2)
}

object ReplArtifacts {
  // TODO In order to isolate more Ammonite dependencies, we'd need to get two class paths:
  //      - a shared one, with ammonite-repl-api, ammonite-compiler, and dependencies
  //      - an Ammonite-specific one, with the other ammonite JARs
  // Then, use the coursier-bootstrap library to generate a launcher creating to class loaders,
  // with each of those class paths, and run Ammonite with this launcher.
  // This requires to change this line in Ammonite, https://github.com/com-lihaoyi/Ammonite/blob/0f0d597f04e62e86cbf76d3bd16deb6965331470/amm/src/main/scala/ammonite/Main.scala#L99,
  // to
  //     val contextClassLoader = classOf[ammonite.repl.api.ReplAPI].getClassLoader
  // so that only the first loader is exposed to users in Ammonite.
  def apply(
    scalaVersion: String,
    ammoniteVersion: String,
    dependencies: Seq[coursierapi.Dependency]
  ): ReplArtifacts = {
    val allDeps = dependencies ++ ammoniteDependencies(ammoniteVersion, scalaVersion)
    val replArtifacts = Artifacts.artifacts(allDeps)
    ReplArtifacts(replArtifacts)
  }

  private def ammoniteDependencies(ammoniteVersion: String, scalaVersion: String): Seq[coursierapi.Dependency] =
    Seq(
      coursierapi.Dependency.of("com.lihaoyi", s"ammonite_$scalaVersion", ammoniteVersion)
    )
}
