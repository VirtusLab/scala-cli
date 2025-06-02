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

  implicit class MillTestInputs(inputs: TestInputs) {
    def withMillJvmOpts: TestInputs = inputs.add(os.rel / millJvmOptsFileName -> millJvmOptsContent)
  }

  protected val millOutputDir: os.RelPath = os.rel / "output-project"

  protected def millCommand(root: os.Path, args: String*): os.proc =
    os.proc(root / millOutputDir / millLauncher, args)
}
