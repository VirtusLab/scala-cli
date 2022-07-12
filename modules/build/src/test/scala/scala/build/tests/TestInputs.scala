package scala.build.tests

import java.nio.charset.StandardCharsets
import scala.build.blooprifle.BloopRifleConfig
import scala.build.{Build, BuildThreads, Directories, Inputs}
import scala.build.compiler.{BloopCompilerMaker, SimpleScalaCompilerMaker}
import scala.build.errors.BuildException
import scala.build.options.BuildOptions
import scala.util.control.NonFatal
import scala.util.Try

final case class TestInputs(
  files: Seq[(os.RelPath, String)],
  inputArgs: Seq[String]
) {
  def withInputs[T](f: (os.Path, Inputs) => T): T =
    withCustomInputs(false, None)(f)

  def fromRoot[T](f: os.Path => T): T =
    TestInputs.withTmpDir("scala-cli-tests-") { tmpDir =>
      for ((relPath, content) <- files) {
        val path = tmpDir / relPath
        os.write(path, content.getBytes(StandardCharsets.UTF_8), createFolders = true)
      }

      f(tmpDir)
    }

  def withCustomInputs[T](
    viaDirectory: Boolean,
    forcedWorkspaceOpt: Option[os.FilePath]
  )(
    f: (os.Path, Inputs) => T
  ): T =
    fromRoot { tmpDir =>
      val inputArgs0 =
        if (viaDirectory) Seq(tmpDir.toString)
        else if (inputArgs.isEmpty) files.map(_._1.toString)
        else inputArgs
      val res = Inputs(
        inputArgs0,
        tmpDir,
        Directories.under(tmpDir / ".data"),
        forcedWorkspace = forcedWorkspaceOpt.map(_.resolveFrom(tmpDir))
      )
      res match {
        case Left(err)     => throw new Exception(err)
        case Right(inputs) => f(tmpDir, inputs)
      }
    }

  def withLoadedBuild[T](
    options: BuildOptions,
    buildThreads: BuildThreads, // actually only used when bloopConfigOpt is non-empty
    bloopConfigOpt: Option[BloopRifleConfig],
    fromDirectory: Boolean = false
  )(f: (os.Path, Inputs, Build) => T) =
    withBuild(options, buildThreads, bloopConfigOpt, fromDirectory)((p, i, maybeBuild) =>
      maybeBuild match {
        case Left(e)  => throw e
        case Right(b) => f(p, i, b)
      }
    )

  def withBuild[T](
    options: BuildOptions,
    buildThreads: BuildThreads, // actually only used when bloopConfigOpt is non-empty
    bloopConfigOpt: Option[BloopRifleConfig],
    fromDirectory: Boolean = false,
    buildTests: Boolean = true
  )(f: (os.Path, Inputs, Either[BuildException, Build]) => T): T =
    withCustomInputs(fromDirectory, None) { (root, inputs) =>
      val compilerMaker = bloopConfigOpt match {
        case Some(bloopConfig) =>
          new BloopCompilerMaker(bloopConfig, buildThreads.bloop, strictBloopJsonCheck = true)
        case None =>
          SimpleScalaCompilerMaker("java", Nil)
      }
      val res =
        Build.build(
          inputs,
          options,
          compilerMaker,
          None,
          TestLogger(),
          crossBuilds = false,
          buildTests = buildTests,
          partial = None
        )
      f(root, inputs, res.map(_.main))
    }
}

object TestInputs {

  def apply(files: (os.RelPath, String)*): TestInputs =
    TestInputs(files, Nil)

  def withTmpDir[T](prefix: String)(f: os.Path => T): T = {
    val tmpDir = os.temp.dir(prefix = prefix)
    try f(tmpDir)
    finally tryRemoveAll(tmpDir)
  }

  def tryRemoveAll(f: os.Path): Unit =
    try os.remove.all(f)
    catch {
      case ex: java.nio.file.FileSystemException =>
        System.err.println(s"Could not remove $f ($ex), will try to remove it upon JVM shutdown.")
        System.err.println(s"find $f = '${Try(os.walk(f))}'")
        Runtime.getRuntime.addShutdownHook(
          new Thread("remove-dir-windows") {
            setDaemon(true)
            override def run() =
              try os.remove.all(f)
              catch {
                case NonFatal(e) =>
                  System.err.println(s"Caught $e while trying to remove $f, ignoring it.")
              }
          }
        )
    }
}
