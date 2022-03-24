package scala.build.options

final case class JavaOpt(value: String) {
  def key: Option[String] =
    JavaOpt.optionPrefixes.find(value.startsWith)
      .orElse {
        if (value.startsWith("-"))
          Some(value.takeWhile(_ != ':'))
            .filterNot(key => JavaOpt.repeatingKeys.exists(_.startsWith(key)))
        else if (value.startsWith("@")) Some("@")
        else None
      }
}

object JavaOpt {
  private val repeatingKeys = Set(
    "--add-exports",
    "--add-modules",
    "--add-opens",
    "--add-reads",
    "--patch-module"
  )

  /* Hardcoded prefixes for java options */
  private val optionPrefixes = Set("-Xmn", "-Xms", "-Xmx", "-Xss")

  implicit val hashedType: HashedType[JavaOpt] = {
    opt => opt.value
  }
  implicit val keyOf: ShadowingSeq.KeyOf[JavaOpt] =
    ShadowingSeq.KeyOf(
      _.key,
      seq => ScalacOpt.groupCliOptions(seq.map(_.value))
    )
}
