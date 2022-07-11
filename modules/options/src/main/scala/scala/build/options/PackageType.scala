package scala.build.options

sealed abstract class PackageType extends Product with Serializable {
  def runnable: Option[Boolean] = Some(false)
  def sourceBased: Boolean      = false
}

object PackageType {
  sealed abstract class NativePackagerType extends PackageType

  case object Bootstrap extends PackageType {
    override def runnable = Some(true)
  }
  case object LibraryJar extends PackageType
  case object SourceJar extends PackageType {
    override def sourceBased = true
  }
  case object DocJar extends PackageType
  final case class Assembly(addPreamble: Boolean) extends PackageType {
    override def runnable = Some(addPreamble)
  }
  case object Js extends PackageType
  case object Native extends PackageType {
    override def runnable = Some(true)
  }
  case object Docker extends PackageType {
    override def runnable = None
  }
  case object GraalVMNativeImage extends PackageType {
    override def runnable = Some(true)
  }
  case object Debian extends NativePackagerType
  case object Dmg    extends NativePackagerType
  case object Pkg    extends NativePackagerType
  case object Rpm    extends NativePackagerType
  case object Msi    extends NativePackagerType

  val mapping = Seq(
    "assembly"     -> Assembly(true),
    "raw-assembly" -> Assembly(false),
    "bootstrap"    -> Bootstrap,
    "library"      -> LibraryJar,
    "source"       -> SourceJar,
    "doc"          -> DocJar,
    "js"           -> Js,
    "native"       -> Native,
    "docker"       -> Docker,
    "graalvm"      -> GraalVMNativeImage,
    "deb"          -> Debian,
    "dmg"          -> Dmg,
    "pkg"          -> Pkg,
    "rpm"          -> Rpm,
    "msi"          -> Msi
  )
  private lazy val map = mapping.toMap
  def parse(input: String): Option[PackageType] =
    map.get(input)
}
