package scala.build

import _root_.bloop.config.{Config => BloopConfig, ConfigCodecs => BloopCodecs}
import com.github.plokhotnyuk.jsoniter_scala.core.{writeToArray => writeAsJsonToArray}

import java.nio.file.{Path, Paths}
import java.util.Arrays

final case class Project(
  workspace: os.Path,
  classesDir: os.Path,
  javaHome: os.Path,
  scalaCompiler: ScalaCompiler,
  scalaJsOptions: Option[BloopConfig.JsConfig],
  scalaNativeOptions: Option[BloopConfig.NativeConfig],
  projectName: String,
  classPath: Seq[Path],
  sources: Seq[os.Path],
  resourceDirs: Seq[os.Path]
) {

  import Project._

  def bloopProject: BloopConfig.Project = {
    val platform = (scalaJsOptions, scalaNativeOptions) match {
      case (None, None) => bloopJvmPlatform(javaHome.toNIO)
      case (Some(jsConfig), _) => BloopConfig.Platform.Js(config = jsConfig, mainClass = None)
      case (_, Some(nativeConfig)) => BloopConfig.Platform.Native(config = nativeConfig, mainClass = None)
    }
    val scalaConfig = bloopScalaConfig("org.scala-lang", "scala-compiler", scalaCompiler.scalaVersion).copy(
      options = scalaCompiler.scalacOptions.toList,
      jars = scalaCompiler.compilerClassPath.toList
    )
    baseBloopProject(
      projectName,
      workspace.toNIO,
      (workspace / ".scala" / ".bloop" / projectName).toNIO,
      classesDir.toNIO
    )
    .copy(
      workspaceDir = Some(workspace.toNIO),
      classpath = classPath.toList,
      sources = sources.iterator.map(_.toNIO).toList,
      resources = Some(resourceDirs).filter(_.nonEmpty).map(_.iterator.map(_.toNIO).toList),
      platform = Some(platform),
      `scala` = Some(scalaConfig),
      java = Some(BloopConfig.Java(Nil))
    )
  }

  def bloopFile: BloopConfig.File =
    BloopConfig.File(BloopConfig.File.LatestVersion, bloopProject)

  def writeBloopFile(logger: Logger): Boolean = {
    val bloopFileContent = writeAsJsonToArray(bloopFile)(BloopCodecs.codecFile)
    val dest = workspace / ".scala" / ".bloop" / s"$projectName.json"
    val doWrite = !os.isFile(dest) || {
      val currentContent = os.read.bytes(dest)
      !Arrays.equals(currentContent, bloopFileContent)
    }
    if (doWrite) {
      logger.debug(s"Writing bloop project in $dest")
      os.write.over(dest, bloopFileContent, createFolders = true)
    } else
      logger.debug(s"Bloop project in $dest doesn't need updating")
    doWrite
  }
}

object Project {

  private def baseBloopProject(name: String, directory: Path, out: Path, classesDir: Path): BloopConfig.Project =
    BloopConfig.Project(
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
      tags = None
    )
  private def bloopJvmPlatform(javaHome: Path): BloopConfig.Platform.Jvm =
    BloopConfig.Platform.Jvm(
      config = BloopConfig.JvmConfig(Some(javaHome), Nil),
      mainClass = None,
      runtimeConfig = None,
      classpath = None,
      resources = None
    )
  private def bloopScalaConfig(organization: String, name: String, version: String): BloopConfig.Scala =
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
