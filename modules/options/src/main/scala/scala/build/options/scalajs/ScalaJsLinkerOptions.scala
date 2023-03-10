package scala.build.options.scalajs

import scala.build.internal.{Constants, FetchExternalBinary}
import scala.build.options.ConfigMonoid

final case class ScalaJsLinkerOptions(
  javaArgs: Seq[String] = Nil,
  /** If right, use JVM, if left, use the value as architecture */
  useJvm: Option[Either[String, Unit]] = None,
  scalaJsVersion: Option[String] = None,
  scalaJsCliVersion: Option[String] = None,
  linkerPath: Option[os.Path] = None
) {
  def finalScalaJsCliVersion = scalaJsCliVersion.orElse(scalaJsVersion).getOrElse {
    Constants.scalaJsVersion
  }

  /** If right, use JVM, if left, use the value as architecture */
  lazy val finalUseJvm: Either[String, Unit] = useJvm.getOrElse {
    FetchExternalBinary.maybePlatformSuffix() match {
      case Left(_) =>
        // FIXME Log error?
        Right(())
      case Right(osArch) =>
        Left(osArch)
    }
  }
}

object ScalaJsLinkerOptions {
  implicit val monoid: ConfigMonoid[ScalaJsLinkerOptions] = ConfigMonoid.derive
}
