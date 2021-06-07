package scala.build

import java.nio.file.Paths

import ammonite.util.{Name, Util}
import dependency.parser.DependencyParser
import dependency.ScalaParameters

import scala.build.internal.CodeWrapper
import scala.build.internal.Util.DependencyOps

final case class Sources(
  paths: Seq[os.Path],
  inMemory: Seq[(os.Path, os.RelPath, String, Int)],
  mainClass: Option[String],
  dependencies: Seq[coursierapi.Dependency],
  resourceDirs: Seq[os.Path]
) {

  def generateSources(generatedSrcRoot: os.Path): Seq[(os.Path, os.Path, Int)] = {
    val generated =
      for ((reportingPath, relPath, code, topWrapperLen) <- inMemory) yield {
        os.write.over(generatedSrcRoot / relPath, code.getBytes("UTF-8"), createFolders = true)
        (reportingPath, relPath, topWrapperLen)
      }

    val generatedSet = generated.map(_._2).toSet
    if (os.isDir(generatedSrcRoot))
      os.walk(generatedSrcRoot)
        .filter(os.isFile(_))
        .filter(p => !generatedSet(p.relativeTo(generatedSrcRoot)))
        .foreach(os.remove(_))

    generated.map {
      case (reportingPath, path, topWrapperLen) =>
        (generatedSrcRoot / path, reportingPath, topWrapperLen)
    }
  }
}

object Sources {

  private def process(path: os.Path): Option[(Seq[String], String)] = {
    val content = os.read(path)

    import fastparse._
    import scalaparse._
    import scala.build.internal.ScalaParse._

    val res = parse(content, Header(_))

    val indicesOrFailingIdx0 = res.fold((_, idx, _) => Left(idx), (value, _) => Right(value))

    val indicesOrErrorMsg = indicesOrFailingIdx0 match {
      case Left(failingIdx) =>
        val newCode = content.take(failingIdx)
        val res1 = parse(newCode, Header(_))
        res1 match {
          case f: Parsed.Failure =>
            val msg = formatFastparseError(path.relativeTo(Os.pwd).toString, content, f)
            Left(msg)
          case s: Parsed.Success[Seq[(Int, Int)]] =>
            Right(s.value)
        }
      case Right(ind) =>
        Right(ind)
    }

    // TODO Report error if indicesOrErrorMsg.isLeft?

    val importTrees = indicesOrErrorMsg.right.toSeq.iterator.flatMap(_.iterator).flatMap {
      case (start, end) =>
        val code = content.substring(start, end)
          // .trim // meh
        val importRes = parse(code, ImportSplitter(_))
        importRes.fold((_, _, _) => Iterator.empty, (trees, _) => trees.iterator).map { tree =>
          tree.copy(start = start + tree.start, end = start + tree.end)
        }
    }.toVector

    val dependencyTrees = importTrees.filter(_.prefix.headOption.contains("$ivy"))

    if (dependencyTrees.isEmpty) None
    else {
      // replace statements like
      //   import $ivy.`foo`,
      // by
      //   import $ivy.$   ,
      // Ideally, we should just wipe those statements, and take care of keeping 'import' and ','
      // for standard imports.
      val buf = content.toCharArray
      for (t <- dependencyTrees) {
        val substitute = (t.prefix(0) + ".$").padTo(t.end - t.start, ' ')
        assert(substitute.length == (t.end - t.start))
        System.arraycopy(substitute.toArray, 0, buf, t.start, substitute.length)
      }
      val newCode = new String(buf)
      Some((dependencyTrees.map(_.prefix.drop(1).mkString(".")), newCode))
    }
  }

  private def parseDependency(str: String, platformSuffix: String, scalaVersion: String, scalaBinaryVersion: String): coursierapi.Dependency = {
    val anyDep = DependencyParser.parse(str) match {
      case Left(msg) => sys.error(s"Malformed dependency '$str': $msg")
      case Right(dep) => dep
    }
    val params = ScalaParameters(scalaVersion, scalaBinaryVersion, Some(platformSuffix).filter(_.nonEmpty))
    anyDep.applyParams(params).toApi
  }

  private def scriptData(
    workspace: os.Path,
    codeWrapper: CodeWrapper,
    script: Inputs.Script,
    platformSuffix: String,
    scalaVersion: String,
    scalaBinaryVersion: String
  ): ScriptData = {

    val root = script.relativeTo.getOrElse(workspace)
    val (pkg, wrapper) = Util.pathToPackageWrapper(Nil, script.path.relativeTo(root))

    val (deps, updatedCode) = process(script.path) match {
      case None => (Nil, os.read(script.path))
      case Some((deps, updatedCode)) => (deps, updatedCode)
    }

    val (code, topWrapperLen, _) = codeWrapper.wrapCode(
      pkg,
      wrapper,
      updatedCode
    )

    val deps0 = deps.map(parseDependency(_, platformSuffix, scalaVersion, scalaBinaryVersion))

    val className = (pkg :+ wrapper).map(_.raw).mkString(".")
    ScriptData(script.path, className, code, deps0, topWrapperLen)
  }

  def forInputs(
    inputs: Inputs,
    codeWrapper: CodeWrapper,
    platformSuffix: String,
    scalaVersion: String,
    scalaBinaryVersion: String
  ): Sources = {

    val sourceFiles = inputs.sourceFiles()

    val scalaFilePathsOrCode = sourceFiles
      .iterator
      .collect {
        case f: Inputs.ScalaFile => f
      }
      .flatMap { f =>
        process(f.path) match {
          case None => Iterator(Right(f.path))
          case Some((deps, updatedCode)) =>
            val relPath = f.path.relativeTo(f.relativeTo.getOrElse(inputs.workspace))
            Iterator(Left((f.path, deps, relPath, updatedCode)))
        }
      }
      .toVector

    val javaFilePaths = sourceFiles
      .iterator
      .collect {
        case f: Inputs.JavaFile => f.path
      }
      .toVector

    val scalaFilePaths = scalaFilePathsOrCode.collect { case Right(v) => v }
    val inMemoryScalaFiles = scalaFilePathsOrCode.collect {
      case Left((originalPath, deps, relPath, updatedCode)) =>
        (originalPath, relPath, updatedCode, 0)
    }

    val scalaFilesDependencies = scalaFilePathsOrCode
      .collect {
        case Left((_, deps, _, _)) => deps
      }
      .flatten
      .map(str => parseDependency(str, platformSuffix, scalaVersion, scalaBinaryVersion))

    val mainClassOpt = inputs.mainClassElement.collect {
      case s: Inputs.ScalaFile if s.path.last.endsWith(".scala") => // TODO ignore case for the suffix?
        val (pkg, wrapper) = Util.pathToPackageWrapper(Nil, s.path.relativeTo(s.relativeTo.getOrElse(inputs.workspace)))
        (pkg :+ wrapper).map(_.raw).mkString(".")
      case s: Inputs.Script =>
        scriptData(inputs.workspace, codeWrapper, s, platformSuffix, scalaVersion, scalaBinaryVersion).className
    }

    val allScriptData = sourceFiles
      .iterator
      .collect {
        case s: Inputs.Script => scriptData(inputs.workspace, codeWrapper, s, platformSuffix, scalaVersion, scalaBinaryVersion)
      }
      .toVector

    Sources(
      paths = javaFilePaths ++ scalaFilePaths,
      inMemory = inMemoryScalaFiles ++ allScriptData.map(script => (script.reportingPath, script.relPath, script.code, script.topWrapperLen)),
      mainClass = mainClassOpt,
      dependencies = (scalaFilesDependencies ++ allScriptData.flatMap(_.dependencies)).distinct,
      resourceDirs = inputs.elements.collect {
        case r: Inputs.ResourceDirectory => r.path
      }
    )
  }

  private final case class ScriptData(
    reportingPath: os.Path,
    className: String,
    code: String,
    dependencies: Seq[coursierapi.Dependency],
    topWrapperLen: Int
  ) {
    def relPath: os.RelPath = {
      val components = className.split('.')
      os.rel / components.init.toSeq / s"${components.last}.scala"
    }
  }

}
