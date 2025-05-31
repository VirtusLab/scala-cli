package scala.build.errors

import java.net.URI

import scala.build.Position

final class UsingFileFromUriError(uri: URI, positions: Seq[Position], description: String)
    extends BuildException(
      message = s"Error using file from $uri - $description",
      positions = positions
    )
