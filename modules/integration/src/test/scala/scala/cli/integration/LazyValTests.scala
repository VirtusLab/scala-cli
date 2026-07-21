package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.File

trait LazyValTests:
  private val lazyValsLibOrg     = "test.lazyvals"
  private val lazyValsLibName    = "lazyvals-lib"
  private val lazyValsLibVersion = "0.1.0"
  protected val lazyValsLibDep   = s"$lazyValsLibOrg::$lazyValsLibName:$lazyValsLibVersion"

  protected val slothNoOpWarnPrefix: String = "Sloth patching is not applicable to"
  protected val slothCacheSegment: String   = s"${File.separator}sloth${File.separator}"
  protected val slothOptions: Seq[String]   =
    Seq("--sloth", "--suppress-experimental-feature-warning")
  protected val slothAgentOptions: Seq[String] =
    Seq("--sloth-agent", "--suppress-experimental-feature-warning")

  protected def expectScaladocClasspathContains(output: String, fragment: String): Unit =
    val marker       = "dotty.tools.scaladoc.Main -classpath "
    val classpathOpt = output.split(marker).lift(1).map(_.takeWhile(c => c != ' ' && c != '\n'))
    expect(classpathOpt.exists(_.contains(fragment)))

  protected def publishLazyValsLib(
    scalaVersion: String,
    workspace: os.Path,
    buildJvm: Option[String] = None
  ): (String, os.Path) =
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
      buildJvm.toSeq.flatMap(j => Seq("--jvm", j)),
      "--publish-repo",
      repoDir.toNIO.toUri.toASCIIString
    ).call(cwd = workspace, stdin = os.Inherit, stdout = os.Inherit)
    os.remove.all(libDir)
    (lazyValsLibDep, repoDir)
