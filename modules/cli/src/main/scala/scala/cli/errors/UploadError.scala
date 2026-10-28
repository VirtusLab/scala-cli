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
              System.lineSeparator() + s"  ${path.repr}: ${err.getMessage}"
          }
          .mkString
    )
