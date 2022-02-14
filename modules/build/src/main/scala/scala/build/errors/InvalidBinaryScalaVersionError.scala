package scala.build.errors

final class InvalidBinaryScalaVersionError(
  val invalidBinaryVersion: String,
  val latestSupportedStableVersions: Seq[String]
) extends ScalaVersionError(s"Cannot find matching Scala version for '$invalidBinaryVersion'\n" +
  s"You can only choose one of the 3.x, 2.13.x, and 2.12.x. versions \n" +
  s"The latest supported stable versions are ${latestSupportedStableVersions.mkString(", ")}. \n" +
  s"In addition, you can request the latest Scala 2 and Scala 3 nightly versions by passing 2.nightly, and 3.nightly arguments respectively.\n" +
  s"For requesting a specific Scala 2 or Scala 3 nightly version, please specify the full version of the nightly without the repository argument.")
