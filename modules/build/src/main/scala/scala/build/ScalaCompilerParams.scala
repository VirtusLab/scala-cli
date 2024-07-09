package scala.build

final case class ScalaCompilerParams(
  scalaVersion: String,
  scalaBinaryVersion: String,
  scalacOptions: Seq[String],
  compilerClassPath: Seq[os.Path],
  bridgeJarsOpt: Option[Seq[os.Path]],
  argsFiles: Seq[os.Path]
)
