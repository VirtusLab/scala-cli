package scala.build.options

final case class ReplOptions(
  ammoniteVersionOpt: Option[String] = None
) {
  def orElse(other: ReplOptions): ReplOptions =
    ReplOptions(
      ammoniteVersionOpt = ammoniteVersionOpt.orElse(other.ammoniteVersionOpt)
    )
}

object ReplOptions {
  implicit val hasHashData: HasHashData[ReplOptions] = HasHashData.derive
}
