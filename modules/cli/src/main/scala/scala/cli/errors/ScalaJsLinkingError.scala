package scala.cli.errors

import scala.build.errors.BuildException

final class ScalaJsLinkingError(
  val expected: os.RelPath,
  val foundFiles: Seq[os.RelPath]
) extends BuildException(
      s"Error: $expected not found after Scala.JS linking " +
        (if (foundFiles.isEmpty) "(no files found)" else s"(found ${foundFiles.mkString(", ")})")
    )
