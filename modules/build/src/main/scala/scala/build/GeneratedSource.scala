package scala.build

final case class GeneratedSource(
  generated: os.Path,
  reportingPath: Either[String, os.Path],
  topWrapperLen: Int
)
