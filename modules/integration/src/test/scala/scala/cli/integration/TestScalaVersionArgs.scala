package scala.cli.integration

import scala.annotation.unused

trait TestScalaVersionArgs extends ScalaCliSuite { _: TestScalaVersion =>
  override def group: ScalaCliSuite.TestGroup =
    if (actualScalaVersion.startsWith("2.12.")) ScalaCliSuite.TestGroup.Third
    else if (actualScalaVersion.startsWith("2.13.")) ScalaCliSuite.TestGroup.Second
    else if (actualScalaVersion.startsWith(Constants.scala3LtsPrefix))
      ScalaCliSuite.TestGroup.Fourth
    else ScalaCliSuite.TestGroup.First

  lazy val scalaVersionArgs: Seq[String] = scalaVersionOpt match {
    case None     => Nil
    case Some(sv) => Seq("--scala", sv)
  }

  lazy val actualScalaVersion: String = scalaVersionOpt.getOrElse(Constants.defaultScala)
}

sealed trait TestScalaVersion { _: TestScalaVersionArgs =>
  def scalaVersionOpt: Option[String]
}
trait Test213 extends TestScalaVersion { _: TestScalaVersionArgs =>
  override lazy val scalaVersionOpt: Option[String] = Some(Constants.scala213)
}
trait Test212 extends TestScalaVersion { _: TestScalaVersionArgs =>
  override lazy val scalaVersionOpt: Option[String] = Some(Constants.scala212)
}
trait Test3Lts extends TestScalaVersion { _: TestScalaVersionArgs =>
  override lazy val scalaVersionOpt: Option[String] = Some(Constants.scala3Lts)
}
@unused // TestDefault should normally be mixed in instead
trait Test3Next extends TestScalaVersion { _: TestScalaVersionArgs =>
  override lazy val scalaVersionOpt: Option[String] = Some(Constants.scala3Next)
}
trait TestDefault extends TestScalaVersion { _: TestScalaVersionArgs =>
  // this effectively should make the launcher default to 3.next
  override lazy val scalaVersionOpt: Option[String] = None
}
