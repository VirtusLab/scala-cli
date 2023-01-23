package scala.build

import dependency.{AnyDependency, ScalaParameters}

final case class ScalaArtifacts(
  compilerDependencies: Seq[AnyDependency],
  compilerArtifacts: Seq[(String, os.Path)],
  compilerPlugins: Seq[(AnyDependency, String, os.Path)],
  scalaJsCli: Seq[os.Path],
  scalaNativeCli: Seq[os.Path],
  internalDependencies: Seq[AnyDependency],
  extraDependencies: Seq[AnyDependency],
  params: ScalaParameters,
  bridgeJarsOpt: Option[Seq[os.Path]]
) {

  lazy val compilerClassPath: Seq[os.Path] =
    compilerArtifacts.map(_._2)
}
