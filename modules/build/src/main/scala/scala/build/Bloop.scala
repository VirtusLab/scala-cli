package scala.build

import bloop.rifle.{BloopRifleConfig, BuildServer}
import ch.epfl.scala.bsp4j
import coursier.cache.FileCache
import coursier.util.Task
import dependency.parser.ModuleParser
import dependency.{AnyDependency, DependencyLike, ScalaParameters, ScalaVersion}

import java.io.{File, IOException}

import scala.annotation.tailrec
import scala.build.EitherCps.{either, value}
import scala.build.errors.{BuildException, ModuleFormatError}
import scala.build.internal.CsLoggerUtil.*
import scala.concurrent.ExecutionException
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.*

object Bloop {

  final case class BloopTestOptions(
    testOnly: Option[String] = None,
    selectedTestClasses: Seq[String] = Nil,
    extraArgs: Seq[String] = Nil
  )

  private object BrokenPipeInCauses {
    @tailrec
    def unapply(ex: Throwable): Option[IOException] =
      ex match {
        case null                                                           => None
        case ex: IOException if ex.getMessage == "Broken pipe"              => Some(ex)
        case ex: IOException if ex.getMessage == "Connection reset by peer" => Some(ex)
        case _                                                              => unapply(ex.getCause)
      }
  }

  def compile(
    projectName: String,
    buildServer: BuildServer,
    logger: Logger,
    buildTargetsTimeout: FiniteDuration
  ): Either[Throwable, Boolean] =
    try retry()(logger) {
        logger.debug("Listing BSP build targets")
        val results = buildServer.workspaceBuildTargets()
          .get(buildTargetsTimeout.length, buildTargetsTimeout.unit)
        val buildTargetOpt = results.getTargets.asScala.find(_.getDisplayName == projectName)

        val buildTarget = buildTargetOpt.getOrElse {
          throw new Exception(
            s"Expected to find project '$projectName' in build targets (only got ${results.getTargets
                .asScala.map("'" + _.getDisplayName + "'").mkString(", ")})"
          )
        }

        logger.debug(s"Compiling $projectName with Bloop")
        val compileRes = buildServer.buildTargetCompile(
          new bsp4j.CompileParams(List(buildTarget.getId).asJava)
        ).get()

        val success = compileRes.getStatusCode == bsp4j.StatusCode.OK
        logger.debug(if (success) "Compilation succeeded" else "Compilation failed")
        Right(success)
      }
    catch {
      case ex @ BrokenPipeInCauses(_) =>
        logger.debug(s"Caught $ex while exchanging with Bloop server, assuming Bloop server exited")
        Left(ex)
      case ex: ExecutionException =>
        logger.debug(
          s"Caught $ex while exchanging with Bloop server, you may consider restarting the build server"
        )
        Left(ex)
    }

  def test(
    projectName: String,
    buildServer: BuildServer,
    logger: Logger,
    buildTargetsTimeout: FiniteDuration,
    testOptions: BloopTestOptions = BloopTestOptions()
  ): Either[Throwable, Int] =
    try retry()(logger) {
        logger.debug("Listing BSP build targets for test")
        val results = buildServer.workspaceBuildTargets()
          .get(buildTargetsTimeout.length, buildTargetsTimeout.unit)
        val buildTargetOpt = results.getTargets.asScala.find(_.getDisplayName == projectName)

        val buildTarget = buildTargetOpt.getOrElse {
          throw new Exception(
            s"Expected to find project '$projectName' in build targets (only got ${results.getTargets
                .asScala.map("'" + _.getDisplayName + "'").mkString(", ")})"
          )
        }

        logger.debug(s"Testing $projectName with Bloop")
        val testParams = buildTestParams(buildTarget.getId, testOptions)
        val testRes    = buildServer.buildTargetTest(testParams).get()

        val statusCode = testRes.getStatusCode
        val exitCode   = statusCode match {
          case bsp4j.StatusCode.OK        => 0
          case bsp4j.StatusCode.ERROR     => 1
          case bsp4j.StatusCode.CANCELLED => 1
        }
        logger.debug(if (exitCode == 0) "Tests succeeded" else "Tests failed")
        Right(exitCode)
      }
    catch {
      case ex @ BrokenPipeInCauses(_) =>
        logger.debug(s"Caught $ex while exchanging with Bloop server, assuming Bloop server exited")
        Left(ex)
      case ex: ExecutionException =>
        logger.debug(
          s"Caught $ex while exchanging with Bloop server, you may consider restarting the build server"
        )
        Left(ex)
    }

  private def buildTestParams(
    buildTargetId: bsp4j.BuildTargetIdentifier,
    testOptions: BloopTestOptions
  ): bsp4j.TestParams = {
    val params  = new bsp4j.TestParams(List(buildTargetId).asJava)
    val classes = testOptions.selectedTestClasses
    if classes.nonEmpty then
      if testOptions.extraArgs.nonEmpty then {
        val selections = classes.map { className =>
          new bsp4j.ScalaTestSuiteSelection(className, testOptions.extraArgs.asJava)
        }
        val suites = new bsp4j.ScalaTestSuites(
          selections.asJava,
          List.empty[String].asJava,
          List.empty[String].asJava
        )
        params.setDataKind(bsp4j.TestParamsDataKind.SCALA_TEST_SUITES_SELECTION)
        params.setData(suites)
      }
      else {
        params.setDataKind(bsp4j.TestParamsDataKind.SCALA_TEST_SUITES)
        params.setData(classes.asJava)
      }
    params
  }

  def bloopClassPath(
    dep: AnyDependency,
    params: ScalaParameters,
    logger: Logger,
    cache: FileCache[Task]
  ): Either[BuildException, Seq[File]] =
    either {
      val res = value {
        Artifacts.artifacts(
          Seq(Positioned.none(dep)),
          Seq(
            coursier.Repositories.centralMavenSnapshots,
            RepositoryUtils.snapshotsRepository,
            RepositoryUtils.scala3NightlyRepository
          ),
          Some(params),
          logger,
          cache.withMessage(s"Downloading compilation server ${dep.version}")
        )
      }
      res.map(_._2.toIO)
    }

  def bloopClassPath(
    logger: Logger,
    cache: FileCache[Task]
  ): Either[BuildException, Seq[File]] =
    bloopClassPath(logger, cache, BloopRifleConfig.defaultVersion)

  def bloopClassPath(
    logger: Logger,
    cache: FileCache[Task],
    bloopVersion: String
  ): Either[BuildException, Seq[File]] = either {
    val moduleStr = BloopRifleConfig.defaultModule
    val mod       = value {
      ModuleParser.parse(moduleStr)
        .left.map(err => new ModuleFormatError(moduleStr, err, Some("Bloop")))
    }
    val dep    = DependencyLike(mod, bloopVersion)
    val sv     = BloopRifleConfig.defaultScalaVersion
    val sbv    = ScalaVersion.binary(sv)
    val params = ScalaParameters(sv, sbv)
    value(bloopClassPath(dep, params, logger, cache))
  }
}
