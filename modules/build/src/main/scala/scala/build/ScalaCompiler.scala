package scala.build

final case class ScalaCompiler(
  scalaVersion: String,
  scalaBinaryVersion: String,
  scalacOptions: Seq[String],
  compilerClassPath: Seq[os.Path]
)
