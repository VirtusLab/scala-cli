package scala.cli

import _root_.bloop.config.{Config => BloopConfig, ConfigCodecs => BloopCodecs}
import com.github.plokhotnyuk.jsoniter_scala.core.{writeToArray => writeAsJsonToArray}

import java.nio.file.{Path, Paths}
import java.util.Arrays

final case class Project(
  workspace: os.Path,
  classesDir: os.Path,
  javaHome: os.Path,
  scalaCompiler: ScalaCompiler,
  scalaJsOptions: Option[Project.ScalaJsOptions],
  scalaNativeOptions: Option[Project.ScalaNativeOptions],
  projectName: String,
  classPath: Seq[Path],
  sources: Seq[os.Path],
  resourceDirs: Seq[os.Path]
) {

  import Project._

  def bloopProject: BloopConfig.Project = {
    val platform = (scalaJsOptions, scalaNativeOptions) match {
      case (None, None) => bloopJvmPlatform(javaHome.toNIO)
      case (Some(jsOptions), _) => bloopJsPlatform(jsOptions)
      case (_, Some(nativeOptions)) => bloopNativePlatform(nativeOptions)
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

  def writeBloopFile(logger: Logger): os.Path = {
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
    dest
  }
}

object Project {

  final case class ScalaJsOptions(
    version: String,
    mode: String
  )

  final case class ScalaNativeOptions(
    version: String,
    mode: String,
    gc: String,
    clang: Path,
    clangpp: Path,
    linkingOptions: Seq[String],
    compileOptions: Seq[String]
  )

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
  private def bloopJsConfig(config: ScalaJsOptions): BloopConfig.JsConfig =
    BloopConfig.JsConfig(
           version = config.version,
              mode = if (config.mode == "release") BloopConfig.LinkerMode.Release else BloopConfig.LinkerMode.Debug,
              kind = BloopConfig.ModuleKindJS.CommonJSModule,
    emitSourceMaps = false,
             jsdom = None,
            output = None,
          nodePath = None,
         toolchain = Nil
    )
  private def bloopNativeConfig(config: ScalaNativeOptions): BloopConfig.NativeConfig =
    BloopConfig.NativeConfig(
           version = config.version,
                     // there are more modes than bloop allows, but that setting here shouldn't end up being used anyway
              mode = if (config.mode == "release") BloopConfig.LinkerMode.Release else BloopConfig.LinkerMode.Debug,
                gc = config.gc,
      targetTriple = None,
             clang = config.clang,
           clangpp = config.clangpp,
         toolchain = Nil,
           options = BloopConfig.NativeOptions(
               linker = config.linkingOptions.toList,
             compiler = config.compileOptions.toList
           ),
         linkStubs = false,
             check = false,
              dump = false,
            output = None
    )
  private def bloopJsPlatform(config: ScalaJsOptions): BloopConfig.Platform.Js =
    BloopConfig.Platform.Js(
      config = bloopJsConfig(config),
      mainClass = None
    )
  private def bloopNativePlatform(config: ScalaNativeOptions): BloopConfig.Platform.Native =
    BloopConfig.Platform.Native(
      config = bloopNativeConfig(config),
      mainClass = None
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
