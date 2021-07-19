package scala.build.options

final case class JmhOptions(
  addJmhDependencies: Option[String] = None,
  runJmh: Option[Boolean] = None
) {
  def orElse(other: JmhOptions): JmhOptions =
    JmhOptions(
      addJmhDependencies = addJmhDependencies.orElse(other.addJmhDependencies),
      runJmh = runJmh.orElse(other.runJmh)
    )
}

object JmhOptions {
  implicit val hasHashData: HasHashData[JmhOptions] = HasHashData.derive
}
