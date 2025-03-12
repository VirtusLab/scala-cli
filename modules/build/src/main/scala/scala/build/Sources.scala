package scala.build

import coursier.cache.ArchiveCache
import coursier.util.Task

import java.nio.charset.StandardCharsets

import scala.build.info.BuildInfo
import scala.build.input.Inputs
import scala.build.internal.{CodeWrapper, WrapperParams}
import scala.build.options.{BuildOptions, Scope}
import scala.build.preprocessing.*

final case class Sources(
  paths: Seq[(os.Path, os.RelPath)],
  inMemory: Seq[Sources.InMemory],
  defaultMainClass: Option[String],
  resourceDirs: Seq[os.Path],
  buildOptions: BuildOptions
) {

  def withExtraSources(other: Sources): Sources =
    copy(
      paths = paths ++ other.paths,
      inMemory = inMemory ++ other.inMemory,
      resourceDirs = resourceDirs ++ other.resourceDirs
    )

  def withVirtualDir(inputs: Inputs, scope: Scope, options: BuildOptions): Sources = {

    val srcRootPath = inputs.generatedSrcRoot(scope)
    val resourceDirs0 = options.classPathOptions.resourcesVirtualDir.map { path =>
      srcRootPath / path
    }

    copy(
      resourceDirs = resourceDirs ++ resourceDirs0
    )
  }

  /** Write all in-memory sources to disk.
    *
    * @param generatedSrcRoot
    *   the root directory where the sources should be written
    */
  def generateSources(generatedSrcRoot: os.Path): Seq[GeneratedSource] = {
    val generated =
      for (inMemSource <- inMemory) yield {
        os.write.over(
          generatedSrcRoot / inMemSource.generatedRelPath,
          inMemSource.content,
          createFolders = true
        )
        (
          inMemSource.originalPath.map(_._2),
          inMemSource.generatedRelPath,
          inMemSource.wrapperParamsOpt
        )
      }

    val generatedSet = generated.map(_._2).toSet
    if (os.isDir(generatedSrcRoot))
      os.walk(generatedSrcRoot)
        .filter(os.isFile(_))
        .filter(p => !generatedSet(p.relativeTo(generatedSrcRoot)))
        .foreach(os.remove(_))

    generated.map {
      case (reportingPath, path, wrapperParamsOpt) =>
        GeneratedSource(generatedSrcRoot / path, reportingPath, wrapperParamsOpt)
    }
  }

  lazy val hasJava =
    (paths.iterator.map(_._1.last) ++ inMemory.iterator.map(_.generatedRelPath.last))
      .exists(_.endsWith(".java"))
  lazy val hasScala =
    (paths.iterator.map(_._1.last) ++ inMemory.iterator.map(_.generatedRelPath.last))
      .exists(_.endsWith(".scala"))
}

object Sources {

  final case class InMemory(
    originalPath: Either[String, (os.SubPath, os.Path)],
    generatedRelPath: os.RelPath,
    content: Array[Byte],
    wrapperParamsOpt: Option[WrapperParams]
  )

  final case class UnwrappedScript(
    originalPath: Either[String, (os.SubPath, os.Path)],
    generatedRelPath: os.RelPath,
    wrapScriptFun: CodeWrapper => (String, WrapperParams)
  ) {
    def wrap(wrapper: CodeWrapper): InMemory = {
      val (content, wrapperParams) = wrapScriptFun(wrapper)
      InMemory(
        originalPath,
        generatedRelPath,
        content.getBytes(StandardCharsets.UTF_8),
        Some(wrapperParams)
      )
    }
  }

  /** The default preprocessor list.
    *
    * @param archiveCache
    *   used from native launchers by the Java preprocessor, to download a java-class-name binary,
    *   used to infer the class name of unnamed Java sources (like stdin)
    * @param javaClassNameVersionOpt
    *   if using a java-class-name binary, the version we should download. If empty, the default
    *   version is downloaded.
    * @return
    */
  def defaultPreprocessors(
    archiveCache: ArchiveCache[Task],
    javaClassNameVersionOpt: Option[String],
    javaCommand: () => String
  ): Seq[Preprocessor] =
    Seq(
      ScriptPreprocessor,
      MarkdownPreprocessor,
      JavaPreprocessor(archiveCache, javaClassNameVersionOpt, javaCommand),
      ScalaPreprocessor,
      DataPreprocessor,
      JarPreprocessor
    )
}
