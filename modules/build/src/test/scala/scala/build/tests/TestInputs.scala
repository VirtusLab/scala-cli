package scala.build.tests

import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService

import scala.build.{Build, BuildThreads, Directories, Inputs}
import scala.build.blooprifle.BloopRifleConfig
import scala.build.errors.BuildException
import scala.build.options.BuildOptions
import scala.util.control.NonFatal
import scala.util.{Properties, Try}

final case class TestInputs(
  files: Seq[(os.RelPath, String)],
  inputArgs: Seq[String]
) {
  def withInputs[T](f: (os.Path, Inputs) => T): T =
    TestInputs.withTmpDir("scala-cli-tests-") { tmpDir =>
      for ((relPath, content) <- files) {
        val path = tmpDir / relPath
        os.write(path, content.getBytes(StandardCharsets.UTF_8), createFolders = true)
      }

      val inputArgs0 = if (inputArgs.isEmpty) files.map(_._1.toString) else inputArgs
      Inputs(inputArgs0, tmpDir, Directories.under(tmpDir / ".data")) match {
        case Left(err)     => sys.error(err)
        case Right(inputs) => f(tmpDir, inputs)
      }
    }

  def withBuild[T](
    options: BuildOptions,
    buildThreads: BuildThreads,
    bloopConfig: BloopRifleConfig
  )(f: (os.Path, Inputs, Either[BuildException, Build]) => T): T =
    withInputs { (root, inputs) =>
      val res =
        Build.build(inputs, options, buildThreads, bloopConfig, TestLogger(), crossBuilds = false)
      f(root, inputs, res.map(_._1))
    }
}

object TestInputs {

  def apply(files: (os.RelPath, String)*): TestInputs =
    TestInputs(files, Nil)

  private def withTmpDir[T](prefix: String)(f: os.Path => T): T = {
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
                  System.err.println(s"Could not remove $f, ignoring it.")
              }
          }
        )
    }
}
