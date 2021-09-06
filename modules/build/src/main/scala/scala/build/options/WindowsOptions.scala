package scala.build.options

final case class WindowsOptions(
  licensePath: Option[os.Path] = None,
  productName: Option[String] = None,
  exitDialog: Option[String] = None,
  suppressValidation: Option[Boolean] = None,
  extraConfig: List[String] = Nil,
  is64Bits: Option[Boolean] = None,
  installerVersion: Option[String] = None
)

object WindowsOptions {
  implicit val hasHashData: HasHashData[WindowsOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[WindowsOptions]     = ConfigMonoid.derive
}
