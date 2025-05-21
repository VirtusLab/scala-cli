package scala.build.errors

import java.net.URI

import scala.build.Position

final class UsingFileFromUriError(uri: URI, positions: Seq[Position], cause: Throwable)
    extends BuildException(
      message = s"Error using file from $uri - ${cause.getLocalizedMessage}",
      positions = positions,
      cause = cause
    )
