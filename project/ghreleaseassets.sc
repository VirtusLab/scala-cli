// from https://github.com/coursier/coursier/blob/382250d4f26b4728400a0546088e27ca0f129e8b/scripts/shared/UploadGhRelease.sc

import $ivy.`com.softwaremill.sttp.client::core:2.0.0-RC6`
import $ivy.`com.lihaoyi::ujson:0.9.5`

import java.nio.ByteBuffer
import java.nio.charset.{MalformedInputException, StandardCharsets}
import java.nio.file.{Files, Path}
import java.util.zip.{ZipException, ZipFile}

import sttp.client.quick._

import scala.util.control.NonFatal

private def contentType(path: Path): String = {

  val isZipFile = {
    var zf: ZipFile = null
    try { zf = new ZipFile(path.toFile); true }
    catch { case _: ZipException => false }
    finally { if (zf != null) zf.close() }
  }

  lazy val isTextFile =
    try {
      StandardCharsets.UTF_8
        .newDecoder()
        .decode(ByteBuffer.wrap(Files.readAllBytes(path)))
      true
    }
    catch { case e: MalformedInputException => false }

  if (isZipFile) "application/zip"
  else if (isTextFile) "text/plain"
  else "application/octet-stream"
}

private def releaseId(
  ghOrg: String,
  ghProj: String,
  ghToken: String,
  tag: String
): Long = {
  val url = uri"https://api.github.com/repos/$ghOrg/$ghProj/releases?access_token=$ghToken"
  val resp = quickRequest.get(url).send()

  val json = ujson.read(resp.body)
  val releaseId =
    try {
      json
        .arr
        .find(_("tag_name").str == tag)
        .map(_("id").num.toLong)
        .getOrElse {
          val tags = json.arr.map(_("tag_name").str).toVector
          sys.error(s"Tag $tag not found (found tags: ${tags.mkString(", ")}")
        }
    } catch {
      case NonFatal(e) =>
        System.err.println(resp.body)
        throw e
    }

  System.err.println(s"Release id is $releaseId")

  releaseId
}

def currentAssets(
  releaseId: Long,
  ghOrg: String,
  ghProj: String,
  ghToken: String
): Map[String, Long] = {

  val resp = quickRequest
    .header("Accept", "application/vnd.github.v3+json")
    .header("Authorization", s"token $ghToken")
    .get(uri"https://api.github.com/repos/$ghOrg/$ghProj/releases/$releaseId/assets")
    .send()
  val json = ujson.read(resp.body)
  json
    .arr
    .iterator
    .map { obj =>
      obj("name").str -> obj("id").num.toLong
    }
    .toMap
}

/**
 * Uploads files as GitHub release assets.
 *
 * @param uploads List of local files / asset name to be uploaded
 * @param ghOrg GitHub organization of the release
 * @param ghProj GitHub project name of the release
 * @param ghToken GitHub token
 * @param tag Tag to upload assets to
 * @param dryRun Whether to run a dry run (printing the actions that would have been done, but not uploading anything)
 */
def upload(
  ghOrg: String,
  ghProj: String,
  ghToken: String,
  tag: String,
  dryRun: Boolean,
  overwrite: Boolean
)(
  uploads: (Path, String)*
): Unit = {

  val releaseId0 = releaseId(ghOrg, ghProj, ghToken, tag)

  val currentAssets0 = if (overwrite) currentAssets(releaseId0, ghOrg, ghProj, ghToken) else Map.empty[String, Long]

  for ((f0, name) <- uploads) {

    currentAssets0
      .get(name)
      .filter(_ => overwrite)
      .foreach { assetId =>
        val resp = quickRequest
          .header("Accept", "application/vnd.github.v3+json")
          .header("Authorization", s"token $ghToken")
          .delete(uri"https://api.github.com/repos/$ghOrg/$ghProj/releases/assets/$assetId")
          .send()
      }

    val uri = uri"https://uploads.github.com/repos/$ghOrg/$ghProj/releases/$releaseId0/assets?name=$name&access_token=$ghToken"
    val contentType0 = contentType(f0)
    System.err.println(s"Detected content type of $f0: $contentType0")
    if (dryRun)
      System.err.println(s"Would have uploaded $f0 as $name")
    else {
      System.err.println(s"Uploading $f0 as $name")
      quickRequest
        .body(f0)
        .header("Content-Type", contentType0)
        .post(uri)
        .send()
    }
  }
}
