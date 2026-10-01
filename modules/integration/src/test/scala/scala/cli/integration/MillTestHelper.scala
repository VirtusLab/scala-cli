package scala.cli.integration

import scala.util.Properties

trait MillTestHelper {

  protected def millLauncher: os.RelPath =
    if (Properties.isWin) os.rel / "mill.bat"
    else os.rel / "mill"

  protected val millJvmOptsFileName: String = ".mill-jvm-opts"
  protected val millJvmOptsContent: String  = """-Xmx512m
                                                |-Xms128m
                                                |""".stripMargin

  protected val millDefaultProjectName = "project"

  protected val millOutputDir: os.RelPath = os.rel / "output-project"

  /** Writes the Mill JVM options file into an exported Mill project directory. Mill only reads
    * `.mill-jvm-opts` from its workspace root (the directory containing `build.mill`), so it has to
    * be written there rather than next to the exported sources.
    */
  protected def writeMillJvmOpts(millProjectDir: os.Path): Unit =
    os.write.over(millProjectDir / millJvmOptsFileName, millJvmOptsContent)

  /** Runs Mill in an exported project. Caps the Mill daemon's heap first, so that it doesn't
    * compete for memory with the processes Mill forks itself (i.e. `run` and test runners), which
    * on the CI have been observed to be killed under memory pressure.
    */
  protected def millCommand(root: os.Path, args: String*): os.proc = {
    val millProjectDir = root / millOutputDir
    if os.isDir(millProjectDir) then writeMillJvmOpts(millProjectDir)
    os.proc(millProjectDir / millLauncher, args)
  }
}
