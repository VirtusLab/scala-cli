package scala.build.options

final case class ReplOptions(
  ammoniteVersionOpt: Option[String] = None
) {
  def orElse(other: ReplOptions): ReplOptions =
    ReplOptions(
      ammoniteVersionOpt = ammoniteVersionOpt.orElse(other.ammoniteVersionOpt)
    )

  def addHashData(update: String => Unit): Unit = {
    for (ver <- ammoniteVersionOpt)
      update("ammoniteVersion+=" + ver + "\n")
  }
}
