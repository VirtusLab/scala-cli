package scala.cli.commands
package util

import caseapp.RemainingArgs
import coursier.util.Artifact
import dependency.AnyDependency
import dependency.parser.DependencyParser

import java.io.{ByteArrayOutputStream, File, InputStream}

import scala.build._
import scala.build.blooprifle.BloopRifleConfig
import scala.build.compiler.{BloopCompilerMaker, ScalaCompilerMaker, SimpleScalaCompilerMaker}
import scala.build.internal.CsLoggerUtil._
import scala.build.internal.{Constants, FetchExternalBinary, OsLibc}
import scala.build.options.{Platform, ScalacOpt, ShadowingSeq}
import scala.build.{options => bo}
import scala.cli.commands.ScalaJsOptions
import scala.concurrent.duration._
import scala.util.Properties
import scala.util.control.NonFatal

object SharedOptionsUtil {
  implicit class SharedOptionsOps(v: SharedOptions) {
    import v._

    def logger = logging.logger

    private def scalaJsOptions(opts: ScalaJsOptions): options.ScalaJsOptions = {
      import opts._
      options.ScalaJsOptions(
        version = jsVersion,
        mode = jsMode,
        moduleKindStr = jsModuleKind,
        checkIr = jsCheckIr,
        emitSourceMaps = jsEmitSourceMaps,
        sourceMapsDest = jsSourceMapsPath.filter(_.trim.nonEmpty).map(os.Path(_, Os.pwd)),
        dom = jsDom,
        header = jsHeader,
        allowBigIntsForLongs = jsAllowBigIntsForLongs,
        avoidClasses = jsAvoidClasses,
        avoidLetsAndConsts = jsAvoidLetsAndConsts,
        moduleSplitStyleStr = jsModuleSplitStyle,
        esVersionStr = jsEsVersion
      )
    }

    private def linkerOptions(opts: ScalaJsOptions): options.scalajs.ScalaJsLinkerOptions = {
      import opts._
      options.scalajs.ScalaJsLinkerOptions(
        linkerPath = jsLinkerPath
          .filter(_.trim.nonEmpty)
          .map(os.Path(_, Os.pwd)),
        scalaJsCliVersion = jsCliVersion.map(_.trim).filter(_.nonEmpty),
        javaArgs = jsCliJavaArg,
        useJvm = jsCliOnJvm.map {
          case false => Left(FetchExternalBinary.platformSuffix())
          case true  => Right(())
        }
      )
    }

    private def scalaNativeOptions(opts: ScalaNativeOptions): options.ScalaNativeOptions = {
      import opts._
      options.ScalaNativeOptions(
        nativeVersion,
        nativeMode,
        nativeGc,
        nativeClang,
        nativeClangpp,
        nativeLinking,
        nativeLinkingDefaults,
        nativeCompile,
        nativeCompileDefaults
      )
    }

    def buildOptions(
      enableJmh: Boolean,
      jmhVersion: Option[String],
      ignoreErrors: Boolean = false
    ): bo.BuildOptions = {
      val platformOpt =
        if (js.js) Some(Platform.JS)
        else if (native.native) Some(Platform.Native)
        else None
      bo.BuildOptions(
        scalaOptions = bo.ScalaOptions(
          scalaVersion = scalaVersion.map(_.trim).filter(_.nonEmpty),
          scalaBinaryVersion = scalaBinaryVersion.map(_.trim).filter(_.nonEmpty),
          addScalaLibrary = scalaLibrary.orElse(java.map(!_)),
          generateSemanticDbs = semanticDb,
          scalacOptions = ShadowingSeq.from(
            scalac.scalacOption
              .filter(_.nonEmpty)
              .map(ScalacOpt(_))
              .map(Positioned.commandLine(_))
          ),
          compilerPlugins =
            SharedOptionsUtil.parseDependencies(
              dependencies.compilerPlugin.map(Positioned.none(_)),
              ignoreErrors
            ),
          platform = platformOpt.map(o => Positioned(List(Position.CommandLine()), o))
        ),
        scriptOptions = bo.ScriptOptions(
          codeWrapper = None
        ),
        scalaJsOptions = scalaJsOptions(js),
        scalaNativeOptions = scalaNativeOptions(native),
        javaOptions = scala.cli.commands.util.JvmUtils.javaOptions(jvm),
        internalDependencies = bo.InternalDependenciesOptions(
          addStubsDependencyOpt = addStubs,
          addRunnerDependencyOpt = runner
        ),
        jmhOptions = bo.JmhOptions(
          addJmhDependencies =
            if (enableJmh) jmhVersion.orElse(Some(Constants.jmhVersion))
            else None,
          runJmh = if (enableJmh) Some(true) else None
        ),
        classPathOptions = bo.ClassPathOptions(
          extraClassPath = extraJars
            .flatMap(_.split(File.pathSeparator).toSeq)
            .filter(_.nonEmpty)
            .map(os.Path(_, os.pwd)),
          extraCompileOnlyJars = extraCompileOnlyJars
            .flatMap(_.split(File.pathSeparator).toSeq)
            .filter(_.nonEmpty)
            .map(os.Path(_, os.pwd)),
          extraRepositories = dependencies.repository.map(_.trim).filter(_.nonEmpty),
          extraDependencies = ShadowingSeq.from(
            SharedOptionsUtil.parseDependencies(
              dependencies.dependency.map(Positioned.none(_)),
              ignoreErrors
            )
          )
        ),
        internal = bo.InternalOptions(
          cache = Some(coursierCache),
          localRepository = LocalRepo.localRepo(directories.directories.localRepoDir),
          verbosity = Some(logging.verbosity),
          strictBloopJsonCheck = strictBloopJsonCheck
        ),
        notForBloopOptions = bo.PostBuildOptions(
          scalaJsLinkerOptions = linkerOptions(js)
        )
      )
    }

    private def downloadJvm(jvmId: String, options: bo.BuildOptions): String = {
      implicit val ec = options.finalCache.ec
      val javaHomeManager = options.javaHomeManager
        .withMessage(s"Downloading JVM $jvmId")
      val logger = javaHomeManager.cache
        .flatMap(_.archiveCache.cache.loggerOpt)
        .getOrElse(_root_.coursier.cache.CacheLogger.nop)
      val command = {
        val path = logger.use {
          try javaHomeManager.get(jvmId).unsafeRun()
          catch {
            case NonFatal(e) => throw new Exception(e)
          }
        }
        os.Path(path)
      }
      val ext     = if (Properties.isWin) ".exe" else ""
      val javaCmd = (command / "bin" / s"java$ext").toString
      javaCmd
    }

    def bloopRifleConfig(): BloopRifleConfig = {

      val options = buildOptions(false, None)
      lazy val defaultJvmCmd =
        downloadJvm(OsLibc.baseDefaultJvm(OsLibc.jvmIndexOs, "17"), options)
      val javaCmd = compilationServer.bloopJvm.map(downloadJvm(_, options)).orElse {
        for (javaHome <- options.javaHomeLocationOpt()) yield {
          val (javaHomeVersion, javaHomeCmd) = OsLibc.javaHomeVersion(javaHome.value)
          if (javaHomeVersion >= 17) javaHomeCmd
          else defaultJvmCmd
        }
      }.getOrElse(defaultJvmCmd)

      compilationServer.bloopRifleConfig(
        logging.logger,
        coursierCache,
        logging.verbosity,
        javaCmd,
        directories.directories,
        Some(17)
      )
    }

    def compilerMaker(threads: BuildThreads): ScalaCompilerMaker =
      if (compilationServer.server.getOrElse(true))
        new BloopCompilerMaker(
          bloopRifleConfig(),
          threads.bloop,
          strictBloopJsonCheckOrDefault
        )
      else
        SimpleScalaCompilerMaker("java", Nil)

    def coursierCache = cached(this)(coursier.coursierCache(logging.logger.coursierLogger("")))

    def inputsOrExit(
      args: RemainingArgs,
      defaultInputs: () => Option[Inputs] = () => Inputs.default()
    ): Inputs = inputsOrExit(args.all, defaultInputs)

    def inputsOrExit(
      args: Seq[String]
    ): Inputs =
      inputsOrExit(args, () => Inputs.default())
    def inputsOrExit(
      args: Seq[String],
      defaultInputs: () => Option[Inputs]
    ): Inputs = {
      val download: String => Either[String, Array[Byte]] = { url =>
        val artifact = Artifact(url).withChanging(true)
        val res = coursierCache.logger.use {
          try coursierCache.withTtl(0.seconds).file(artifact).run.unsafeRun()(coursierCache.ec)
          catch {
            case NonFatal(e) => throw new Exception(e)
          }
        }
        res
          .left.map(_.describe)
          .map(f => os.read.bytes(os.Path(f, Os.pwd)))
      }
      val resourceInputs = resourceDirs
        .map(os.Path(_, Os.pwd))
        .map { path =>
          if (!os.exists(path))
            logger.message(s"WARNING: provided resource directory path doesn't exist: $path")
          path
        }
        .map(Inputs.ResourceDirectory(_))
      val inputs = Inputs(
        args,
        Os.pwd,
        directories.directories,
        defaultInputs = defaultInputs,
        download = download,
        stdinOpt = readStdin(logger = logger),
        acceptFds = !Properties.isWin,
        forcedWorkspace = workspace.forcedWorkspaceOpt
      ) match {
        case Left(message) =>
          System.err.println(message)
          sys.exit(1)
        case Right(i) => i
      }
      val forbiddenDirs =
        (if (defaultForbiddenDirectories) myDefaultForbiddenDirectories else Nil) ++
          forbid.filter(_.trim.nonEmpty).map(os.Path(_, Os.pwd))

      inputs
        .add(resourceInputs)
        .checkAttributes(directories.directories)
        .avoid(forbiddenDirs, directories.directories)
    }

    def strictBloopJsonCheckOrDefault =
      strictBloopJsonCheck.getOrElse(bo.InternalOptions.defaultStrictBloopJsonCheck)
  }

  private def readStdin(in: InputStream = System.in, logger: Logger): Option[Array[Byte]] =
    if (in == null) {
      logger.debug("No stdin available")
      None
    }
    else {
      logger.debug("Reading stdin")
      val baos = new ByteArrayOutputStream
      val buf  = Array.ofDim[Byte](16 * 1024)
      var read = -1
      while ({
        read = in.read(buf)
        read >= 0
      })
        if (read > 0)
          baos.write(buf, 0, read)
      val result = baos.toByteArray
      logger.debug(s"Done reading stdin (${result.length} B)")
      Some(result)
    }

  private def myDefaultForbiddenDirectories: Seq[os.Path] =
    if (Properties.isWin)
      Seq(os.Path("""C:\Windows\System32"""))
    else
      Nil

  def parseDependencies(
    deps: List[Positioned[String]],
    ignoreErrors: Boolean
  ): Seq[Positioned[AnyDependency]] =
    deps.map(_.map(_.trim)).filter(_.value.nonEmpty)
      .flatMap { posDepStr =>
        val depStr = posDepStr.value
        DependencyParser.parse(depStr) match {
          case Left(err) =>
            if (ignoreErrors) Nil
            else sys.error(s"Error parsing dependency '$depStr': $err")
          case Right(dep) => Seq(posDepStr.map(_ => dep))
        }
      }
}
