package scala.build.internals

enum FeatureType(stringRepr: String) {
  override def toString: String = stringRepr

  case Option     extends FeatureType("option")
  case Directive  extends FeatureType("directive")
  case Subcommand extends FeatureType("sub-command")
  case ConfigKey  extends FeatureType("configuration key")
}
