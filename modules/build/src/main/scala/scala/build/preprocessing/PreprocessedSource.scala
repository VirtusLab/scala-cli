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

  private def index(s: PreprocessedSource): Int =
    s match {
      case _: NoSourceCode => 0
      case _: InMemory     => 1
      case _: OnDisk       => 2
    }

  implicit val ordering: Ordering[PreprocessedSource] =
    new Ordering[PreprocessedSource] {
      def compare(a: PreprocessedSource, b: PreprocessedSource): Int = {
        val aIdx   = index(a)
        val bIdx   = index(b)
        val idxCmp = aIdx.compare(bIdx)
        if (idxCmp == 0)
          (a, b) match {
            case (a0: NoSourceCode, b0: NoSourceCode) =>
              a0.path.toString.compareTo(b0.path.toString)
            case (a0: InMemory, b0: InMemory) =>
              (a0.reportingPath, b0.reportingPath) match {
                case (Left(ap), Left(bp))   => ap.compareTo(bp)
                case (Left(_), Right(_))    => -1
                case (Right(ap), Right(bp)) => ap.toString.compareTo(bp.toString)
                case (Right(_), Left(_))    => 1
              }
            case (a0: OnDisk, b0: OnDisk) => a0.path.toString.compareTo(b0.path.toString)
            case _                        => ???
          }
        else idxCmp
      }
    }

}
