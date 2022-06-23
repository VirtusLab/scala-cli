package scala.cli.commands

import caseapp._
import coursier.paths.CoursierPaths

import scala.build.Os
import scala.cli.CurrentParams
import scala.cli.internal.BloopCache
object CacheImport extends ScalaCommand[CacheImportOptions] {
  override def group = "Main"

  override def names: List[List[String]] = List(
    List("cache", "import")
  )

  def run(options: CacheImportOptions, args: RemainingArgs): Unit = {
    maybePrintGroupHelp(options)
    CurrentParams.verbosity = options.logging.verbosity

    val cache = options.cachePath.getOrElse {
      System.err.println(
        "To specify path to the zipped cache file by scala-cli pass --cache-path or -c."
      )
      sys.exit(1)
    }
    val cachePath = os.Path(cache, Os.pwd)

    if (!os.exists(cachePath)) {
      System.err.println(
        s"""|Warning: No zipped cache file found.
            |To specify path to the zipped cache file by scala-cli pass --cache-path or -c""".stripMargin
      )
      sys.exit(1)
    }

    val csCachePath       = os.Path(CoursierPaths.cacheDirectory())
    val csArchPath        = os.Path(CoursierPaths.archiveCacheDirectory())
    val bloopCacheDirPath = os.Path(BloopCache.cacheDir())

    // tmp dir to unpack zipped cache file and then copy file to default cache directories
    val cacheDir = os.pwd / "tmp"
    os.makeDir.all(cacheDir)

    os.makeDir.all(csCachePath)
    os.makeDir.all(csArchPath)
    os.makeDir.all(bloopCacheDirPath)

    println("Starting importing cache")
    unZipArchiveOs(cachePath, cacheDir)
    unZipArchiveOs(CacheExport.coursierCachePath(cacheDir), csCachePath)
    unZipArchiveOs(CacheExport.coursierArchiveCachePath(cacheDir), csArchPath)
    unZipArchiveOs(CacheExport.bloopCachePath(cacheDir), bloopCacheDirPath)
    println("Successfully imported cache")

    // clean cache dir
    os.remove.all(cacheDir)
  }

  private def unZipArchiveOs(zipPath: os.Path, dest: os.Path) =
    os.proc("tar", "-xzvf", zipPath, "-C", dest, "--skip-old-files").call(
      cwd = os.pwd
    )

}
