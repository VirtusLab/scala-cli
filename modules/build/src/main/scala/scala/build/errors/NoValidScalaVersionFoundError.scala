package scala.build.errors

final class NoValidScalaVersionFoundError(
  val foundVersions: Seq[String]
) extends ScalaVersionError(
      s"Cannot find valid Scala version among ${foundVersions.mkString(", ")}"
    )
