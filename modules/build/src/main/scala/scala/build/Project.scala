package scala.build

import _root_.bloop.config.{Config => BloopConfig, ConfigCodecs => BloopCodecs}
import _root_.coursier.{Dependency => CsDependency, core => csCore, util => csUtil}
import com.github.plokhotnyuk.jsoniter_scala.core.{writeToArray => writeAsJsonToArray}
import coursier.core.Classifier

import java.nio.file.Path
import java.util.Arrays

import scala.build.options.Scope

final case class Project(
  workspace: os.Path,
  directory: os.Path,
  classesDir: os.Path,
  scalaCompiler: ScalaCompiler,
  scalaJsOptions: Option[BloopConfig.JsConfig],
  scalaNativeOptions: Option[BloopConfig.NativeConfig],
  projectName: String,
  classPath: Seq[os.Path],
  sources: Seq[os.Path],
  resolution: Option[BloopConfig.Resolution],
  resourceDirs: Seq[os.Path],
  javaHomeOpt: Option[os.Path],
  scope: Scope,
  javacOptions: List[String]
) {

  import Project._

  def bloopProject: BloopConfig.Project = {
    val platform = (scalaJsOptions, scalaNativeOptions) match {
      case (None, None) =>
        val baseJvmConf = bloopJvmPlatform
        baseJvmConf.copy(
          // We don't pass jvm home here, because it applies only to java files compilation
          config = baseJvmConf.config.copy(home = None)
        )
      case (Some(jsConfig), _) => BloopConfig.Platform.Js(config = jsConfig, mainClass = None)
      case (_, Some(nativeConfig)) =>
        BloopConfig.Platform.Native(config = nativeConfig, mainClass = None)
    }
    val scalaConfig =
      bloopScalaConfig("org.scala-lang", "scala-compiler", scalaCompiler.scalaVersion).copy(
        options = scalaCompiler.scalacOptions.toList,
        jars = scalaCompiler.compilerClassPath.map(_.toNIO).toList
      )
    baseBloopProject(
      projectName,
      directory.toNIO,
      (directory / ".bloop" / projectName).toNIO,
      classesDir.toNIO,
      scope
    )
      .copy(
        workspaceDir = Some(workspace.toNIO),
        classpath = classPath.map(_.toNIO).toList,
        sources = sources.iterator.map(_.toNIO).toList,
        resources = Some(resourceDirs).filter(_.nonEmpty).map(_.iterator.map(_.toNIO).toList),
        platform = Some(platform),
        `scala` = Some(scalaConfig),
        java = Some(BloopConfig.Java(javacOptions)),
        resolution = resolution
      )
  }

  def bloopFile: BloopConfig.File =
    BloopConfig.File(BloopConfig.File.LatestVersion, bloopProject)

  def writeBloopFile(strictCheck: Boolean, logger: Logger): Boolean = {
    lazy val bloopFileContent =
      writeAsJsonToArray(bloopFile)(BloopCodecs.codecFile)
    val dest = directory / ".bloop" / s"$projectName.json"
    val doWrite = !os.isFile(dest) || {
      strictCheck && {
        logger.debug(s"Checking Bloop project in $dest")
        val currentContent = os.read.bytes(dest)
        !Arrays.equals(currentContent, bloopFileContent)
      }
    }
    if (doWrite) {
      logger.debug(s"Writing bloop project in $dest")
      os.write.over(dest, bloopFileContent, createFolders = true)
    }
    else
      logger.debug(s"Bloop project in $dest doesn't need updating")
    doWrite
  }
}

object Project {

  def resolution(
    detailedArtifacts: Seq[(CsDependency, csCore.Publication, csUtil.Artifact, os.Path)]
  ): BloopConfig.Resolution = {
    val indices = detailedArtifacts.map(_._1.moduleVersion).zipWithIndex.toMap
    val modules = detailedArtifacts
      .groupBy(_._1.moduleVersion)
      .toVector
      .sortBy { case (modVer, _) => indices.getOrElse(modVer, Int.MaxValue) }
      .iterator
      .map {
        case ((mod, ver), values) =>
          val artifacts = values.toList.map {
            case (_, pub, _, f) =>
              val classifier =
                if (pub.classifier == Classifier.empty) None
                else Some(pub.classifier.value)
              BloopConfig.Artifact(pub.name, classifier, None, f.toNIO)
          }
          BloopConfig.Module(mod.organization.value, mod.name.value, ver, None, artifacts)
      }
      .toList
    BloopConfig.Resolution(modules)
  }

  private def setProjectTestConfig(p: BloopConfig.Project): BloopConfig.Project =
    p.copy(
      dependencies = List(p.name.stripSuffix("-test")),
      test = Some(
        BloopConfig.Test(
          frameworks = BloopConfig.TestFramework.DefaultFrameworks,
          options = BloopConfig.TestOptions.empty
        )
      ),
      tags = Some(List("test"))
    )

  private def baseBloopProject(
    name: String,
    directory: Path,
    out: Path,
    classesDir: Path,
    scope: Scope
  ): BloopConfig.Project = {
    val project = BloopConfig.Project(
      name = name,
      directory = directory,
      workspaceDir = None,
      sources = Nil,
      sourcesGlobs = None,
      sourceRoots = None,
      dependencies = Nil,
      classpath = Nil,
      out = out,
      classesDir = classesDir,
      resources = None,
      `scala` = None,
      java = None,
      sbt = None,
      test = None,
      platform = None,
      resolution = None,
      tags = Some(List("library"))
    )
    if (scope == Scope.Test)
      setProjectTestConfig(project)
    else project
  }

  private def bloopJvmPlatform: BloopConfig.Platform.Jvm =
    BloopConfig.Platform.Jvm(
      config = BloopConfig.JvmConfig(None, Nil),
      mainClass = None,
      runtimeConfig = None,
      classpath = None,
      resources = None
    )
  private def bloopScalaConfig(
    organization: String,
    name: String,
    version: String
  ): BloopConfig.Scala =
    BloopConfig.Scala(
      organization = organization,
      name = name,
      version = version,
      options = Nil,
      jars = Nil,
      analysis = None,
      setup = None
    )
}
