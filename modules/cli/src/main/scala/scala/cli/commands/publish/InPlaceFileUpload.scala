package scala.cli.commands.publish

import coursier.core.Authentication

import java.nio.file.Path

import scala.publish.upload.logger.UploadLogger
import scala.publish.upload.{FileUpload, Upload}

/** Writes files to a local repository like [[FileUpload]], but overwrites existing files in place
  *
  * Since scala-publish 1.1.0, [[FileUpload]] replaces existing files (deleting, then re-creating
  * them) when copying file content. On Windows, that fails for files opened by other processes,
  * like JARs on the class path of an application run with `--watch`. Overwriting them in place
  * works, like it did with scala-publish 1.0.0.
  */
final case class InPlaceFileUpload(base: Path) extends Upload {
  private val underlying = FileUpload(base)

  // The other upload overloads end up here, reading the content in memory
  def upload(
    url: String,
    authentication: Option[Authentication],
    content: Array[Byte],
    logger: UploadLogger,
    loggingIdOpt: Option[Object]
  ): Option[Upload.Error] =
    underlying.upload(url, authentication, content, logger, loggingIdOpt)
}
