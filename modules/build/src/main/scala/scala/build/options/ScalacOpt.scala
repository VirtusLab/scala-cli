package scala.build.options

final case class ScalacOpt(value: String) {
  def key: Option[String] =
    if (value.startsWith("-"))
      Some(value.takeWhile(_ != ':'))
        .filterNot(ScalacOpt.repeatingKeys.contains)
    else if (value.startsWith("@"))
      Some("@")
    else
      None
}

object ScalacOpt {
  private val repeatingKeys = Set(
    "-Xplugin:",
    "-P" // plugin options
  )

  implicit val hashedType: HashedType[ScalacOpt] = {
    opt => opt.value
  }
  implicit val keyOf: ShadowingSeq.KeyOf[ScalacOpt] =
    ShadowingSeq.KeyOf(
      _.key,
      seq => groupCliOptions(seq.map(_.value))
    )

  // Groups options (starting with `-` or `@`) with option arguments that follow
  def groupCliOptions(opts: Seq[String]): Seq[Int] =
    opts
      .zipWithIndex
      .collect {
        case (opt, idx) if opt.startsWith("-") || opt.startsWith("@") =>
          idx
      }
}
