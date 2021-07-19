package scala.build.options

sealed abstract class PackageType extends Product with Serializable

object PackageType {
  sealed abstract class NativePackagerType extends PackageType

  case object Bootstrap extends PackageType
  case object LibraryJar extends PackageType
  case object Assembly extends PackageType
  case object Js extends PackageType
  case object Native extends PackageType
  case object Debian extends NativePackagerType
  case object Dmg extends NativePackagerType
  case object Pkg extends NativePackagerType
  case object Rpm extends NativePackagerType
  case object Msi extends NativePackagerType
}
