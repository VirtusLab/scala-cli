package scala.build

import java.nio.file.Path

final case class ScalaCompiler(
  scalaVersion: String,
  scalaBinaryVersion: String,
  scalacOptions: Seq[String],
  compilerClassPath: Seq[Path]
)
