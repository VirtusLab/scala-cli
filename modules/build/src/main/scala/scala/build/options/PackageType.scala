package scala.build.options

sealed abstract class PackageType extends Product with Serializable {
  def runnable: Boolean = false
}

object PackageType {
  sealed abstract class NativePackagerType extends PackageType

  case object Bootstrap extends PackageType {
    override def runnable = true
  }
  case object LibraryJar extends PackageType
  case object Assembly extends PackageType {
    override def runnable = true
  }
  case object Js extends PackageType
  case object Native extends PackageType {
    override def runnable = true
  }
  case object Debian extends NativePackagerType
  case object Dmg extends NativePackagerType
  case object Pkg extends NativePackagerType
  case object Rpm extends NativePackagerType
  case object Msi extends NativePackagerType
}
