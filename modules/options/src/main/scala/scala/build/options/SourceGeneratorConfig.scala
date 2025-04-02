package scala.build.options

final case class SourceGeneratorConfig(
  inputDir: Option[os.Path] = None,
  outputDir: Option[os.Path] = None,
  unmanaged: List[os.Path] = Nil,
  glob: List[String] = Nil,
  command: List[String] = Nil
)

object SourceGeneratorConfig {
  implicit val monoid: ConfigMonoid[SourceGeneratorConfig] = ConfigMonoid.derive
}
