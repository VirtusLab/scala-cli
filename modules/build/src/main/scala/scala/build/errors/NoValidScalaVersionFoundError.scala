package scala.build.errors

final class NoValidScalaVersionFoundError(
  val foundVersions: Seq[String],
  val latestSupportedStableVersions: Seq[String]
) extends ScalaVersionError(
      s"Cannot find a valid matching Scala version among ${foundVersions.mkString(", ")}\n" +
        s"You can only choose one of the 3.x, 2.13.x, and 2.12.x. \n" +
        s"The latest supported stable versions are ${latestSupportedStableVersions.mkString(", ")}. \n" +
        s"In addition, you can request the latest Scala 2 and Scala 3 nightly versions by passing 2.nightly, and 3.nightly arguments respectively.\n" +
        "For requesting a specific Scala 2 or Scala 3 nightly version, please specify the full version of the nightly without the repository argument."
    )
