package scala.cli.integration

trait LazyValTests {
  private val lazyValsLibOrg     = "test.lazyvals"
  private val lazyValsLibName    = "lazyvals-lib"
  private val lazyValsLibVersion = "0.1.0"
  private val lazyValsLibDep     = s"$lazyValsLibOrg::$lazyValsLibName:$lazyValsLibVersion"

  protected def publishLazyValsLib(
    scalaVersion: String,
    workspace: os.Path
  ): (String, os.Path) = {
    val libDir  = workspace / "lazyvals-lib"
    val repoDir = workspace / "test-repo"
    os.write(
      libDir / "LazyValsLib.scala",
      """package lazyvalslib
        |object LazyValsLib {
        |  lazy val greeting: String = "Hello"
        |}
        |""".stripMargin,
      createFolders = true
    )
    os.proc(
      TestUtil.cli,
      "--power",
      "publish",
      libDir,
      "--organization",
      lazyValsLibOrg,
      "--name",
      lazyValsLibName,
      "--project-version",
      lazyValsLibVersion,
      "--scala",
      scalaVersion,
      "--publish-repo",
      repoDir.toNIO.toUri.toASCIIString
    ).call(cwd = workspace, stdin = os.Inherit, stdout = os.Inherit)
    os.remove.all(libDir)
    (lazyValsLibDep, repoDir)
  }
}
