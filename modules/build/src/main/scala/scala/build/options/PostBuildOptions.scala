package scala.build.options

final case class PostBuildOptions(
  packageOptions: PackageOptions = PackageOptions(),
  replOptions: ReplOptions = ReplOptions()
)

object PostBuildOptions {
  /* Using HasHashData.nop here (PostBuildOptions values are not used during compilation) */
  implicit val hasHashData: HasHashData[PostBuildOptions] = HasHashData.nop
  implicit val monoid: ConfigMonoid[PostBuildOptions]     = ConfigMonoid.derive
}
