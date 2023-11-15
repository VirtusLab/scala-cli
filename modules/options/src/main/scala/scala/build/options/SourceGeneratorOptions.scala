package scala.build.options

final case class SourceGeneratorOptions(
  useBuildInfo: Option[Boolean] = None,
  projectVersion: Option[String] = None,
  computeVersion: Option[ComputeVersion] = None
)

object SourceGeneratorOptions {
  /* Using HasHashData.nop here (SourceGeneratorOptions values are not used during compilation) */
  implicit val hasHashData: HasHashData[SourceGeneratorOptions] = HasHashData.nop
  implicit val monoid: ConfigMonoid[SourceGeneratorOptions]     = ConfigMonoid.derive
}
