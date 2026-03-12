package scala.build.options

final case class WatchOptions(
  extraWatchPaths: Seq[os.Path] = Nil
)

object WatchOptions {
  implicit val hasHashData: HasHashData[WatchOptions] = HasHashData.nop
  implicit val monoid: ConfigMonoid[WatchOptions]     = ConfigMonoid.derive
}
