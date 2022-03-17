package scala.build

import coursier.cache.FileCache
import coursier.core.Classifier
import coursier.parse.RepositoryParser
import coursier.util.Task
import coursier.{Dependency => CsDependency, Fetch, core => csCore, util => csUtil}
import dependency._

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
import scala.build.errors.{
  BuildException,
  CompositeBuildException,
  FetchingDependenciesError,
  RepositoryFormatError
}
import scala.build.internal.Constants
import scala.build.internal.Constants._
import scala.build.internal.CsLoggerUtil._
import scala.build.internal.ScalaParse.scala2NightlyRegex
import scala.build.internal.Util.ScalaDependencyOps

final case class Artifacts(
  compilerDependencies: Seq[AnyDependency],
  compilerArtifacts: Seq[(String, os.Path)],
  compilerPlugins: Seq[(AnyDependency, String, os.Path)],
  javacPluginDependencies: Seq[(AnyDependency, String, os.Path)],
  extraJavacPlugins: Seq[os.Path],
  userDependencies: Seq[AnyDependency],
  internalDependencies: Seq[AnyDependency],
  scalaNativeCli: Seq[os.Path],
  detailedArtifacts: Seq[(CsDependency, csCore.Publication, csUtil.Artifact, os.Path)],
  extraClassPath: Seq[os.Path],
  extraCompileOnlyJars: Seq[os.Path],
  extraSourceJars: Seq[os.Path],
  params: ScalaParameters
) {
  lazy val artifacts: Seq[(String, os.Path)] =
    detailedArtifacts
      .iterator
      .collect {
        case (_, pub, a, f) if pub.classifier != Classifier.sources =>
          (a.url, f)
      }
      .toVector
  lazy val sourceArtifacts: Seq[(String, os.Path)] =
    detailedArtifacts
      .iterator
      .collect {
        case (_, pub, a, f) if pub.classifier == Classifier.sources =>
          (a.url, f)
      }
      .toVector
  lazy val compilerClassPath: Seq[os.Path] =
    compilerArtifacts.map(_._2)
  lazy val classPath: Seq[os.Path] =
    artifacts.map(_._2) ++ extraClassPath
  lazy val compileClassPath: Seq[os.Path] =
    artifacts.map(_._2) ++ extraClassPath ++ extraCompileOnlyJars
  lazy val sourcePath: Seq[os.Path] =
    sourceArtifacts.map(_._2) ++ extraSourceJars
}

object Artifacts {

  def apply(
    params: ScalaParameters,
    compilerPlugins: Seq[Positioned[AnyDependency]],
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
    addClang: Option[Boolean],
    addJsTestBridge: Option[String],
    addNativeTestInterface: Option[String],
    addJmhDependencies: Option[String],
    scalaNativeCliVersion: Option[String],
    extraRepositories: Seq[String],
    cache: FileCache[Task],
    logger: Logger
  ): Either[BuildException, Artifacts] = either {

    if (addClang.getOrElse(false))
      value(CLangInstaller.install(logger))

    val compilerDependencies =
      if (params.scalaVersion.startsWith("3."))
        Seq(
          dep"org.scala-lang::scala3-compiler:${params.scalaVersion}"
        )
      else
        Seq(
          dep"org.scala-lang:scala-compiler:${params.scalaVersion}"
        )
    val compilerDependenciesMessage =
      s"Downloading Scala ${params.scalaVersion} compiler"

    val jvmRunnerDependencies =
      if (addJvmRunner.getOrElse(true))
        Seq(dep"$runnerOrganization::$runnerModuleName:$runnerVersion")
      else
        Nil
    val jvmTestRunnerDependencies =
      if (addJvmTestRunner)
        Seq(dep"$testRunnerOrganization::$testRunnerModuleName:$testRunnerVersion")
      else
        Nil
    val jsTestBridgeDependencies = addJsTestBridge.toSeq.map { scalaJsVersion =>
      if (params.scalaVersion.startsWith("2."))
        dep"org.scala-js::scalajs-test-bridge:$scalaJsVersion"
      else
        dep"org.scala-js:scalajs-test-bridge_2.13:$scalaJsVersion"
    }
    val nativeTestInterfaceDependencies = addNativeTestInterface.toSeq.map { scalaNativeVersion =>
      dep"org.scala-native::test-interface::$scalaNativeVersion"
    }

    val jmhDependencies = addJmhDependencies.toSeq.map { version =>
      dep"org.openjdk.jmh:jmh-generator-bytecode:$version"
    }

    val maybeSnapshotRepo = {
      val hasSnapshots = (jvmRunnerDependencies ++ jvmTestRunnerDependencies)
        .exists(_.version.endsWith("SNAPSHOT")) ||
        scalaNativeCliVersion.exists(_.endsWith("SNAPSHOT"))
      val runnerNeedsSonatypeSnapshots = Constants.runnerNeedsSonatypeSnapshots(params.scalaVersion)
      val stubsNeedSonatypeSnapshots   = addStubs && stubsVersion.endsWith("SNAPSHOT")
      if (hasSnapshots || runnerNeedsSonatypeSnapshots || stubsNeedSonatypeSnapshots)
        Seq(coursier.Repositories.sonatype("snapshots").root)
      else
        Nil
    }

    val scala2NightlyRepo = Seq(coursier.Repositories.scalaIntegration.root)

    val scalaNativeCliDependency =
      scalaNativeCliVersion.map { version =>
        import coursier.moduleString
        Seq(coursier.Dependency(mod"org.scala-native:scala-native-cli_2.12", version))
      }

    val isScala2NightlyRequested = scala2NightlyRegex.unapplySeq(params.scalaVersion).isDefined

    val allExtraRepositories = {
      val baseRepos =
        maybeSnapshotRepo ++ extraRepositories
      if (isScala2NightlyRequested) baseRepos ++ scala2NightlyRepo
      else baseRepos
    }

    val internalDependencies =
      jvmRunnerDependencies.map(Positioned.none(_)) ++
        jvmTestRunnerDependencies.map(Positioned.none(_)) ++
        jsTestBridgeDependencies.map(Positioned.none(_)) ++
        nativeTestInterfaceDependencies.map(Positioned.none(_)) ++
        jmhDependencies.map(Positioned.none(_))
    val updatedDependencies = dependencies ++ internalDependencies

    val updatedDependenciesMessage = {
      val b           = new StringBuilder("Downloading ")
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

    val compilerArtifacts = value {
      artifacts(
        Positioned.none(compilerDependencies),
        allExtraRepositories,
        params,
        logger,
        cache.withMessage(compilerDependenciesMessage)
      )
    }

    val fetchRes = value {
      fetch(
        Positioned.sequence(updatedDependencies),
        allExtraRepositories,
        params,
        logger,
        cache.withMessage(updatedDependenciesMessage),
        classifiersOpt = Some(Set("_") ++ (if (fetchSources) Set("sources") else Set.empty))
      )
    }

    val fetchedScalaNativeCli = scalaNativeCliDependency match {
      case Some(dependency) =>
        Some(
          value {
            fetch0(
              Positioned.none(dependency),
              allExtraRepositories,
              None,
              logger,
              cache.withMessage("Downloading Scala Native CLI"),
              None
            )
          }
        )
      case None =>
        None
    }

    val scalaNativeCli = fetchedScalaNativeCli.toSeq.flatMap { fetched =>
      fetched.fullDetailedArtifacts.collect { case (_, _, _, Some(f)) =>
        os.Path(f, Os.pwd)
      }
    }

    val extraStubsJars =
      if (addStubs)
        value {
          artifacts(
            Positioned.none(Seq(dep"$stubsOrganization:$stubsModuleName:$stubsVersion")),
            allExtraRepositories,
            params,
            logger,
            cache.withMessage("Downloading internal stub dependency")
          ).map(_.map(_._2))
        }
      else
        Nil

    val compilerPlugins0 = value {
      compilerPlugins
        .map { posDep =>
          val posDep0 =
            posDep.map(dep => dep.copy(userParams = dep.userParams + ("intransitive" -> None)))
          artifacts(
            posDep0.map(Seq(_)),
            allExtraRepositories,
            params,
            logger,
            cache.withMessage(s"Downloading compiler plugin ${posDep.value.render}")
          ).map(_.map { case (url, path) => (posDep0.value, url, path) })
        }
        .sequence
        .left.map(CompositeBuildException(_))
        .map(_.flatten)
    }

    val javacPlugins0 = value {
      javacPluginDependencies
        .map { posDep =>
          val cache0 = cache.withMessage(s"Downloading javac plugin ${posDep.value.render}")
          artifacts(posDep.map(Seq(_)), allExtraRepositories, params, logger, cache0)
            .map(_.map { case (url, path) => (posDep.value, url, path) })
        }
        .sequence
        .left.map(CompositeBuildException(_))
        .map(_.flatten)
    }

    Artifacts(
      compilerDependencies,
      compilerArtifacts,
      compilerPlugins0,
      javacPlugins0,
      extraJavacPlugins,
      dependencies.map(_.value),
      internalDependencies.map(_.value),
      scalaNativeCli,
      fetchRes.fullDetailedArtifacts.collect { case (d, p, a, Some(f)) =>
        (d, p, a, os.Path(f, Os.pwd))
      },
      extraClassPath ++ extraStubsJars,
      extraCompileOnlyJars,
      extraSourceJars,
      params
    )
  }

  private[build] def artifacts(
    dependencies: Positioned[Seq[AnyDependency]],
    extraRepositories: Seq[String],
    params: ScalaParameters,
    logger: Logger,
    cache: FileCache[Task],
    classifiersOpt: Option[Set[String]] = None
  ): Either[BuildException, Seq[(String, os.Path)]] = either {
    val res = value(fetch(dependencies, extraRepositories, params, logger, cache, classifiersOpt))
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
    params: ScalaParameters,
    logger: Logger,
    cache: FileCache[Task],
    classifiersOpt: Option[Set[String]]
  ): Either[BuildException, Fetch.Result] =
    fetch0(
      dependencies.map(_.map(_.toCs(params))),
      extraRepositories,
      Some(params.scalaVersion),
      logger,
      cache,
      classifiersOpt
    )

  private def fetch0(
    dependencies: Positioned[Seq[coursier.Dependency]],
    extraRepositories: Seq[String],
    forceScalaVersionOpt: Option[String],
    logger: Logger,
    cache: FileCache[Task],
    classifiersOpt: Option[Set[String]]
  ): Either[BuildException, Fetch.Result] = either {
    logger.debug {
      s"Fetching ${dependencies.value}" +
        (if (extraRepositories.isEmpty) "" else s", adding $extraRepositories")
    }

    val extraRepositories0 = value {
      RepositoryParser.repositories(extraRepositories)
        .either
        .left.map(errors => new RepositoryFormatError(errors))
    }

    val forceVersions = forceScalaVersionOpt match {
      case None => Nil
      case Some(sv) =>
        import coursier.moduleString
        if (sv.startsWith("2."))
          Seq(
            mod"org.scala-lang:scala-library"  -> sv,
            mod"org.scala-lang:scala-compiler" -> sv,
            mod"org.scala-lang:scala-reflect"  -> sv
          )
        else
          // FIXME Shouldn't we force the org.scala-lang:scala-library version too?
          // (to a 2.13.x version)
          Seq(
            mod"org.scala-lang:scala3-library_3"         -> sv,
            mod"org.scala-lang:scala3-compiler_3"        -> sv,
            mod"org.scala-lang:scala3-interfaces_3"      -> sv,
            mod"org.scala-lang:scala3-tasty-inspector_3" -> sv,
            mod"org.scala-lang:tasty-core_3"             -> sv
          )
    }

    // FIXME Many parameters that we could allow to customize here
    var fetcher = coursier.Fetch()
      .withCache(cache)
      .addRepositories(extraRepositories0: _*)
      .addDependencies(dependencies.value: _*)
      .mapResolutionParams { params =>
        params.addForceVersion(forceVersions: _*)
      }
    for (classifiers <- classifiersOpt) {
      if (classifiers("_"))
        fetcher = fetcher.withMainArtifacts()
      fetcher = fetcher
        .addClassifiers(classifiers.toSeq.filter(_ != "_").map(coursier.Classifier(_)): _*)
    }

    val res = cache.logger.use {
      fetcher.eitherResult()
    }
    value {
      res.left.map(ex => new FetchingDependenciesError(ex, dependencies.positions))
    }
  }

}
