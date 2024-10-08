package scala.build

import scala.build.internal.WrapperParams

/** Represents a source that's not originally in the user's workspace, yet it's a part of the
  * project. It can either be synthetically generated by Scala CLI, e.g. BuildInfo or just modified,
  * e.g. script wrappers
  *
  * @param generated
  *   path to the file created by Scala CLI
  * @param reportingPath
  *   the origin of the source:
  *   - Left(String): there's no path that corresponds to the source it may be a snippet or a gist
  *     etc.
  *   - Right(os.Path): this source has been generated based on a file at this path
  * @param wrapperParamsOpt
  *   if the generated source is a script wrapper then the params are present here
  */
final case class GeneratedSource(
  generated: os.Path,
  reportingPath: Either[String, os.Path],
  wrapperParamsOpt: Option[WrapperParams]
)
