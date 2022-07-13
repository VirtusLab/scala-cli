package scala.build.options

import scala.build.options.scalajs.ScalaJsLinkerOptions

final case class PostBuildOptions(
  packageOptions: PackageOptions = PackageOptions(),
  replOptions: ReplOptions = ReplOptions(),
  publishOptions: PublishOptions = PublishOptions(),
  scalaJsLinkerOptions: ScalaJsLinkerOptions = ScalaJsLinkerOptions(),
  runWithManifest: Option[Boolean] = None
)

object PostBuildOptions {
  /* Using HasHashData.nop here (PostBuildOptions values are not used during compilation) */
  implicit val hasHashData: HasHashData[PostBuildOptions] = HasHashData.nop
  implicit val monoid: ConfigMonoid[PostBuildOptions]     = ConfigMonoid.derive
}
