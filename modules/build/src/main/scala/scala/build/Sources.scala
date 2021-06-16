package scala.build

import java.nio.charset.StandardCharsets
import java.nio.file.Paths

import dependency.parser.DependencyParser
import dependency.{AnyDependency, ScalaParameters}

import scala.build.internal.{AmmUtil, CodeWrapper, Name}
import scala.build.internal.Util.DependencyOps
import pureconfig.ConfigSource
import scala.build.config.ConfigFormat

final case class Sources(
  paths: Seq[(os.Path, os.RelPath)],
  inMemory: Seq[(os.Path, os.RelPath, String, Int)],
  mainClass: Option[String],
  dependencies: Seq[AnyDependency],
  resourceDirs: Seq[os.Path],
  buildOptions: BuildOptions
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
    val printablePath =
      if (path.startsWith(Os.pwd)) path.relativeTo(Os.pwd).toString
      else path.toString
    val content = os.read(path)
    process(content, printablePath)
  }
  private def process(content: String, printablePath: String): Option[(Seq[String], String)] = {

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
            val msg = formatFastparseError(printablePath, content, f)
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

  private def parseDependency(str: String): AnyDependency =
    DependencyParser.parse(str) match {
      case Left(msg) => sys.error(s"Malformed dependency '$str': $msg")
      case Right(dep) => dep
    }

  private def scriptData(codeWrapper: CodeWrapper, script: Inputs.Script): ScriptData = {

    val (pkg, wrapper) = AmmUtil.pathToPackageWrapper(Nil, script.subPath)

    val (deps, updatedCode) = process(script.path).getOrElse((Nil, os.read(script.path)))

    val (code, topWrapperLen, _) = codeWrapper.wrapCode(
      pkg,
      wrapper,
      updatedCode
    )

    val deps0 = deps.map(parseDependency)

    val className = (pkg :+ wrapper).map(_.raw).mkString(".")
    ScriptData(script.path, className, code, deps0, topWrapperLen)
  }

  private def virtualScriptData(codeWrapper: CodeWrapper, script: Inputs.VirtualScript): ScriptData = {

    val (pkg, wrapper) = AmmUtil.pathToPackageWrapper(Nil, os.rel / "stdin.sc")

    val content = new String(script.content, StandardCharsets.UTF_8)
    val (deps, updatedCode) = process(content, "<stdin>").getOrElse((Nil, content))

    val (code, topWrapperLen, _) = codeWrapper.wrapCode(
      pkg,
      wrapper,
      updatedCode
    )

    val deps0 = deps.map(parseDependency)

    val className = (pkg :+ wrapper).map(_.raw).mkString(".")
    ScriptData(os.pwd / "<stdin>", className, code, deps0, topWrapperLen)
  }

  def forInputs(
    inputs: Inputs,
    codeWrapper: CodeWrapper
  ): Sources = {

    val singleFiles = inputs.singleFiles()
    val virtualSourceFiles = inputs.virtualSourceFiles()

    val configFiles = singleFiles.collect {
      case c: Inputs.ConfigFile => c.path
    }
    val configOptions = configFiles.flatMap { f =>
      if (os.isFile(f)) {
        val source = ConfigSource.string(os.read(f))
        source.load[ConfigFormat] match {
          case Left(err) =>
            sys.error(s"Parsing $f:" + err.prettyPrint(indentLevel = 2))
          case Right(conf) =>
            Seq(conf.buildOptions)
        }
      }
      else Nil
    }
    val buildOptions = configOptions.foldLeft(BuildOptions())(_.orElse(_))

    val scalaFilePathsOrCode = singleFiles
      .iterator
      .collect {
        case f: Inputs.ScalaFile => f
      }
      .flatMap { f =>
        process(f.path) match {
          case None =>
            Iterator(Right(f.path))
          case Some((deps, updatedCode)) =>
            Iterator(Left((f.path, deps, f.path.relativeTo(inputs.workspace), updatedCode)))
        }
      }
      .toVector

    val fromVirtualSources = virtualSourceFiles.map {
      case v: Inputs.VirtualScalaFile =>
        val content = new String(v.content, StandardCharsets.UTF_8)
        val (deps, updatedContent) = process(content, "<stdin>").getOrElse((Nil, content))
        (os.pwd / "<stdin>", deps, os.rel / "stdin.scala", updatedContent)
      case v: Inputs.VirtualScript =>
        val content = new String(v.content, StandardCharsets.UTF_8)
        val (deps, updatedContent) = process(content, "<stdin>").getOrElse((Nil, content))
        (os.pwd / "<stdin>", deps, os.rel / "stdin.scala", updatedContent)
    }

    val javaFilePaths = singleFiles
      .iterator
      .collect {
        case f: Inputs.JavaFile =>
          f.path
      }
      .toVector

    val scalaFilePaths = scalaFilePathsOrCode.collect { case Right(v) => v }
    val inMemoryScalaFiles =
      fromVirtualSources.map {
        case (originalPath, _, relPath, updatedCode) =>
          (originalPath, relPath, updatedCode, 0)
      } ++
      scalaFilePathsOrCode.collect {
        case Left((originalPath, _, relPath, updatedCode)) =>
          (originalPath, relPath, updatedCode, 0)
      }

    val scalaFilesDependencies = {
      val depStrings =
        fromVirtualSources.flatMap(_._2) ++
        scalaFilePathsOrCode
          .collect {
            case Left((_, deps, _, _)) => deps
          }
          .flatten

      depStrings.map(parseDependency)
    }

    val mainClassOpt = inputs.mainClassElement.collect {
      case s: Inputs.ScalaFile if s.path.last.endsWith(".scala") => // TODO ignore case for the suffix?
        val (pkg, wrapper) = AmmUtil.pathToPackageWrapper(Nil, s.subPath)
        (pkg :+ wrapper).map(_.raw).mkString(".")
      case s: Inputs.Script =>
        scriptData(codeWrapper, s).className
    }

    val allScriptData =
      singleFiles
        .iterator
        .collect {
          case s: Inputs.Script =>
            scriptData(codeWrapper, s)
        }
        .toVector ++
      virtualSourceFiles
        .iterator
        .collect {
          case s: Inputs.VirtualScript =>
            virtualScriptData(codeWrapper, s)
        }

    Sources(
      paths = (javaFilePaths ++ scalaFilePaths).map(p => (p, p.relativeTo(inputs.workspace))),
      inMemory = inMemoryScalaFiles ++ allScriptData.map(script => (script.reportingPath, script.relPath, script.code, script.topWrapperLen)),
      mainClass = mainClassOpt,
      dependencies = (scalaFilesDependencies ++ allScriptData.flatMap(_.dependencies)).distinct,
      resourceDirs = inputs.elements.collect {
        case r: Inputs.ResourceDirectory => r.path
      },
      buildOptions = buildOptions
    )
  }

  private final case class ScriptData(
    reportingPath: os.Path,
    className: String,
    code: String,
    dependencies: Seq[AnyDependency],
    topWrapperLen: Int
  ) {
    def relPath: os.RelPath = {
      val components = className.split('.')
      os.rel / components.init.toSeq / s"${components.last}.scala"
    }
  }

}
