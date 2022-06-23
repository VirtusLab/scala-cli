package scala.cli.commands

import caseapp._
import coursier.cache.FileCache
import coursier.paths.CoursierPaths
import coursier.util.Task

import scala.build.internal.CsLoggerUtil.CsJavaHomeExtensions
import scala.build.internal.{Constants, OsLibc}
import scala.build.options.BuildOptions
import scala.build.{Inputs, Logger, Os}
import scala.cli.CurrentParams
import scala.cli.commands.util.SharedOptionsUtil._
import scala.cli.internal.BloopCache
import scala.util.control.NonFatal

object CacheExport extends ScalaCommand[CacheExportOptions] {
  override def group = "Main"

  override def names: List[List[String]] = List(
    List("cache", "export")
  )

  def run(options: CacheExportOptions, args: RemainingArgs): Unit = {
    maybePrintGroupHelp(options)
    CurrentParams.verbosity = options.runOptions.shared.logging.verbosity

    val inputs = options.runOptions.shared.inputsOrExit(args)
    CurrentParams.workspaceOpt = Some(inputs.workspace)
    val logger                 = options.runOptions.shared.logger
    val cache: FileCache[Task] = options.runOptions.shared.coursierCache

    val buildOptions = options.runOptions.shared.buildOptions()
    val dest         = options.output.getOrElse("cache.tar.gz")
    val destPath     = os.Path(dest, Os.pwd)

    // clean workspace .scala-build
    cleanWorkspace(logger, inputs)
    // restart bloop to force download bloop class path
    exitBloop(options.runOptions.shared, args)
    // run sources to force download all dependencies
    downloadAppDependencies(options.runOptions, args)
    // force to download java to arch cache dir
    downloadJava(cache, buildOptions)

    val csCachePath       = os.Path(CoursierPaths.cacheDirectory())
    val csArchPath        = os.Path(CoursierPaths.archiveCacheDirectory())
    val bloopCacheDirPath = os.Path(BloopCache.cacheDir())

    val tmpCacheDir = os.temp.dir(prefix = "scala-cli-cache")

    println(s"Starting exporting caches to $destPath")
    println(s"Exporting coursier cache")
    compressOs(coursierCachePath(tmpCacheDir), csCachePath)
    println(s"Exported coursier cache")

    println(s"Exporting coursier archive cache")
    compressOs(coursierArchiveCachePath(tmpCacheDir), csArchPath)
    println(s"Exported coursier archive cache")

    println(s"Exporting bloop cache")
    compressOs(bloopCachePath(tmpCacheDir), bloopCacheDirPath)
    println(s"Exported bloop cache")

    compressOs(destPath, tmpCacheDir)
    println(s"Exported caches to $destPath")

    os.remove.all(tmpCacheDir)
  }

  private def cleanWorkspace(logger: Logger, inputs: Inputs) = {
    val workDir = inputs.workspace / Constants.workspaceDirName
    if (os.isDir(workDir)) {
      logger.message(s"Removing $workDir")
      os.remove.all(workDir)
    }
    else
      logger.message(s"$workDir is not a directory, ignoring it.")
  }

  private def downloadAppDependencies(runOptions: RunOptions, args: RemainingArgs) = Run.run(
    runOptions,
    args.remaining,
    args.unparsed,
    () => Inputs.default(),
    allowTerminate = false
  )

  private def exitBloop(shared: SharedOptions, args: RemainingArgs) = {
    val bloopExitOptions = BloopExitOptions(
      logging = shared.logging,
      compilationServer = shared.compilationServer,
      directories = shared.directories,
      coursier = shared.coursier
    )

    BloopExit.run(bloopExitOptions, args)
  }

  private def downloadJava(cache: FileCache[Task], buildOptions: BuildOptions) = {
    val jvmId            = OsLibc.baseDefaultJvm(OsLibc.jvmIndexOs, OsLibc.defaultJvmVersion)
    val javaHomeManager0 = buildOptions.javaHomeManager.withMessage(s"Downloading JVM $jvmId")

    implicit val ec = cache.ec
    cache.logger.use {
      try javaHomeManager0.get(jvmId).unsafeRun()
      catch {
        case NonFatal(e) => throw new Exception(e)
      }
    }
  }

  def compressOs(zipFilepath: os.Path, dirPath: os.Path) =
    os.proc("tar", "czf", zipFilepath, "-C", dirPath, ".").call(
      cwd = os.pwd
    )

  def coursierCachePath(cacheDir: os.Path): os.Path        = cacheDir / "csCache.tar.gz"
  def coursierArchiveCachePath(cacheDir: os.Path): os.Path = cacheDir / "csArch.tar.gz"
  def bloopCachePath(cacheDir: os.Path): os.Path           = cacheDir / "bloopCache.tar.gz"

}
