// from https://github.com/coursier/coursier/blob/382250d4f26b4728400a0546088e27ca0f129e8b/scripts/shared/UploadGhRelease.sc

import $ivy.`com.softwaremill.sttp.client::core:2.0.0-RC6`
import $ivy.`com.lihaoyi::ujson:0.9.5`

import $file.publish, publish.{ghOrg, ghName}
import $file.settings, settings.{platformExtension, platformSuffix}

import java.io._
import java.nio.ByteBuffer
import java.nio.charset.{MalformedInputException, StandardCharsets}
import java.nio.file.{Files, Path}
import java.util.zip.{ZipException, ZipFile}

import sttp.client.quick._

import scala.util.control.NonFatal
import scala.util.Properties

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

def readInto(is: InputStream, os: OutputStream): Unit = {
  val buf = Array.ofDim[Byte](1024 * 1024)
  var read = -1
  while ({
    read = is.read(buf)
    read >= 0
  }) os.write(buf, 0, read)
}

def writeInZip(name: String, file: os.Path, zip: os.Path): Unit = {
  import java.nio.file.attribute.FileTime
  import java.util.zip._

  os.makeDir.all(zip / os.up)

  var fis: InputStream = null
  var fos: FileOutputStream = null
  var zos: ZipOutputStream = null

  try {
    fis = os.read.inputStream(file)
    fos = new FileOutputStream(zip.toIO)
    zos = new ZipOutputStream(new BufferedOutputStream(fos))

    val ent = new ZipEntry(name)
    ent.setLastModifiedTime(FileTime.fromMillis(os.mtime(file)))
    ent.setSize(os.size(file))
    zos.putNextEntry(ent)
    readInto(fis, zos)
    zos.closeEntry()

    zos.finish()
  } finally {
    if (zos != null) zos.close()
    if (fos != null) fos.close()
    if (fis != null) fis.close()
  }
}

def copyLauncher(
  nativeLauncher: os.Path,
  directory: String
): Unit = {
  val path = os.Path(directory, os.pwd)
  val name = s"scala-$platformSuffix$platformExtension"
  if (Properties.isWin)
    writeInZip(s"scala$platformExtension", nativeLauncher, path / s"scala-$platformSuffix.zip")
  else {
    val dest = path / name
    os.copy(nativeLauncher, dest, createFolders = true, replaceExisting = true)
    os.proc("gzip", "-v", dest.toString).call(
      stdin = os.Inherit,
      stdout = os.Inherit,
      stderr = os.Inherit
    )
  }
}

def uploadLaunchers(
  version: String,
  directory: String
): Unit = {
  val path = os.Path(directory, os.pwd)
  val launchers = os.list(path).filter(os.isFile(_)).map { path =>
    path.toNIO -> path.last
  }
  val ghToken = Option(System.getenv("UPLOAD_GH_TOKEN")).getOrElse {
    sys.error("UPLOAD_GH_TOKEN not set")
  }
  val (tag, overwriteAssets) =
    if (version.endsWith("-SNAPSHOT")) ("latest", true)
    else ("v" + version, false)
  upload(ghOrg, ghName, ghToken, tag, dryRun = false, overwrite = overwriteAssets)(launchers: _*)
}
