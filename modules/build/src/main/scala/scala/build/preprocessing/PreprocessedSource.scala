package scala.build.preprocessing

import scala.build.options.BuildOptions

sealed abstract class PreprocessedSource extends Product with Serializable {
  def options: Option[BuildOptions]
  def mainClassOpt: Option[String]
}

object PreprocessedSource {

  final case class OnDisk(
    path: os.Path,
    options: Option[BuildOptions],
    mainClassOpt: Option[String]
  ) extends PreprocessedSource
  final case class InMemory(
    reportingPath: Either[String, os.Path],
    relPath: os.RelPath,
    code: String,
    ignoreLen: Int,
    options: Option[BuildOptions],
    mainClassOpt: Option[String]
  ) extends PreprocessedSource
  final case class NoSource(options: Option[BuildOptions]) extends PreprocessedSource {
    def mainClassOpt: None.type = None
  }

}
