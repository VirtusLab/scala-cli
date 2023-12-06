package scala.build.errors

final class UnrecognizedJsOptModeError(
  mode: String,
  aliasesForFullLink: Seq[String],
  aliasesForFastLink: Seq[String]
) extends BuildException(
      s"""Unrecognized JS optimization mode: $mode.
         |Available options:
         |- for fastLinkJs: ${aliasesForFastLink.mkString(", ")}
         |- for fullLinkJs: ${aliasesForFullLink.mkString(", ")}""".stripMargin
    )
