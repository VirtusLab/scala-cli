package scala.build.tests

import bloop.rifle.BloopRifleConfig

import java.nio.charset.StandardCharsets
import scala.build.{Build, BuildThreads, Builds, Directories}
import scala.build.compiler.{BloopCompilerMaker, SimpleScalaCompilerMaker}
import scala.build.errors.BuildException
import scala.build.input.{Module, ScalaCliInvokeData, SubCommand}
import scala.build.internal.Util
import scala.build.options.{BuildOptions, Scope}
import scala.util.control.NonFatal
import scala.util.Try

final case class TestInputs(
  files: Seq[(os.RelPath, String)],
  inputArgs: Seq[String] = Seq.empty,
  forceCwd: Option[os.Path] = None
) {
  def withInputs[T](f: (os.Path, Module) => T): T =
    withCustomInputs(false, None)(f)

  def fromRoot[T](f: os.Path => T, skipCreatingSources: Boolean = false): T =
    TestInputs.withTmpDir("scala-cli-tests-", forceCwd) { tmpDir =>
      if skipCreatingSources then f(tmpDir)
      else {
        for ((relPath, content) <- files) {
          val path = tmpDir / relPath
          os.write(path, content.getBytes(StandardCharsets.UTF_8), createFolders = true)
        }

        f(tmpDir)
      }
    }

  def withCustomInputs[T](
    viaDirectory: Boolean,
    forcedWorkspaceOpt: Option[os.FilePath],
    skipCreatingSources: Boolean = false
  )(
    f: (os.Path, Module) => T
  ): T =
    fromRoot(
      { tmpDir =>
        val inputArgs0 =
          if (viaDirectory) Seq(tmpDir.toString)
          else if (inputArgs.isEmpty) files.map(_._1.toString)
          else inputArgs
        val res = Module(
          inputArgs0,
          tmpDir,
          forcedWorkspace = forcedWorkspaceOpt.map(_.resolveFrom(tmpDir)),
          allowRestrictedFeatures = true,
          extraClasspathWasPassed = false
        )(using ScalaCliInvokeData.dummy)
        res match {
          case Left(err)     => throw new Exception(err)
          case Right(inputs) => f(tmpDir, inputs)
        }
      },
      skipCreatingSources
    )

  def withLoadedBuild[T](
    options: BuildOptions,
    buildThreads: BuildThreads, // actually only used when bloopConfigOpt is non-empty
    bloopConfigOpt: Option[BloopRifleConfig],
    fromDirectory: Boolean = false
  )(f: (os.Path, Module, Build) => T) =
    withBuild(options, buildThreads, bloopConfigOpt, fromDirectory)((p, i, maybeBuild) =>
      maybeBuild match {
        case Left(e)  => throw e
        case Right(b) => f(p, i, b)
      }
    )

  def withBuilds[T](
    options: BuildOptions,
    buildThreads: BuildThreads, // actually only used when bloopConfigOpt is non-empty
    bloopConfigOpt: Option[BloopRifleConfig],
    fromDirectory: Boolean = false,
    buildTests: Boolean = true,
    actionableDiagnostics: Boolean = false,
    skipCreatingSources: Boolean = false
  )(f: (os.Path, Module, Either[BuildException, Builds]) => T): T =
    withCustomInputs(fromDirectory, None, skipCreatingSources) { (root, inputs) =>
      val compilerMaker = bloopConfigOpt match {
        case Some(bloopConfig) =>
          new BloopCompilerMaker(
            _ => Right(bloopConfig),
            buildThreads.bloop,
            strictBloopJsonCheck = true,
            offline = false
          )
        case None =>
          SimpleScalaCompilerMaker("java", Nil)
      }
      val builds =
        Build.build(
          inputs,
          options,
          compilerMaker,
          None,
          TestLogger(),
          crossBuilds = false,
          buildTests = buildTests,
          partial = None,
          actionableDiagnostics = Some(actionableDiagnostics)
        )(using ScalaCliInvokeData.dummy)
      f(root, inputs, builds)
    }

  def withBuild[T](
    options: BuildOptions,
    buildThreads: BuildThreads, // actually only used when bloopConfigOpt is non-empty
    bloopConfigOpt: Option[BloopRifleConfig],
    fromDirectory: Boolean = false,
    buildTests: Boolean = true,
    actionableDiagnostics: Boolean = false,
    scope: Scope = Scope.Main,
    skipCreatingSources: Boolean = false
  )(f: (os.Path, Module, Either[BuildException, Build]) => T): T =
    withBuilds(
      options,
      buildThreads,
      bloopConfigOpt,
      fromDirectory,
      buildTests = buildTests,
      actionableDiagnostics = actionableDiagnostics,
      skipCreatingSources = skipCreatingSources
    ) {
      (p, i, builds) =>
        f(
          p,
          i,
          builds.map(_.get(scope).getOrElse(sys.error(s"No ${scope.name} build found")))
        )
    }
}

object TestInputs {

  def apply(files: (os.RelPath, String)*): TestInputs =
    TestInputs(files, Nil)

  def withTmpDir[T](prefix: String, forceCwd: Option[os.Path] = None)(f: os.Path => T): T =
    forceCwd match {
      case Some(path) => f(path)
      case None =>
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
