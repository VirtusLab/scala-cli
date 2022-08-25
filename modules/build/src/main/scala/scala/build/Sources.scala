package scala.build

import coursier.cache.ArchiveCache
import coursier.util.Task

import scala.build.internal.CodeWrapper
import scala.build.options.{BuildOptions, Scope}
import scala.build.preprocessing.*

final case class Sources(
  paths: Seq[(os.Path, os.RelPath)],
  inMemory: Seq[Sources.InMemory],
  defaultMainClass: Option[String],
  resourceDirs: Seq[os.Path],
  buildOptions: BuildOptions
) {

  def withVirtualDir(inputs: Inputs, scope: Scope, options: BuildOptions): Sources = {

    val srcRootPath = inputs.generatedSrcRoot(scope)
    val resourceDirs0 = options.classPathOptions.resourcesVirtualDir.map { path =>
      srcRootPath / path
    }

    copy(
      resourceDirs = resourceDirs ++ resourceDirs0
    )
  }

  def generateSources(generatedSrcRoot: os.Path): Seq[GeneratedSource] = {
    val generated =
      for (inMemSource <- inMemory) yield {
        os.write.over(
          generatedSrcRoot / inMemSource.generatedRelPath,
          inMemSource.generatedContent.getBytes("UTF-8"),
          createFolders = true
        )
        (
          inMemSource.originalPath.map(_._2),
          inMemSource.generatedRelPath,
          inMemSource.topWrapperLen
        )
      }

    val generatedSet = generated.map(_._2).toSet
    if (os.isDir(generatedSrcRoot))
      os.walk(generatedSrcRoot)
        .filter(os.isFile(_))
        .filter(p => !generatedSet(p.relativeTo(generatedSrcRoot)))
        .foreach(os.remove(_))

    generated.map {
      case (reportingPath, path, topWrapperLen) =>
        GeneratedSource(generatedSrcRoot / path, reportingPath, topWrapperLen)
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
    generatedContent: String,
    topWrapperLen: Int
  )

  /** The default preprocessor list.
    *
    * @param codeWrapper
    *   used by the Scala script preprocessor to "wrap" user code
    * @param archiveCache
    *   used from native launchers by the Java preprocessor, to download a java-class-name binary,
    *   used to infer the class name of unnamed Java sources (like stdin)
    * @param javaClassNameVersionOpt
    *   if using a java-class-name binary, the version we should download. If empty, the default
    *   version is downloaded.
    * @return
    */
  def defaultPreprocessors(
    codeWrapper: CodeWrapper,
    archiveCache: ArchiveCache[Task],
    javaClassNameVersionOpt: Option[String],
    javaCommand: () => String
  ): Seq[Preprocessor] =
    Seq(
      ScriptPreprocessor(codeWrapper),
      MarkdownPreprocessor,
      JavaPreprocessor(archiveCache, javaClassNameVersionOpt, javaCommand),
      ScalaPreprocessor,
      DataPreprocessor
    )
}
