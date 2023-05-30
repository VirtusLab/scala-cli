package scala.build

import scala.build.internal.WrapperParams

final case class GeneratedSource(
  generated: os.Path,
  reportingPath: Either[String, os.Path],
  wrapperParamsOpt: Option[WrapperParams]
)
