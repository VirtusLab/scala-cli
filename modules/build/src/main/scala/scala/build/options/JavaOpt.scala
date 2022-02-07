package scala.build.options

final case class JavaOpt(value: String) {
  def key: Option[String] =
    JavaOpt.optionPrefixes.find(value.startsWith)
      .orElse {
        if (value.startsWith("-")) Some(value.takeWhile(_ != ':'))
        else if (value.startsWith("@")) Some("@")
        else None
      }
}

object JavaOpt {
  /* Hardcoded prefixes for java options */
  private val optionPrefixes = Set("-Xmn", "-Xms", "-Xmx", "-Xss")

  implicit val keyOf: ShadowingSeq.KeyOf[JavaOpt] =
    ShadowingSeq.KeyOf(
      _.key,
      seq => ScalacOpt.groupCliOptions(seq.map(_.value))
    )
}
