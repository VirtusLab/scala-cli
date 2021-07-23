package scala.build

import scala.build.internal.CodeWrapper
import scala.build.options.BuildOptions
import scala.build.preprocessing._

final case class Sources(
  paths: Seq[(os.Path, os.RelPath)],
  inMemory: Seq[(Either[String, os.Path], os.RelPath, String, Int)],
  mainClass: Option[String],
  resourceDirs: Seq[os.Path],
  buildOptions: BuildOptions
) {

  def generateSources(generatedSrcRoot: os.Path): Seq[GeneratedSource] = {
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
        GeneratedSource(generatedSrcRoot / path, reportingPath, topWrapperLen)
    }
  }
}

object Sources {

  def process(path: os.Path): Option[(Seq[String], String)] = {
    val printablePath =
      if (path.startsWith(Os.pwd)) path.relativeTo(Os.pwd).toString
      else path.toString
    val content = os.read(path)
    process(content, printablePath)
  }
  def process(content: String, printablePath: String): Option[(Seq[String], String)] = {

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

    val dependencyTrees = importTrees.filter { t =>
      val firstSegmentOpt = t.prefix.headOption
      firstSegmentOpt.contains("$ivy") || firstSegmentOpt.contains("$dep")
    }

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
        val substitute = (t.prefix(0) + ".A").padTo(t.end - t.start, ' ')
        assert(substitute.length == (t.end - t.start))
        System.arraycopy(substitute.toArray, 0, buf, t.start, substitute.length)
      }
      val newCode = new String(buf)
      Some((dependencyTrees.map(_.prefix.drop(1).mkString(".")), newCode))
    }
  }


  def defaultPreprocessors(codeWrapper: CodeWrapper): Seq[Preprocessor] =
    Seq(
      ScriptPreprocessor(codeWrapper),
      JavaPreprocessor,
      ConfigPreprocessor,
      ScalaFilePreprocessor
    )

  def forInputs(
    inputs: Inputs,
    preprocessors: Seq[Preprocessor]
  ): Sources = {

    val preprocessedSources = inputs.flattened().flatMap { elem =>
      preprocessors.iterator.flatMap(p => p.preprocess(elem).iterator).toStream.headOption
        .getOrElse(Nil) // FIXME Warn about unprocessed stuff?
    }

    val buildOptions = preprocessedSources
      .flatMap(_.options.toSeq)
      .foldLeft(BuildOptions())(_.orElse(_))

    val mainClassOpt = inputs.mainClassElement
      .collect {
        case elem: Inputs.SingleElement =>
          preprocessors.iterator
            .flatMap(p => p.preprocess(elem).iterator)
            .toStream.headOption
            .getOrElse(Nil)
            .flatMap(_.mainClassOpt.toSeq)
            .headOption
      }
      .flatten

    val paths = preprocessedSources.collect {
      case d: PreprocessedSource.OnDisk =>
        (d.path, d.path.relativeTo(inputs.workspace))
    }
    val inMemory = preprocessedSources.collect {
      case m: PreprocessedSource.InMemory =>
        (m.reportingPath, m.relPath, m.code, m.ignoreLen)
    }

    val resourceDirs = inputs.elements.collect {
      case r: Inputs.ResourceDirectory =>
        r.path
    }

    Sources(paths, inMemory, mainClassOpt, resourceDirs, buildOptions)
  }
}
