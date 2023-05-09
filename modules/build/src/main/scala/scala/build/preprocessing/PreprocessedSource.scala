package scala.build.preprocessing

import scala.build.Position
import scala.build.internal.CodeWrapper
import scala.build.options.{BuildOptions, BuildRequirements, WithBuildRequirements}

sealed abstract class PreprocessedSource extends Product with Serializable {
  def options: Option[BuildOptions]
  def optionsWithTargetRequirements: List[WithBuildRequirements[BuildOptions]]
  def requirements: Option[BuildRequirements]
  def mainClassOpt: Option[String]

  def scopedRequirements: Seq[Scoped[BuildRequirements]]
  def scopePath: ScopePath
  def directivesPositions: Option[Position.File]
}

object PreprocessedSource {

  final case class OnDisk(
    path: os.Path,
    options: Option[BuildOptions],
    optionsWithTargetRequirements: List[WithBuildRequirements[BuildOptions]],
    requirements: Option[BuildRequirements],
    scopedRequirements: Seq[Scoped[BuildRequirements]],
    mainClassOpt: Option[String],
    directivesPositions: Option[Position.File]
  ) extends PreprocessedSource {
    def scopePath: ScopePath =
      ScopePath.fromPath(path)
  }
  final case class InMemory(
    originalPath: Either[String, (os.SubPath, os.Path)],
    relPath: os.RelPath,
    code: String,
    ignoreLen: Int,
    options: Option[BuildOptions],
    optionsWithTargetRequirements: List[WithBuildRequirements[BuildOptions]],
    requirements: Option[BuildRequirements],
    scopedRequirements: Seq[Scoped[BuildRequirements]],
    mainClassOpt: Option[String],
    scopePath: ScopePath,
    directivesPositions: Option[Position.File]
  ) extends PreprocessedSource {
    def reportingPath: Either[String, os.Path] =
      originalPath.map(_._2)
  }

  final case class UnwrappedScript(
    originalPath: Either[String, (os.SubPath, os.Path)],
    relPath: os.RelPath,
    options: Option[BuildOptions],
    optionsWithTargetRequirements: List[WithBuildRequirements[BuildOptions]],
    requirements: Option[BuildRequirements],
    scopedRequirements: Seq[Scoped[BuildRequirements]],
    mainClassOpt: Option[String],
    scopePath: ScopePath,
    directivesPositions: Option[Position.File],
    wrapScriptFun: CodeWrapper => (String, Int)
  ) extends PreprocessedSource

  final case class NoSourceCode(
    options: Option[BuildOptions],
    optionsWithTargetRequirements: List[WithBuildRequirements[BuildOptions]],
    requirements: Option[BuildRequirements],
    scopedRequirements: Seq[Scoped[BuildRequirements]],
    path: os.Path
  ) extends PreprocessedSource {
    def mainClassOpt: None.type = None
    def scopePath: ScopePath =
      ScopePath.fromPath(path)
    def directivesPositions: None.type = None
  }
}
