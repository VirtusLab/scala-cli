package scala.cli.errors

import scala.build.errors.BuildException
import scala.publish.Content
import scala.publish.fileset.Path
import scala.publish.upload.Upload

final class UploadError(errors: ::[(Path, Content, Upload.Error)]) extends BuildException(
      s"Error uploading ${errors.length} file(s):" +
        errors
          .map {
            case (path, _, err) =>
              System.lineSeparator() + s"  ${path.repr}: ${UploadError.describe(err)}"
          }
          .mkString
    )

object UploadError {

  /** The message of `err`, followed by its cause, if any.
    *
    * Errors such as [[Upload.Error.FileException]] (failing to write to a local repository) only
    * say `I/O error`, the actual reason being in their cause.
    */
  def describe(err: Upload.Error): String =
    Option(err.getCause).fold(err.getMessage)(cause => s"${err.getMessage} ($cause)")
}
