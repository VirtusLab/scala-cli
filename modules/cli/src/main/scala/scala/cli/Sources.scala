package scala.cli

import java.nio.file.Paths

import ammonite.util.{Name, Util}
import scala.cli.internal.CodeWrapper

final case class Sources(
  paths: Seq[String],
  inMemory: Seq[(String, String, Int)],
  mainClass: Option[String],
  dependencies: Seq[coursierapi.Dependency],
  resourceDirs: Seq[os.Path]
) {

  def generateSources(generatedSrcRoot: os.Path): Seq[(os.Path, Int)] = {
    val generated =
      for ((className, code, topWrapperLen) <- inMemory) yield {
        val components = className.split('.')
        val pkg = components.init
        val simpleName = components.last
        val srcDest = os.rel / "main" / "scala" / pkg / s"$simpleName.scala"
        os.write.over(generatedSrcRoot / srcDest, code.getBytes("UTF-8"), createFolders = true)
        (srcDest, topWrapperLen)
      }

    val generatedSet = generated.map(_._1).toSet
    if (os.isDir(generatedSrcRoot))
      os.walk(generatedSrcRoot)
        .filter(os.isFile(_))
        .filter(p => !generatedSet(p.relativeTo(generatedSrcRoot)))
        .foreach(os.remove(_))

    generated.map {
      case (path, topWrapperLen) =>
        (generatedSrcRoot / path, topWrapperLen)
    }
  }
}

object Sources {

  private def process(path: os.Path): Option[(Seq[String], String)] = {
    val content = os.read(path)

    import fastparse._
    import scalaparse._
    import scala.cli.internal.ScalaParse._

    val res = parse(content, Header(_))

    val indicesOrFailingIdx0 = res.fold((_, idx, _) => Left(idx), (value, _) => Right(value))

    val indicesOrErrorMsg = indicesOrFailingIdx0 match {
      case Left(failingIdx) =>
        val newCode = content.take(failingIdx)
        val res1 = parse(newCode, Header(_))
        res1 match {
          case f: Parsed.Failure =>
            val msg = formatFastparseError(path.relativeTo(os.pwd).toString, content, f)
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

    val splitted = str.split(":", -1)
    // TODO Move that logic elsewhere (in coursier? coursier-interface?)
    splitted match {
      case Array(org, "", name, ver) if org.nonEmpty && name.nonEmpty && ver.nonEmpty =>
        coursierapi.Dependency.of(org, name + "_" + scalaBinaryVersion, ver)
      case Array(org, "", "", name, ver) if org.nonEmpty && name.nonEmpty && ver.nonEmpty =>
        coursierapi.Dependency.of(org, name + "_" + scalaVersion, ver)
      case Array(org, "", name, "", ver) if org.nonEmpty && name.nonEmpty && ver.nonEmpty =>
        coursierapi.Dependency.of(org, name + platformSuffix + "_" + scalaBinaryVersion, ver)
      case Array(org, "", "", name, "", ver) if org.nonEmpty && name.nonEmpty && ver.nonEmpty =>
        coursierapi.Dependency.of(org, name + platformSuffix + "_" + scalaVersion, ver)
      case Array(org, name, ver) if org.nonEmpty && name.nonEmpty && ver.nonEmpty =>
        coursierapi.Dependency.of(org, name, ver)
      case _ =>
        pprint.log(str)
        pprint.log(splitted)
        ???
    }
  }

  def apply(
    inputRoot: os.Path,
    inputs: Inputs,
    codeWrapper: CodeWrapper,
    platformSuffix: String,
    scalaVersion: String,
    scalaBinaryVersion: String
  ): Sources = {

    def scriptData(script: Inputs.Script): ScriptData = {

      val scriptPath = os.Path(inputRoot.toNIO.resolve(script.path).toAbsolutePath.normalize)

      val root = script.relativeTo
        .map(inputRoot.toNIO.resolve(_).toAbsolutePath.normalize)
        .map(os.Path(_))
        .getOrElse(inputRoot)
      val (pkg, wrapper) = Util.pathToPackageWrapper(Nil, scriptPath.relativeTo(root))

      val (deps, updatedCode) = process(scriptPath) match {
        case None => (Nil, os.read(scriptPath))
        case Some((deps, updatedCode)) => (deps, updatedCode)
      }

      val (code, topWrapperLen, _) = codeWrapper.wrapCode(
        Seq(Name("ammonite"), Name("$file")) ++ pkg,
        wrapper,
        updatedCode
      )

      val deps0 = deps.map(parseDependency(_, platformSuffix, scalaVersion, scalaBinaryVersion))

      ScriptData(wrapper.raw, code, deps0, topWrapperLen)
    }

    val fromDirectories = inputs.elements
      .collect {
        case d: Inputs.Directory =>
          val dir = os.FilePath(d.path).resolveFrom(inputs.cwd)
          os.walk.stream(dir)
            .filter { p =>
              !p.relativeTo(dir).segments.exists(_.startsWith("."))
            }
            .filter(os.isFile(_))
            .collect {
              case p if p.last.endsWith(".java") =>
                Inputs.JavaFile(p.relativeTo(inputRoot).toString)
              case p if p.last.endsWith(".scala") =>
                Inputs.ScalaFile(p.relativeTo(inputRoot).toString)
              case p if p.last.endsWith(".sc") =>
                Inputs.Script(p.relativeTo(inputRoot).toString, Some(d.path))
            }
            .toVector
      }
      .flatten

    val scalaFilePathsOrCode = (inputs.elements.iterator ++ fromDirectories.iterator)
      .collect {
        case f: Inputs.ScalaFile =>
          process(os.Path(inputRoot.toNIO.resolve(f.path).toAbsolutePath)) match {
            case None => Iterator(Right(f.path))
            case Some((deps, updatedCode)) =>
              // TODO Ensure f.path is relative, splitting might have issues on Windows
              Iterator(Left((deps, f.path.stripSuffix(".scala").split("/").mkString("."), updatedCode)))
          }
      }
      .flatten
      .toVector

    val javaFilePaths = (inputs.elements.iterator ++ fromDirectories.iterator)
      .collect {
        case f: Inputs.JavaFile => f.path
      }
      .toVector

    val scalaFilePaths = scalaFilePathsOrCode.collect { case Right(v) => v }
    val inMemoryScalaFiles = scalaFilePathsOrCode.collect {
      case Left((deps, relPath, updatedCode)) =>
        (relPath, updatedCode, 0)
    }

    val scalaFilesDependencies = scalaFilePathsOrCode
      .collect {
        case Left((deps, _, _)) => deps
      }
      .flatten
      .map(str => parseDependency(str, platformSuffix, scalaVersion, scalaBinaryVersion))

    val headScriptDataOpt = inputs.elements.headOption.collect {
      case s: Inputs.Script => scriptData(s)
    }
    def otherScriptsData = (inputs.elements.iterator.drop(1) ++ fromDirectories.iterator).collect {
      case s: Inputs.Script => scriptData(s)
    }
    val allScriptData = (headScriptDataOpt.iterator ++ otherScriptsData).toVector

    Sources(
      paths = javaFilePaths ++ scalaFilePaths,
      inMemory = inMemoryScalaFiles ++ allScriptData.map(script => (script.className, script.code, script.topWrapperLen)),
      mainClass = headScriptDataOpt.map(_.className),
      dependencies = (scalaFilesDependencies ++ allScriptData.flatMap(_.dependencies)).distinct,
      resourceDirs = inputs.elements.collect {
        case r: Inputs.ResourceDirectory =>
          os.FilePath(r.path).resolveFrom(inputs.cwd)
      }
    )
  }

  private final case class ScriptData(
    className: String,
    code: String,
    dependencies: Seq[coursierapi.Dependency],
    topWrapperLen: Int
  )

}
