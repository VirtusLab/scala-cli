package scala.build.preprocessing

import scala.build.options.BuildOptions
import scala.build.options.BuildRequirements

sealed abstract class PreprocessedSource extends Product with Serializable {
  def options: Option[BuildOptions]
  def requirements: Option[BuildRequirements]
  def mainClassOpt: Option[String]
}

object PreprocessedSource {

  final case class OnDisk(
    path: os.Path,
    options: Option[BuildOptions],
    requirements: Option[BuildRequirements],
    mainClassOpt: Option[String]
  ) extends PreprocessedSource
  final case class InMemory(
    reportingPath: Either[String, os.Path],
    relPath: os.RelPath,
    code: String,
    ignoreLen: Int,
    options: Option[BuildOptions],
    requirements: Option[BuildRequirements],
    mainClassOpt: Option[String]
  ) extends PreprocessedSource
  final case class NoSourceCode(
    options: Option[BuildOptions],
    requirements: Option[BuildRequirements],
    path: os.Path
  ) extends PreprocessedSource {
    def mainClassOpt: None.type = None
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
