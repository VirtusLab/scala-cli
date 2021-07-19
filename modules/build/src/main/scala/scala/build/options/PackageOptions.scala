package scala.build.options

final case class PackageOptions(
  packageTypeOpt: Option[PackageType] = None
) {

  def orElse(other: PackageOptions): PackageOptions =
    PackageOptions(
      packageTypeOpt = packageTypeOpt.orElse(other.packageTypeOpt)
    )
}

object PackageOptions {
  implicit val hasHashData: HasHashData[PackageOptions] = HasHashData.derive
}
