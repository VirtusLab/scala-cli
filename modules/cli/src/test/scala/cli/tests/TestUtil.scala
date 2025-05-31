package cli.tests

import coursier.cache.{ArtifactError, FileCache}
import coursier.util.{Artifact, Task}

import java.io.File

import scala.concurrent.duration.DurationInt
import scala.util.control.NonFatal

object TestUtil {
  def downloadFile(url: String): Either[ArtifactError, Array[Byte]] = {
    val artifact = Artifact(url).withChanging(true)
    val cache    = FileCache()

    val file: Either[ArtifactError, File] = cache.logger.use {
      try cache.withTtl(0.seconds).file(artifact).run.unsafeRun()(cache.ec)
      catch {
        case NonFatal(e) => throw new Exception(e)
      }
    }

    file.map(f => os.read.bytes(os.Path(f, os.pwd)))
  }
  val isCI: Boolean = System.getenv("CI") != null
}
