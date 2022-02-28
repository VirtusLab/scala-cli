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
  case object Assembly extends PackageType {
    override def runnable = Some(true)
  }
  case object Js extends PackageType
  case object Native extends PackageType {
    override def runnable = Some(true)
  }
  case object Docker extends PackageType {
    override def runnable = None
  }
  case object Debian extends NativePackagerType
  case object Dmg    extends NativePackagerType
  case object Pkg    extends NativePackagerType
  case object Rpm    extends NativePackagerType
  case object Msi    extends NativePackagerType
}
