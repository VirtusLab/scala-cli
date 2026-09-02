package scala.cli.integration

import scala.annotation.unused

trait TestScalaVersionArgs extends ScalaCliSuite { this: TestScalaVersion =>
  override def group: ScalaCliSuite.TestGroup =
    if (actualScalaVersion.startsWith("2.12.")) ScalaCliSuite.TestGroup.Third
    else if (actualScalaVersion.startsWith("2.13.")) ScalaCliSuite.TestGroup.Second
    else if (actualScalaVersion.startsWith(Constants.scala3LegacyLtsPrefix))
      ScalaCliSuite.TestGroup.Fourth
    else if (actualScalaVersion.startsWith(Constants.scala3NextRc))
      ScalaCliSuite.TestGroup.Fifth
    else ScalaCliSuite.TestGroup.First

  lazy val scalaVersionArgs: Seq[String] = scalaVersionOpt match {
    case None     => Nil
    case Some(sv) => Seq("--scala", sv)
  }

  lazy val actualScalaVersion: String = scalaVersionOpt.getOrElse(Constants.defaultScala)

  def isScala38OrNewer: Boolean =
    Constants.scala38Versions
      .map(_.coursierVersion)
      .exists(_ <= actualScalaVersion.coursierVersion)

  def isScala39OrNewer: Boolean =
    actualScalaVersion.coursierVersion >= "3.9.0-RC1".coursierVersion

  /** Of note, Scala 3.10 and newer no longer support Scala Native 0.4 */
  def isScala310OrNewer: Boolean =
    actualScalaVersion.coursierVersion >= "3.10.0-RC1".coursierVersion

  /** Scaladoc only passes its own compiler context (and thus `-scalajs`) to the TASTy inspector
    * since Scala 3.6.3, which is what makes reading Scala.js TASTy work.
    */
  def isScala363OrNewer: Boolean =
    actualScalaVersion.coursierVersion >= "3.6.3".coursierVersion

  def hasLatestRunnerModule: Boolean = actualScalaVersion.coursierVersion > "3.3.0".coursierVersion

  def retrieveScalaVersionCode: String =
    if actualScalaVersion.startsWith("2.") || isScala38OrNewer then
      "scala.util.Properties.versionNumberString"
    else "dotty.tools.dotc.config.Properties.simpleVersionString"
}

sealed trait TestScalaVersion { this: TestScalaVersionArgs =>
  def scalaVersionOpt: Option[String]
}
trait Test213 extends TestScalaVersion { this: TestScalaVersionArgs =>
  override lazy val scalaVersionOpt: Option[String] = Some(Constants.scala213)
}
trait Test212 extends TestScalaVersion { this: TestScalaVersionArgs =>
  override lazy val scalaVersionOpt: Option[String] = Some(Constants.scala212)
}
trait Test3Lts extends TestScalaVersion { this: TestScalaVersionArgs =>
  override lazy val scalaVersionOpt: Option[String] = Some(Constants.scala3LegacyLts)
}
trait Test3NextRc extends TestScalaVersion { this: TestScalaVersionArgs =>
  override lazy val scalaVersionOpt: Option[String] = Some(Constants.scala3NextRc)
}
@unused // TestDefault should normally be mixed in instead
trait Test3Next extends TestScalaVersion { this: TestScalaVersionArgs =>
  override lazy val scalaVersionOpt: Option[String] = Some(Constants.scala3Next)
}
trait TestDefault extends TestScalaVersion { this: TestScalaVersionArgs =>
  // this effectively should make the launcher default to 3.next
  override lazy val scalaVersionOpt: Option[String] = None
}
