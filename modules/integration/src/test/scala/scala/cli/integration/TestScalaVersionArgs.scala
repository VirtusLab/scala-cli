package scala.cli.integration

import scala.annotation.unused

trait TestScalaVersionArgs extends ScalaCliSuite { this: TestScalaVersion =>
  override def group: ScalaCliSuite.TestGroup =
    if (actualScalaVersion.startsWith("2.12.")) ScalaCliSuite.TestGroup.Third
    else if (actualScalaVersion.startsWith("2.13.")) ScalaCliSuite.TestGroup.Second
    else if (actualScalaVersion.startsWith(Constants.scala3LtsPrefix))
      ScalaCliSuite.TestGroup.Fourth
    else if (actualScalaVersion.startsWith(Constants.scala3LegacyLtsPrefix))
      ScalaCliSuite.TestGroup.Sixth
    else if (actualScalaVersion.startsWith(Constants.scala3NextRc))
      ScalaCliSuite.TestGroup.Fifth
    else ScalaCliSuite.TestGroup.First

  lazy val scalaVersionArgs: Seq[String] = scalaVersionOpt match {
    case None     => Nil
    case Some(sv) => Seq("--scala", sv)
  }

  lazy val actualScalaVersion: String = scalaVersionOpt.getOrElse(Constants.defaultScala)

  def isScala38OrNewer: Boolean = isScala38OrNewer(actualScalaVersion)

  def isScala38OrNewer(scalaVersion: String): Boolean =
    Constants.scala38Versions
      .map(_.coursierVersion)
      .exists(_ <= scalaVersion.coursierVersion)

  def isScala39OrNewer: Boolean =
    Constants.scala39Versions
      .map(_.coursierVersion)
      .exists(_ <= actualScalaVersion.coursierVersion)

  /** Of note, Scala 3.10 and newer no longer support Scala Native 0.4 */
  def isScala310OrNewer: Boolean =
    actualScalaVersion.coursierVersion >= "3.10.0-RC1".coursierVersion

  /** Of note, the published scalalib_native0.5_3:3.10.1-RC1+0.5.12 artifact contains stub NIR
    * files, which makes Scala Native linking fail. TODO: re-enable the Scala Native tests gated
    * with this once a fixed artifact is published
    */
  def isScala3101OrNewer: Boolean =
    actualScalaVersion.coursierVersion >= "3.10.1-RC2".coursierVersion

  /** Scaladoc only passes its own compiler context (and thus `-scalajs`) to the TASTy inspector
    * since Scala 3.6.3, which is what makes reading Scala.js TASTy work.
    */
  def isScala363OrNewer: Boolean =
    actualScalaVersion.coursierVersion >= "3.6.3".coursierVersion

  def hasLatestRunnerModule: Boolean = actualScalaVersion.coursierVersion > "3.3.0".coursierVersion

  def retrieveScalaVersionCode: String = retrieveScalaVersionCode(actualScalaVersion)

  def retrieveScalaVersionCode(scalaVersion: String): String =
    if scalaVersion.startsWith("2.") || isScala38OrNewer(scalaVersion) then
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
trait Test39Lts extends TestScalaVersion { this: TestScalaVersionArgs =>
  override lazy val scalaVersionOpt: Option[String] = Some(Constants.scala3Lts)
}
trait Test33Lts extends TestScalaVersion { this: TestScalaVersionArgs =>
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
