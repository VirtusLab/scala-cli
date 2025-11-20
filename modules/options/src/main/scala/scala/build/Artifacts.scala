package scala.build

import coursier.cache.FileCache
import coursier.core.{Classifier, Module, ModuleName, Organization, Repository, Version}
import coursier.error.ResolutionError
import coursier.util.Task
import coursier.version.VersionConstraint
import coursier.{
  Dependency as CsDependency,
  Fetch,
  Resolution,
  core as csCore,
  util as csUtil
}
import dependency.*

import java.net.URL

import scala.build.CoursierUtils.*
import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.errors.{
  BuildException,
  CompositeBuildException,
  CoursierDependencyError,
  FetchingDependenciesError,
  NoScalaVersionProvidedError,
  ToolkitVersionError
}
import scala.build.internal.Constants
import scala.build.internal.Constants.*
import scala.build.internal.CsLoggerUtil.*
import scala.build.internal.Util.{PositionedScalaDependencyOps, safeFullDetailedArtifacts}
import scala.build.internals.ConsoleUtils.ScalaCliConsole.warnPrefix
import scala.collection.mutable

final case class Artifacts(
  javacPluginDependencies: Seq[(AnyDependency, String, os.Path)],
  extraJavacPlugins: Seq[os.Path],
  defaultDependencies: Seq[AnyDependency],
  extraDependencies: Seq[AnyDependency],
  userCompileOnlyDependencies: Seq[AnyDependency],
  internalDependencies: Seq[AnyDependency],
  detailedArtifacts: Seq[(CsDependency, csCore.Publication, csUtil.Artifact, os.Path)],
  detailedRuntimeArtifacts: Seq[(CsDependency, csCore.Publication, csUtil.Artifact, os.Path)],
  extraClassPath: Seq[os.Path],
  extraCompileOnlyJars: Seq[os.Path],
  extraRuntimeClassPath: Seq[os.Path],
  extraSourceJars: Seq[os.Path],
  scalaOpt: Option[ScalaArtifacts],
  hasJvmRunner: Boolean,
  resolution: Option[Resolution]
) {

  def userDependencies                  = defaultDependencies ++ extraDependencies
  lazy val jarsForUserExtraDependencies = {
    val extraDependenciesMap =
      extraDependencies.map(dep => dep.module.name -> dep.version).toMap
    detailedArtifacts
      .iterator
      .collect {
        case (dep, pub, _, path)
            if pub.classifier != Classifier.sources &&
            extraDependenciesMap.get(dep.module.name.value).contains(
              dep.versionConstraint.asString
            ) => path
      }
      .toVector
      .distinct
  }

  lazy val artifacts: Seq[(String, os.Path)] =
    detailedArtifacts
      .iterator
      .collect {
        case (_, pub, a, f) if pub.classifier != Classifier.sources =>
          (a.url, f)
      }
      .toVector
      .distinct
  lazy val runtimeArtifacts: Seq[(String, os.Path)] =
    detailedRuntimeArtifacts
      .iterator
      .collect {
        case (_, pub, a, f) if pub.classifier != Classifier.sources =>
          (a.url, f)
      }
      .toVector
      .distinct
  lazy val sourceArtifacts: Seq[(String, os.Path)] =
    detailedArtifacts
      .iterator
      .collect {
        case (_, pub, a, f) if pub.classifier == Classifier.sources =>
          (a.url, f)
      }
      .toVector
      .distinct
  lazy val classPath: Seq[os.Path] =
    runtimeArtifacts.map(_._2) ++ extraClassPath ++ extraRuntimeClassPath
  lazy val compileClassPath: Seq[os.Path] =
    artifacts.map(_._2) ++ extraClassPath ++ extraCompileOnlyJars
  lazy val sourcePath: Seq[os.Path] =
    sourceArtifacts.map(_._2) ++ extraSourceJars
}

object Artifacts {

  final case class ScalaArtifactsParams(
    params: ScalaParameters,
    compilerPlugins: Seq[Positioned[AnyDependency]],
    addJsTestBridge: Option[String],
    addNativeTestInterface: Option[String],
    scalaJsVersion: Option[String],
    scalaJsCliVersion: Option[String],
    scalaNativeCliVersion: Option[String],
    addScalapy: Option[String]
  )

  def apply(
    scalaArtifactsParamsOpt: Option[ScalaArtifactsParams],
    javacPluginDependencies: Seq[Positioned[AnyDependency]],
    extraJavacPlugins: Seq[os.Path],
    defaultDependencies: Seq[Positioned[AnyDependency]],
    extraDependencies: Seq[Positioned[AnyDependency]],
    compileOnlyDependencies: Seq[Positioned[AnyDependency]],
    extraClassPath: Seq[os.Path],
    extraCompileOnlyJars: Seq[os.Path],
    extraSourceJars: Seq[os.Path],
    fetchSources: Boolean,
    jvmVersion: Int,
    addJvmRunner: Option[Boolean],
    addJvmTestRunner: Boolean,
    addJmhDependencies: Option[String],
    extraRepositories: Seq[Repository],
    keepResolution: Boolean,
    includeBuildServerDeps: Boolean,
    cache: FileCache[Task],
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException]
  ): Either[BuildException, Artifacts] = either {
    val dependencies = defaultDependencies ++ extraDependencies

    val scalaVersion = (for {
      scalaArtifactsParams <- scalaArtifactsParamsOpt
      scalaParams  = scalaArtifactsParams.params
      scalaVersion = scalaParams.scalaVersion
    } yield scalaVersion).getOrElse(defaultScalaVersion)

    val shouldUseLegacyJava8Runners  = jvmVersion < Constants.scala38MinJavaVersion
    val shouldUseLegacyScala3Runners =
      scalaVersion.startsWith("3") &&
      scalaVersion.coursierVersion < s"$scala3LtsPrefix.0".coursierVersion
    val shouldUseLegacyScala2Runners = scalaVersion.startsWith("2")
    val shouldUseLegacyScalaRunners  = shouldUseLegacyScala3Runners || shouldUseLegacyScala2Runners
    val shouldUseLegacyRunners       = shouldUseLegacyScalaRunners || shouldUseLegacyJava8Runners

    val jvmTestRunnerDependencies =
      if addJvmTestRunner then {
        val runnerLegacyVersion =
          if scalaVersion.startsWith("3")
          then runnerScala30LegacyVersion
          else runnerScala2LegacyVersion
        val testRunnerVersion0 =
          if shouldUseLegacyRunners then {
            if shouldUseLegacyScalaRunners then
              logger.message(
                s"$warnPrefix Scala $scalaVersion is no longer supported by the test-runner module."
              )
            if shouldUseLegacyJava8Runners then
              logger.message(
                s"$warnPrefix Java $jvmVersion is no longer supported by the test-runner module."
              )
            logger.message(
              s"$warnPrefix Defaulting to a legacy test-runner module version: $runnerLegacyVersion."
            )
            if shouldUseLegacyScalaRunners then
              logger.message(
                s"$warnPrefix To use the latest test-runner, upgrade Scala to at least $scala3LtsPrefix."
              )
            if shouldUseLegacyJava8Runners then
              logger.message(
                s"$warnPrefix To use the latest test-runner, upgrade Java to at least ${Constants.defaultJavaVersion}."
              )
            runnerLegacyVersion
          }
          else testRunnerVersion
        Seq(dep"$testRunnerOrganization::$testRunnerModuleName:$testRunnerVersion0")
      }
      else Nil

    val jmhDependencies = addJmhDependencies.toSeq
      .map(version => dep"${Constants.jmhOrg}:${Constants.jmhGeneratorBytecodeModule}:$version")

    val maybeSnapshotRepo = {
      val hasSnapshots = jvmTestRunnerDependencies.exists(_.version.endsWith("SNAPSHOT")) ||
        scalaArtifactsParamsOpt.flatMap(_.scalaNativeCliVersion).exists(_.endsWith("SNAPSHOT"))
      if hasSnapshots then
        Seq(
          coursier.Repositories.sonatype("snapshots"),
          coursier.Repositories.sonatypeS01("snapshots"),
          RepositoryUtils.snapshotsRepository,
          RepositoryUtils.scala3NightlyRepository
        )
      else Nil
    }

    val allExtraRepositories =
      (maybeSnapshotRepo ++ extraRepositories).distinct

    val scalaOpt = scalaArtifactsParamsOpt match {
      case Some(scalaArtifactsParams) =>
        val compilerDependencies =
          if (scalaArtifactsParams.params.scalaVersion.startsWith("3."))
            Seq(
              dep"org.scala-lang::scala3-compiler:${scalaArtifactsParams.params.scalaVersion}"
            )
          else
            Seq(
              dep"org.scala-lang:scala-compiler:${scalaArtifactsParams.params.scalaVersion}"
            )
        val compilerDependenciesMessage =
          s"Downloading Scala ${scalaArtifactsParams.params.scalaVersion} compiler"

        val compilerPlugins0 = value {
          scalaArtifactsParams.compilerPlugins
            .map { posDep =>
              val posDep0 =
                posDep.map(dep =>
                  dep.copy(userParams = dep.userParams ++ Seq("intransitive" -> None))
                )
              artifacts(
                Seq(posDep0),
                allExtraRepositories,
                Some(scalaArtifactsParams.params),
                logger,
                cache.withMessage(s"Downloading compiler plugin ${posDep.value.render}")
              ).map(_.map { case (url, path) => (posDep0.value, url, path) })
            }
            .sequence
            .left.flatMap {
              CompositeBuildException(_)
                .maybeRecoverWithDefault(Seq.empty, maybeRecoverOnError)
            }
            .map(_.flatten)
        }

        val compilerArtifacts = value {
          artifacts(
            compilerDependencies.map(Positioned.none),
            allExtraRepositories,
            Some(scalaArtifactsParams.params),
            logger,
            cache.withMessage(compilerDependenciesMessage)
          ).left.flatMap(_.maybeRecoverWithDefault(Seq.empty, maybeRecoverOnError))
        }

        val bridgeJarsOpt =
          (scalaArtifactsParams.params.scalaVersion -> includeBuildServerDeps match {
            case (sv, true) if sv.startsWith("3.") =>
              Some(Seq(
                dep"org.scala-lang:scala3-sbt-bridge:${scalaArtifactsParams.params.scalaVersion}"
              ))
            case (sv, true)
                if Version(sv) >= Version("2.13.12") ||
                sv.startsWith(Constants.defaultScala213Version) =>
              Some(Seq(
                dep"org.scala-lang:scala2-sbt-bridge:${scalaArtifactsParams.params.scalaVersion}"
              ))
            case _ => None
          })
            .map { bridgeDependencies =>
              value {
                artifacts(
                  bridgeDependencies.map(Positioned.none),
                  (allExtraRepositories ++ Seq(RepositoryUtils.scala3NightlyRepository)).distinct,
                  Some(scalaArtifactsParams.params),
                  logger,
                  cache.withMessage(
                    s"Downloading Scala ${scalaArtifactsParams.params.scalaVersion} bridge"
                  )
                ).left.flatMap(_.maybeRecoverWithDefault(Seq.empty, maybeRecoverOnError))
              }.map(_._2)
            }

        def fetchedArtifactToPath(fetched: Fetch.Result): Either[BuildException, Seq[os.Path]] =
          either {
            value(fetched.fullDetailedArtifacts0.safeFullDetailedArtifacts)
              .collect { case (_, _, _, Some(f)) => os.Path(f, Os.pwd) }
          }

        val scalaJsCliDependency =
          scalaArtifactsParams.scalaJsCliVersion.map { scalaJsCliVersion =>
            val mod = Module(
              Organization("org.virtuslab.scala-cli"),
              ModuleName(s"scalajscli_2.13"),
              Map.empty
            )
            Seq(coursier.Dependency(mod, VersionConstraint(s"$scalaJsCliVersion+")))
          }

        val fetchedScalaJsCli = scalaJsCliDependency match {
          case Some(dependency) =>
            val forcedVersions = Seq(
              cmod"org.scala-js:scalajs-linker_2.13" -> VersionConstraint(scalaJsVersion)
            )
            Some {
              val (_, res) = value {
                fetchCsDependencies(
                  dependencies = dependency.map(Positioned.none),
                  extraRepositories = allExtraRepositories,
                  forceScalaVersionOpt = None,
                  forcedVersions = forcedVersions,
                  logger = logger,
                  cache = cache.withMessage("Downloading Scala.js CLI"),
                  classifiersOpt = None
                )
              }
              res
            }
          case None =>
            None
        }

        val scalaJsCli = value {
          fetchedScalaJsCli.toSeq
            .map(fetchedArtifactToPath)
            .sequence
            .map(_.flatten)
            .left
            .map(CompositeBuildException(_))
        }

        val scalaNativeCliDependency =
          scalaArtifactsParams.scalaNativeCliVersion.map { version =>
            val module = cmod"org.scala-native:scala-native-cli_2.12"
            Seq(coursier.Dependency(module, VersionConstraint(version)))
          }

        val fetchedScalaNativeCli: Option[Fetch.Result] = scalaNativeCliDependency match {
          case Some(dependency) =>
            Some {
              val (_, res) = value {
                fetchCsDependencies(
                  dependencies = dependency.map(Positioned.none),
                  extraRepositories = allExtraRepositories,
                  forceScalaVersionOpt = None,
                  forcedVersions = Nil,
                  logger = logger,
                  cache = cache.withMessage("Downloading Scala Native CLI"),
                  classifiersOpt = None
                )
              }
              res
            }
          case None =>
            None
        }

        val scalaNativeCli = value {
          fetchedScalaNativeCli.toSeq
            .map(fetchedArtifactToPath)
            .sequence
            .map(_.flatten)
            .left
            .map(CompositeBuildException(_))
        }

        val jsTestBridgeDependencies =
          scalaArtifactsParams.addJsTestBridge.toSeq.map { scalaJsVersion =>
            if (scalaArtifactsParams.params.scalaVersion.startsWith("2."))
              dep"org.scala-js::scalajs-test-bridge:$scalaJsVersion"
            else
              dep"org.scala-js:scalajs-test-bridge_2.13:$scalaJsVersion"
          }
        val nativeTestInterfaceDependencies =
          scalaArtifactsParams.addNativeTestInterface.toSeq.map { scalaNativeVersion =>
            dep"org.scala-native::test-interface::$scalaNativeVersion"
          }

        val scalapyDependencies = scalaArtifactsParams.addScalapy match {
          case Some(scalaPyVersion) =>
            Seq(dep"${scalaPyOrganization(scalaPyVersion)}::scalapy-core::$scalaPyVersion")
          case None =>
            Nil
        }

        val internalDependencies =
          jsTestBridgeDependencies ++
            nativeTestInterfaceDependencies

        val scala = ScalaArtifacts(
          compilerDependencies,
          compilerArtifacts,
          compilerPlugins0,
          scalaJsCli,
          scalaNativeCli,
          internalDependencies,
          scalapyDependencies,
          scalaArtifactsParams.params,
          bridgeJarsOpt
        )
        Some(scala)

      case None =>
        None
    }

    val internalDependencies =
      jvmTestRunnerDependencies.map(Positioned.none) ++
        scalaOpt.toSeq.flatMap(_.internalDependencies).map(Positioned.none) ++
        jmhDependencies.map(Positioned.none)
    val updatedDependencies = dependencies ++
      scalaOpt.toSeq.flatMap(_.extraDependencies).map(Positioned.none) ++
      internalDependencies
    val allUpdatedDependencies = compileOnlyDependencies ++ updatedDependencies

    val updatedDependenciesMessage = {
      val b           = new mutable.StringBuilder("Downloading ")
      val depLen      = dependencies.length
      val extraDepLen = allUpdatedDependencies.length - depLen
      depLen match {
        case 1          => b.append("one dependency")
        case n if n > 1 => b.append(s"$n dependencies")
        case _          =>
      }

      if (depLen > 0 && extraDepLen > 0)
        b.append(" and ")

      extraDepLen match {
        case 1          => b.append("one internal dependency")
        case n if n > 1 => b.append(s"$n internal dependencies")
        case _          =>
      }

      b.result()
    }

    val (fetcher: Fetch[Task], fetchRes: Fetch.Result) = value {
      fetchAnyDependenciesWithResult(
        allUpdatedDependencies,
        allExtraRepositories,
        scalaArtifactsParamsOpt.map(_.params),
        logger,
        cache.withMessage(updatedDependenciesMessage),
        classifiersOpt = Some(Set("_") ++ (if (fetchSources) Set("sources") else Set.empty)),
        maybeRecoverOnError
      )
    }

    val updatedDependencies0 = value {
      coursierDeps(
        updatedDependencies,
        scalaArtifactsParamsOpt.map(_.params),
        maybeRecoverOnError
      )
    }
    val runtimeRes = value {
      val resolution = value {
        fetchRes.resolution.subset0(updatedDependencies0.map(_._2._1))
          .left.map(CoursierDependencyError(_))
      }
      // this is actually fetcher.artifacts, which is a private field…
      val artifacts = coursier.Artifacts()
        .withCache(fetcher.cache)
        .withClassifiers(fetcher.classifiers)
        .withMainArtifactsOpt(fetcher.mainArtifactsOpt)
        .withArtifactTypesOpt(fetcher.artifactTypesOpt)
        .withExtraArtifactsSeq(fetcher.extraArtifactsSeq)
        .withClasspathOrder(fetcher.classpathOrder)
        .withTransformArtifacts(fetcher.transformArtifacts)
      artifacts
        .withResolution(resolution)
        .runResult()
        .fullDetailedArtifacts0
        .safeFullDetailedArtifacts
    }

    val (hasRunner, extraRunnerJars) =
      if scalaOpt.nonEmpty then {
        val addJvmRunner0 = addJvmRunner.getOrElse(false)
        val runnerJars    =
          if addJvmRunner0 then {
            val maybeSnapshotRepo =
              if runnerVersion.endsWith("SNAPSHOT") then
                Seq(
                  coursier.Repositories.sonatype("snapshots"),
                  coursier.Repositories.sonatypeS01("snapshots"),
                  RepositoryUtils.snapshotsRepository,
                  RepositoryUtils.scala3NightlyRepository
                )
              else Nil
            val runnerVersion0 =
              if shouldUseLegacyRunners then {
                val runnerLegacyVersion =
                  if shouldUseLegacyScala3Runners
                  then runnerScala30LegacyVersion
                  else runnerScala2LegacyVersion
                if shouldUseLegacyScalaRunners then
                  logger.message(
                    s"$warnPrefix Scala $scalaVersion is no longer supported by the runner module."
                  )
                if shouldUseLegacyJava8Runners then
                  logger.message(
                    s"$warnPrefix Java $jvmVersion is no longer supported by the runner module."
                  )
                logger.message(
                  s"$warnPrefix Defaulting to a legacy runner module version: $runnerLegacyVersion."
                )
                if shouldUseLegacyScalaRunners then
                  logger.message(
                    s"$warnPrefix To use the latest runner, upgrade Scala to at least $scala3LtsPrefix."
                  )
                if shouldUseLegacyJava8Runners then
                  logger.message(
                    s"$warnPrefix To use the latest runner, upgrade Java to at least ${Constants.defaultJavaVersion}."
                  )
                logger.message(
                  s"""$warnPrefix Scala $scalaVersion is no longer supported by the runner module.
                     |$warnPrefix Defaulting to a legacy runner module version: $runnerLegacyVersion.
                     |$warnPrefix To use the latest runner, upgrade Scala to at least $scala3LtsPrefix."""
                    .stripMargin
                )
                runnerLegacyVersion
              }
              else runnerVersion
            value {
              artifacts(
                Seq(Positioned.none(
                  dep"$runnerOrganization::$runnerModuleName:$runnerVersion0,intransitive"
                )),
                extraRepositories ++ maybeSnapshotRepo,
                scalaArtifactsParamsOpt.map(_.params),
                logger,
                cache.withMessage("Downloading runner dependency")
              ).map(_.map(_._2))
            }
          }
          else
            Nil

        (addJvmRunner0, runnerJars)
      }
      else
        (false, Nil)

    val javacPlugins0 = value {
      javacPluginDependencies
        .map { posDep =>
          val cache0 = cache.withMessage(s"Downloading javac plugin ${posDep.value.render}")
          artifacts(
            Seq(posDep),
            allExtraRepositories,
            scalaArtifactsParamsOpt.map(_.params),
            logger,
            cache0
          )
            .map(_.map { case (url, path) => (posDep.value, url, path) })
        }
        .sequence
        .left.map(CompositeBuildException(_))
        .map(_.flatten)
    }

    val detailedArtifacts =
      value(fetchRes.fullDetailedArtifacts0.safeFullDetailedArtifacts)
        .collect { case (d, p, a, Some(f)) => (d, p, a, os.Path(f, Os.pwd)) }
    Artifacts(
      javacPlugins0,
      extraJavacPlugins,
      defaultDependencies.map(_.value),
      extraDependencies.map(_.value) ++ scalaOpt.toSeq.flatMap(_.extraDependencies),
      compileOnlyDependencies.map(_.value),
      internalDependencies.map(_.value),
      detailedArtifacts,
      runtimeRes.collect { case (d, p, a, Some(f)) =>
        (d, p, a, os.Path(f, Os.pwd))
      },
      extraClassPath,
      extraCompileOnlyJars,
      extraRunnerJars,
      extraSourceJars,
      scalaOpt,
      hasRunner,
      if (keepResolution) Some(fetchRes.resolution) else None
    )
  }

  def scalaPyOrganization(version: String): String = {
    def sortAfterPlus(v: String) = coursier.core.Version(v.replace("+", "-"))
    if (sortAfterPlus(version).compareTo(sortAfterPlus("0.5.2+9-623f0807")) < 0)
      "me.shadaj"
    else
      "dev.scalapy"
  }

  private[build] def artifacts(
    dependencies: Seq[Positioned[AnyDependency]],
    extraRepositories: Seq[Repository],
    paramsOpt: Option[ScalaParameters],
    logger: Logger,
    cache: FileCache[Task],
    classifiersOpt: Option[Set[String]] = None
  ): Either[BuildException, Seq[(String, os.Path)]] = either {
    val res =
      value(fetchAnyDependencies(
        dependencies,
        extraRepositories,
        paramsOpt,
        logger,
        cache,
        classifiersOpt
      ))
    val result = res
      .artifacts
      .iterator
      .map { case (a, f) => (a.url, os.Path(f, Os.pwd)) }
      .toList
    logger.debug {
      val elems = Seq(s"Found ${result.length} artifacts:") ++
        result.map("  " + _._2) ++
        Seq("")
      elems.mkString(System.lineSeparator())
    }
    result
  }

  def coursierDeps(
    dependencies: Seq[Positioned[AnyDependency]],
    paramsOpt: Option[ScalaParameters],
    maybeRecoverOnError: BuildException => Option[BuildException]
  ): Either[BuildException, Seq[Positioned[(
    CsDependency,
    Option[((Module, VersionConstraint), (URL, Boolean))]
  )]]] =
    dependencies
      .map(dep =>
        dep.toCs(paramsOpt)
          .map { case Positioned(pos, csDep) => Positioned(pos, (dep.value, csDep)) }
      )
      .map(_.left.map(maybeRecoverOnError))
      .flatMap {
        case Left(Some(e: NoScalaVersionProvidedError)) => Some(Left(e))
        case Left(_)                                    => None
        case Right(depTuple)                            => Some(Right(depTuple))
      }
      .sequence
      .left.map(CompositeBuildException(_))
      .map(positionedDepTupleSeq =>
        positionedDepTupleSeq.map {
          case Positioned(positions, (dep, csDep)) =>
            val maybeUrl = dep.userParams.find(_._1 == "url").flatMap(_._2.map(new URL(_)))
            val fallback = maybeUrl.map(url =>
              (csDep.module -> csDep.versionConstraint) -> (url -> true)
            )
            Positioned(positions, (csDep, fallback))
        }
      )

  def fetchAnyDependencies(
    dependencies: Seq[Positioned[AnyDependency]],
    extraRepositories: Seq[Repository],
    paramsOpt: Option[ScalaParameters],
    logger: Logger,
    cache: FileCache[Task],
    classifiersOpt: Option[Set[String]],
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e)
  ): Either[BuildException, Fetch.Result] = either {
    val (_, res) = value {
      fetchAnyDependenciesWithResult(
        dependencies,
        extraRepositories,
        paramsOpt,
        logger,
        cache,
        classifiersOpt,
        maybeRecoverOnError
      )
    }
    res
  }

  private def fetchAnyDependenciesWithResult(
    dependencies: Seq[Positioned[AnyDependency]],
    extraRepositories: Seq[Repository],
    paramsOpt: Option[ScalaParameters],
    logger: Logger,
    cache: FileCache[Task],
    classifiersOpt: Option[Set[String]],
    maybeRecoverOnError: BuildException => Option[BuildException]
  ): Either[BuildException, (coursier.Fetch[Task], coursier.Fetch.Result)] = either {
    val coursierDependenciesWithFallbacks: Seq[Positioned[(
      CsDependency,
      Option[((Module, VersionConstraint), (URL, Boolean))]
    )]] = value {
      coursierDeps(dependencies, paramsOpt, maybeRecoverOnError)
    }

    val coursierDependencies: Seq[Positioned[CsDependency]] =
      coursierDependenciesWithFallbacks.map(_.map(_._1))
    val fallbacks: Map[(Module, VersionConstraint), (URL, Boolean)] =
      coursierDependenciesWithFallbacks.map(_.value)
        .flatMap(_._2)
        .toMap

    value {
      fetchCsDependencies(
        dependencies = coursierDependencies,
        extraRepositories = extraRepositories,
        forceScalaVersionOpt = paramsOpt.map(_.scalaVersion),
        forcedVersions = Nil,
        logger = logger,
        cache = cache,
        classifiersOpt = classifiersOpt,
        fallbacks = fallbacks
      ).left.flatMap(_.maybeRecoverWithDefault(
        (
          fetcher(
            dependencies = coursierDependencies,
            extraRepositories = extraRepositories,
            forceScalaVersionOpt = paramsOpt.map(_.scalaVersion),
            forcedVersions = Nil,
            cache = cache,
            classifiersOpt = classifiersOpt,
            fallbacks = fallbacks
          ),
          Fetch.Result()
        ),
        maybeRecoverOnError
      ))
    }
  }

  private def fetcher(
    dependencies: Seq[Positioned[coursier.Dependency]],
    extraRepositories: Seq[Repository],
    forceScalaVersionOpt: Option[String],
    forcedVersions: Seq[(coursier.Module, VersionConstraint)],
    cache: FileCache[Task],
    classifiersOpt: Option[Set[String]],
    fallbacks: Map[(Module, VersionConstraint), (URL, Boolean)]
  ): coursier.Fetch[Task] = {

    val fallbackRepository            = TemporaryInMemoryRepository(fallbacks)
    val extraRepositoriesWithFallback = extraRepositories :+ fallbackRepository

    val forceScalaVersions = forceScalaVersionOpt match {
      case None     => Nil
      case Some(sv) =>
        val svc = VersionConstraint(sv)
        if (sv.startsWith("2."))
          Seq(
            cmod"org.scala-lang:scala-library"  -> svc,
            cmod"org.scala-lang:scala-compiler" -> svc,
            cmod"org.scala-lang:scala-reflect"  -> svc
          )
        else
          // FIXME Shouldn't we force the org.scala-lang:scala-library version too?
          // (to a 2.13.x version)
          Seq(
            cmod"org.scala-lang:scala3-library_3"         -> svc,
            cmod"org.scala-lang:scala3-compiler_3"        -> svc,
            cmod"org.scala-lang:scala3-interfaces_3"      -> svc,
            cmod"org.scala-lang:scala3-tasty-inspector_3" -> svc,
            cmod"org.scala-lang:tasty-core_3"             -> svc
          )
    }

    val forceVersion = forceScalaVersions ++ forcedVersions

    // FIXME Many parameters that we could allow to customize here
    val defaultFetcher = coursier.Fetch()
    var fetcher        = defaultFetcher
      .withCache(cache)
      // repository order matters here, since in some cases coursier resolves only the head
      .withRepositories(extraRepositoriesWithFallback ++ defaultFetcher.repositories)
      .addDependencies(dependencies.map(_.value)*)
      .mapResolutionParams(_.addForceVersion0(forceVersion*))
    for (classifiers <- classifiersOpt) {
      if (classifiers("_"))
        fetcher = fetcher.withMainArtifacts()
      fetcher = fetcher
        .addClassifiers(classifiers.toSeq.filter(_ != "_").map(coursier.Classifier(_))*)
    }
    fetcher
  }

  def fetchCsDependencies(
    dependencies: Seq[Positioned[coursier.Dependency]],
    extraRepositories: Seq[Repository],
    forceScalaVersionOpt: Option[String],
    forcedVersions: Seq[(coursier.Module, VersionConstraint)],
    logger: Logger,
    cache: FileCache[Task],
    classifiersOpt: Option[Set[String]],
    fallbacks: Map[(Module, VersionConstraint), (URL, Boolean)] = Map.empty
  ): Either[BuildException, (coursier.Fetch[Task], coursier.Fetch.Result)] = either {
    logger.debug {
      s"Fetching ${dependencies.map(_.value)}" +
        (if (extraRepositories.isEmpty) "" else s", adding $extraRepositories")
    }

    val fetcher0 = fetcher(
      dependencies = dependencies,
      extraRepositories = extraRepositories,
      forceScalaVersionOpt = forceScalaVersionOpt,
      forcedVersions = forcedVersions,
      cache = cache,
      classifiersOpt = classifiersOpt,
      fallbacks = fallbacks
    )

    val res = cache.logger.use {
      fetcher0.eitherResult()
    }
    value {
      res.left.map {
        case ex: ResolutionError.Several =>
          CompositeBuildException(
            ex.errors.map(toFetchingDependenciesError(dependencies, _))
          )
        case ex: ResolutionError.Simple =>
          toFetchingDependenciesError(dependencies, ex)
        case ex => new FetchingDependenciesError(ex, dependencies.flatMap(_.positions))
      }.map((fetcher0, _))
    }
  }

  def toFetchingDependenciesError(
    dependencies: Seq[Positioned[coursier.Dependency]],
    resolutionError: coursier.error.ResolutionError.Simple
  ) = resolutionError match {
    case ex: ResolutionError.CantDownloadModule
        if ex.module.name.value == s"${Constants.toolkitName}_2.12" || ex.module.name
          .value == s"${Constants.toolkitTestName}_2.12" =>
      val errorPositions = dependencies.collect {
        case Positioned(pos, dep)
            if ex.module == dep.module => pos
      }.flatten
      new ToolkitVersionError(
        "Toolkits do not support Scala 2.12",
        errorPositions
      )
    case ex: ResolutionError.CantDownloadModule =>
      val errorPositions = dependencies.collect {
        case Positioned(pos, dep)
            if ex.module == dep.module => pos
      }.flatten
      new FetchingDependenciesError(ex, errorPositions)
    case ex => new FetchingDependenciesError(ex, dependencies.flatMap(_.positions))
  }
}
