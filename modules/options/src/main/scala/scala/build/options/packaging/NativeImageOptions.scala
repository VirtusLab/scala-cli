package scala.build.options.packaging

import scala.build.Positioned
import scala.build.internal.Constants
import scala.build.options.{ConfigMonoid, ShadowingSeq}

final case class NativeImageOptions(
  graalvmJvmId: Option[String] = None,
  graalvmJavaVersion: Option[Int] = None,
  graalvmVersion: Option[String] = None,
  graalvmArgs: Seq[Positioned[String]] = Nil
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
