package scala.build.options.packaging

import scala.build.internal.Constants
import scala.build.options.ConfigMonoid

final case class NativeImageOptions(
  graalvmJvmId: Option[String] = None,
  graalvmJavaVersion: Option[Int] = None,
  graalvmVersion: Option[String] = None
) {
  lazy val jvmId: String =
    graalvmJvmId.getOrElse {
      val javaVersion = graalvmJavaVersion.getOrElse(Constants.defaultGraalVMJavaVersion)
      val version     = graalvmVersion.getOrElse(Constants.defaultGraalVMVersion)
      s"graalvm-java$javaVersion:$version"
    }
}

object NativeImageOptions {
  implicit val monoid: ConfigMonoid[NativeImageOptions] = ConfigMonoid.derive
}
