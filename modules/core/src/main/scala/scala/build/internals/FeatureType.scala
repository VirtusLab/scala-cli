package scala.build.internals

enum FeatureType(stringRepr: String) {
  override def toString: String = stringRepr

  case Option     extends FeatureType("option")
  case Directive  extends FeatureType("directive")
  case Subcommand extends FeatureType("sub-command")
  case ConfigKey  extends FeatureType("configuration key")
}

object FeatureType {
  private val ordering = Map(
    FeatureType.Subcommand -> 0,
    FeatureType.Option     -> 1,
    FeatureType.Directive  -> 2,
    FeatureType.ConfigKey  -> 3
  )

  given Ordering[FeatureType] = Ordering.by(ordering)
}
