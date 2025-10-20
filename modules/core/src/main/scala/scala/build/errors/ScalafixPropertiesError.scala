package scala.build.errors

final class ScalafixPropertiesError(
  path: os.Path,
  cause: Option[Throwable] = None
) extends BuildException(
      message = {
        val causeMessage = cause.map(c => s": ${c.getMessage}").getOrElse("")
        s"Failed to load Scalafix properties at $path$causeMessage"
      },
      cause = cause.orNull
    )
