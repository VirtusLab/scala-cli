package scala.build.options

case class SemanticDbOptions(
  generateSemanticDbs: Option[Boolean] = None,
  semanticDbTargetRoot: Option[os.Path] = None,
  semanticDbSourceRoot: Option[os.Path] = None
)

object SemanticDbOptions {
  implicit val hasHashData: HasHashData[SemanticDbOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[SemanticDbOptions]     = ConfigMonoid.derive
}
