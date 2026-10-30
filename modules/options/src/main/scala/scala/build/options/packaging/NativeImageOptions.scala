package scala.build.options.packaging

import scala.build.internal.Constants
import scala.build.options.ConfigMonoid
import scala.build.{Os, Positioned}
import scala.util.Properties

final case class NativeImageOptions(
  graalvmJvmId: Option[String] = None,
  graalvmJavaVersion: Option[Int] = None,
  graalvmVersion: Option[String] = None,
  graalvmArgs: Seq[Positioned[String]] = Nil
) {
  lazy val jvmId: String =
    graalvmJvmId.getOrElse {
      val javaVersion = graalvmJavaVersion.getOrElse(Constants.defaultGraalVMJavaVersion)
      val version     = graalvmVersion.getOrElse(NativeImageOptions.defaultGraalVMVersion)
      s"graalvm-java$javaVersion:$version"
    }
}

object NativeImageOptions {
  implicit val monoid: ConfigMonoid[NativeImageOptions] = ConfigMonoid.derive

  def defaultGraalVMVersion: String =
    defaultGraalVMVersion(isMacosX64 = Properties.isMac && !Os.isArmArchitecture)

  /** There are no GraalVM CE builds for macOS x64 past [[Constants.defaultGraalVMMacosX64Version]]
    */
  def defaultGraalVMVersion(isMacosX64: Boolean): String =
    if isMacosX64 then Constants.defaultGraalVMMacosX64Version
    else Constants.defaultGraalVMVersion
}
