package scala.build.options

import scala.build.options.ScalacOpt.noDashPrefixes

final case class ScalacOpt(value: String) {

  /** @return raw key for the option (if valid) */
  private[options] def key: Option[String] =
    if value.startsWith("-") || value.startsWith("--") then Some(value.takeWhile(_ != ':'))
    else Some("@").filter(value.startsWith)

  /** @return raw key for the option (only if the key can be shadowed from the CLI) */
  private[options] def shadowableKey: Option[String] = key match
    case Some(key)
        if ScalacOpt.repeatingKeys
          .exists(rKey =>
            rKey.startsWith(key.noDashPrefixes + ":") || rKey == key.noDashPrefixes
          ) => None
    case otherwise => otherwise
}

object ScalacOpt {
  extension (opt: String) {
    def noDashPrefixes: String = opt.stripPrefix("--").stripPrefix("-")
  }
  private val repeatingKeys = Set(
    "coverage-exclude-classlikes",
    "coverage-exclude-files",
    "language",
    "P", // plugin options
    "Wconf",
    "Wunused",
    "Wshadow",
    "Xlint",
    "Xmacro-settings",
    "Xplugin",
    "Xplugin-disable",
    "Xplugin-require",
    "Yimports",
    "Yfrom-tasty-ignore-list"
  )

  implicit val hashedType: HashedType[ScalacOpt] = {
    opt => opt.value
  }
  implicit val keyOf: ShadowingSeq.KeyOf[ScalacOpt] =
    ShadowingSeq.KeyOf(
      opts =>
        opts.headOption.flatMap(_.shadowableKey).orElse(Some(opts.map(_.value).mkString(":"))),
      seq => groupCliOptions(seq.map(_.value))
    )

  // Groups options (starting with `-`, `--` or `@`) with option arguments that follow
  def groupCliOptions(opts: Seq[String]): Seq[Int] =
    opts
      .zipWithIndex
      .collect {
        case (opt, idx) if opt.startsWith("-") || opt.startsWith("--") || opt.startsWith("@") =>
          idx
      }

  extension (opts: ShadowingSeq[ScalacOpt]) {
    def filterScalacOptionKeys(f: String => Boolean): ShadowingSeq[ScalacOpt] =
      opts.filterKeys(_.key.exists(f))
  }

  sealed abstract class PresetOption(val preset: String) extends Product with Serializable
  object PresetOption {
    case object Suggested extends PresetOption("suggested")
    case object CI        extends PresetOption("ci")
    case object Strict    extends PresetOption("strict")
  }
}
