package scala.build.options

final case class PackageOptions(
  packageTypeOpt: Option[PackageType] = None
) {

  def orElse(other: PackageOptions): PackageOptions =
    PackageOptions(
      packageTypeOpt = packageTypeOpt.orElse(other.packageTypeOpt)
    )

  def addHashData(update: String => Unit): Unit = {
    for (t <- packageTypeOpt)
      update("packageType=" + t.toString + "\n")
  }
}
