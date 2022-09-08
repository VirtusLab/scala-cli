package scala.build

import coursier.cache.FileCache
import coursier.core.{Classifier, Module}
import coursier.parse.RepositoryParser
import coursier.util.Task
import coursier.{Dependency => CsDependency, Fetch, Resolution, core => csCore, util => csUtil}
import dependency.*
import os.Path

import java.net.URL

import scala.build.CoursierUtils.*
import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.errors.{
  BuildException,
  CompositeBuildException,
  FetchingDependenciesError,
  NoScalaVersionProvidedError,
  RepositoryFormatError
}
import scala.build.internal.Constants.*
import scala.build.internal.CsLoggerUtil.*
import scala.build.internal.Util.PositionedScalaDependencyOps
import scala.collection.mutable

final case class Artifacts(
  javacPluginDependencies: Seq[(AnyDependency, String, os.Path)],
  extraJavacPlugins: Seq[os.Path],
  userDependencies: Seq[AnyDependency],
  internalDependencies: Seq[AnyDependency],
  detailedArtifacts: Seq[(CsDependency, csCore.Publication, csUtil.Artifact, os.Path)],
  extraClassPath: Seq[os.Path],
  extraCompileOnlyJars: Seq[os.Path],
  extraSourceJars: Seq[os.Path],
  scalaOpt: Option[ScalaArtifacts],
  hasJvmRunner: Boolean,
  resolution: Option[Resolution]
) {
  lazy val artifacts: Seq[(String, os.Path)] =
    detailedArtifacts
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
    artifacts.map(_._2) ++ extraClassPath
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
    scalaJsCliVersion: Option[String],
    scalaNativeCliVersion: Option[String],
    addScalapy: Boolean
  )

  def apply(
    scalaArtifactsParamsOpt: Option[ScalaArtifactsParams],
    javacPluginDependencies: Seq[Positioned[AnyDependency]],
    extraJavacPlugins: Seq[os.Path],
    dependencies: Seq[Positioned[AnyDependency]],
    extraClassPath: Seq[os.Path],
    extraCompileOnlyJars: Seq[os.Path],
    extraSourceJars: Seq[os.Path],
    fetchSources: Boolean,
    addStubs: Boolean,
    addJvmRunner: Option[Boolean],
    addJvmTestRunner: Boolean,
    addJmhDependencies: Option[String],
    extraRepositories: Seq[String],
    keepResolution: Boolean,
    cache: FileCache[Task],
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException]
  ): Either[BuildException, Artifacts] = either {

    val addJvmRunner0 = addJvmRunner.getOrElse(true)
    val jvmRunnerDependencies =
      if (addJvmRunner0)
        Seq(dep"$runnerOrganization::$runnerModuleName:$runnerVersion")
      else
        Nil
    val jvmTestRunnerDependencies =
      if (addJvmTestRunner)
        Seq(dep"$testRunnerOrganization::$testRunnerModuleName:$testRunnerVersion")
      else
        Nil

    val jmhDependencies = addJmhDependencies.toSeq.map { version =>
      dep"org.openjdk.jmh:jmh-generator-bytecode:$version"
    }

    val maybeSnapshotRepo = {
      val hasSnapshots = (jvmRunnerDependencies ++ jvmTestRunnerDependencies)
        .exists(_.version.endsWith("SNAPSHOT")) ||
        scalaArtifactsParamsOpt.flatMap(_.scalaNativeCliVersion).exists(_.endsWith("SNAPSHOT"))
      val stubsNeedSonatypeSnapshots = addStubs && stubsVersion.endsWith("SNAPSHOT")
      if (hasSnapshots || stubsNeedSonatypeSnapshots)
        Seq(coursier.Repositories.sonatype("snapshots").root)
      else
        Nil
    }

    val allExtraRepositories =
      maybeSnapshotRepo ++ extraRepositories

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
                posDep.map(dep => dep.copy(userParams = dep.userParams + ("intransitive" -> None)))
              artifacts(
                posDep0.map(Seq(_)),
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
            Positioned.none(compilerDependencies),
            allExtraRepositories,
            Some(scalaArtifactsParams.params),
            logger,
            cache.withMessage(compilerDependenciesMessage)
          ).left.flatMap(_.maybeRecoverWithDefault(Seq.empty, maybeRecoverOnError))
        }

        def fetchedArtifactToPath(fetched: Fetch.Result): Seq[os.Path] =
          fetched.fullDetailedArtifacts.collect { case (_, _, _, Some(f)) => os.Path(f, Os.pwd) }

        val scalaJsCliDependency =
          scalaArtifactsParams.scalaJsCliVersion.map { version =>
            val mod =
              if (version.contains("-sc")) cmod"io.github.alexarchambault.tmp:scalajs-cli_2.13"
              else cmod"org.scala-js:scalajs-cli_2.13"
            Seq(coursier.Dependency(mod, version))
          }

        val fetchedScalaJsCli = scalaJsCliDependency match {
          case Some(dependency) =>
            val forcedVersions = Seq(
              cmod"org.scala-js:scalajs-linker_2.13" -> scalaJsVersion
            )
            Some(
              value {
                fetch0(
                  Positioned.none(dependency),
                  allExtraRepositories,
                  None,
                  forcedVersions,
                  logger,
                  cache.withMessage("Downloading Scala.js CLI"),
                  None
                )
              }
            )
          case None =>
            None
        }

        val scalaJsCli = fetchedScalaJsCli.toSeq.flatMap(fetchedArtifactToPath)

        val scalaNativeCliDependency =
          scalaArtifactsParams.scalaNativeCliVersion.map { version =>
            val module = cmod"org.scala-native:scala-native-cli_2.12"
            Seq(coursier.Dependency(module, version))
          }

        val fetchedScalaNativeCli = scalaNativeCliDependency match {
          case Some(dependency) =>
            Some(
              value {
                fetch0(
                  Positioned.none(dependency),
                  allExtraRepositories,
                  None,
                  Nil,
                  logger,
                  cache.withMessage("Downloading Scala Native CLI"),
                  None
                )
              }
            )
          case None =>
            None
        }

        val scalaNativeCli = fetchedScalaNativeCli.toSeq.flatMap(fetchedArtifactToPath)

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

        val scalapyDependencies =
          if (scalaArtifactsParams.addScalapy)
            Seq(dep"me.shadaj::scalapy-core::$scalaPyVersion")
          else
            Nil

        val internalDependencies =
          jsTestBridgeDependencies ++
            nativeTestInterfaceDependencies ++
            scalapyDependencies

        val scala = ScalaArtifacts(
          compilerDependencies,
          compilerArtifacts,
          compilerPlugins0,
          scalaJsCli,
          scalaNativeCli,
          internalDependencies,
          scalaArtifactsParams.params
        )
        Some(scala)

      case None =>
        None
    }

    val internalDependencies =
      jvmRunnerDependencies.map(Positioned.none) ++
        jvmTestRunnerDependencies.map(Positioned.none) ++
        scalaOpt.toSeq.flatMap(_.internalDependencies).map(Positioned.none) ++
        jmhDependencies.map(Positioned.none)
    val updatedDependencies = dependencies ++ internalDependencies

    val updatedDependenciesMessage = {
      val b           = new mutable.StringBuilder("Downloading ")
      val depLen      = dependencies.length
      val extraDepLen = updatedDependencies.length - depLen
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

    val fetchRes = value {
      fetch(
        Positioned.sequence(updatedDependencies),
        allExtraRepositories,
        scalaArtifactsParamsOpt.map(_.params),
        logger,
        cache.withMessage(updatedDependenciesMessage),
        classifiersOpt = Some(Set("_") ++ (if (fetchSources) Set("sources") else Set.empty)),
        maybeRecoverOnError
      )
    }

    val extraStubsJars =
      // stubs add classes for 'import $ivy' and 'import $dep' to work
      // we only need those in Scala sources, not in pure Java projects
      if (scalaOpt.nonEmpty && addStubs)
        value {
          artifacts(
            Positioned.none(Seq(dep"$stubsOrganization:$stubsModuleName:$stubsVersion")),
            allExtraRepositories,
            scalaArtifactsParamsOpt.map(_.params),
            logger,
            cache.withMessage("Downloading internal stub dependency")
          ).map(_.map(_._2))
        }
      else
        Nil

    val javacPlugins0 = value {
      javacPluginDependencies
        .map { posDep =>
          val cache0 = cache.withMessage(s"Downloading javac plugin ${posDep.value.render}")
          artifacts(
            posDep.map(Seq(_)),
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

    Artifacts(
      javacPlugins0,
      extraJavacPlugins,
      dependencies.map(_.value),
      internalDependencies.map(_.value),
      fetchRes.fullDetailedArtifacts.collect { case (d, p, a, Some(f)) =>
        (d, p, a, os.Path(f, Os.pwd))
      },
      extraClassPath,
      extraCompileOnlyJars ++ extraStubsJars,
      extraSourceJars,
      scalaOpt,
      addJvmRunner0,
      if (keepResolution) Some(fetchRes.resolution) else None
    )
  }

  private[build] def artifacts(
    dependencies: Positioned[Seq[AnyDependency]],
    extraRepositories: Seq[String],
    paramsOpt: Option[ScalaParameters],
    logger: Logger,
    cache: FileCache[Task],
    classifiersOpt: Option[Set[String]] = None
  ): Either[BuildException, Seq[(String, os.Path)]] = either {
    val res =
      value(fetch(dependencies, extraRepositories, paramsOpt, logger, cache, classifiersOpt))
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

  def fetch(
    dependencies: Positioned[Seq[AnyDependency]],
    extraRepositories: Seq[String],
    paramsOpt: Option[ScalaParameters],
    logger: Logger,
    cache: FileCache[Task],
    classifiersOpt: Option[Set[String]],
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e)
  ): Either[BuildException, Fetch.Result] = either {
    val coursierDependenciesWithFallbacks = value {
      dependencies.value
        .map(Positioned(dependencies.positions, _))
        .map(dep => dep.toCs(paramsOpt).map(csDep => (dep.value, csDep.value)))
        .map(_.left.map(maybeRecoverOnError))
        .flatMap {
          case Left(Some(e: NoScalaVersionProvidedError)) => Some(Left(e))
          case Left(_)                                    => None
          case Right(dep)                                 => Some(Right(dep))
        }
        .sequence
        .left.map(CompositeBuildException(_))
        .map(_.map {
          case (dep, csDep) =>
            val maybeUrl = dep.userParams.get("url").flatten.map(new URL(_))
            val fallback = maybeUrl.map(url => (csDep.module -> csDep.version) -> (url -> true))
            (csDep, fallback)
        })
    }
    val coursierDependenciesWithFallbacks0
      : Positioned[Seq[(CsDependency, Option[((Module, String), (URL, Boolean))])]] =
      dependencies.map(_ => coursierDependenciesWithFallbacks)
    val coursierDependencies: Positioned[Seq[CsDependency]] =
      coursierDependenciesWithFallbacks0.map(_.map(_._1))
    val fallbacks: Map[(Module, String), (URL, Boolean)] =
      coursierDependenciesWithFallbacks0.value.flatMap(_._2).toMap
    value {
      fetch0(
        coursierDependencies,
        extraRepositories,
        paramsOpt.map(_.scalaVersion),
        Nil,
        logger,
        cache,
        classifiersOpt,
        fallbacks
      ).left.flatMap(_.maybeRecoverWithDefault(Fetch.Result(), maybeRecoverOnError))
    }
  }

  def fetch0(
    dependencies: Positioned[Seq[coursier.Dependency]],
    extraRepositories: Seq[String],
    forceScalaVersionOpt: Option[String],
    forcedVersions: Seq[(coursier.Module, String)],
    logger: Logger,
    cache: FileCache[Task],
    classifiersOpt: Option[Set[String]],
    fallbacks: Map[(Module, String), (URL, Boolean)] = Map.empty
  ): Either[BuildException, Fetch.Result] = either {
    logger.debug {
      s"Fetching ${dependencies.value}" +
        (if (extraRepositories.isEmpty) "" else s", adding $extraRepositories")
    }

    val fallbackRepository = TemporaryInMemoryRepository(fallbacks)

    val extraRepositories0 = value {
      RepositoryParser.repositories(extraRepositories)
        .either
        .left.map(errors => new RepositoryFormatError(errors))
    }

    val extraRepositoriesWithFallback = extraRepositories0 :+ fallbackRepository

    val forceScalaVersions = forceScalaVersionOpt match {
      case None => Nil
      case Some(sv) =>
        if (sv.startsWith("2."))
          Seq(
            cmod"org.scala-lang:scala-library"  -> sv,
            cmod"org.scala-lang:scala-compiler" -> sv,
            cmod"org.scala-lang:scala-reflect"  -> sv
          )
        else
          // FIXME Shouldn't we force the org.scala-lang:scala-library version too?
          // (to a 2.13.x version)
          Seq(
            cmod"org.scala-lang:scala3-library_3"         -> sv,
            cmod"org.scala-lang:scala3-compiler_3"        -> sv,
            cmod"org.scala-lang:scala3-interfaces_3"      -> sv,
            cmod"org.scala-lang:scala3-tasty-inspector_3" -> sv,
            cmod"org.scala-lang:tasty-core_3"             -> sv
          )
    }

    val forceVersion = forceScalaVersions ++ forcedVersions

    // FIXME Many parameters that we could allow to customize here
    var fetcher = coursier.Fetch()
      .withCache(cache)
      .addRepositories(extraRepositoriesWithFallback*)
      .addDependencies(dependencies.value*)
      .mapResolutionParams(_.addForceVersion(forceVersion*))
    for (classifiers <- classifiersOpt) {
      if (classifiers("_"))
        fetcher = fetcher.withMainArtifacts()
      fetcher = fetcher
        .addClassifiers(classifiers.toSeq.filter(_ != "_").map(coursier.Classifier(_))*)
    }

    val res = cache.logger.use {
      fetcher.eitherResult()
    }
    value {
      res.left.map(ex => new FetchingDependenciesError(ex, dependencies.positions))
    }
  }
}
