package scala.build.options

final case class ScalacOpt(value: String) {

  /** @return raw key for the option (if valid) */
  private[options] def key: Option[String] =
    if value.startsWith("-") then Some(value.takeWhile(_ != ':'))
    else Some("@").filter(value.startsWith)

  /** @return raw key for the option (only if the key can be shadowed from the CLI) */
  private[options] def shadowableKey: Option[String] =
    key.filterNot(key => ScalacOpt.repeatingKeys.exists(_.startsWith(key)))
}

object ScalacOpt {
  private val repeatingKeys = Set(
    "-Xplugin:",
    "-P", // plugin options
    "-language:"
  )

  implicit val hashedType: HashedType[ScalacOpt] = {
    opt => opt.value
  }
  implicit val keyOf: ShadowingSeq.KeyOf[ScalacOpt] =
    ShadowingSeq.KeyOf(
      _.shadowableKey,
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

  extension (opts: ShadowingSeq[ScalacOpt]) {
    def filterScalacOptionKeys(f: String => Boolean): ShadowingSeq[ScalacOpt] =
      opts.filterKeys(_.key.exists(f))
  }
}
