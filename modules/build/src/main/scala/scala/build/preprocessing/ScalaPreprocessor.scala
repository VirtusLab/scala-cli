package scala.build.preprocessing

import com.virtuslab.using_directives.{Context, UsingDirectivesProcessor}
import com.virtuslab.using_directives.config.Settings
import com.virtuslab.using_directives.custom.model.Path
import com.virtuslab.using_directives.reporter.ConsoleReporter
import dependency.AnyDependency
import dependency.parser.DependencyParser

import java.nio.charset.StandardCharsets

import scala.build.{Inputs, Os, Sources}
import scala.build.internal.AmmUtil
import scala.build.options.{BuildOptions, ClassPathOptions}
import scala.collection.JavaConverters._

case object ScalaPreprocessor extends Preprocessor {
  def preprocess(input: Inputs.SingleElement): Option[Seq[PreprocessedSource]] =
    input match {
      case f: Inputs.ScalaFile =>
        val inferredClsName = {
          val (pkg, wrapper) = AmmUtil.pathToPackageWrapper(Nil, f.subPath)
          (pkg :+ wrapper).map(_.raw).mkString(".")
        }
        val source = process(f.path) match {
          case None =>
            PreprocessedSource.OnDisk(f.path, None, Some(inferredClsName))
          case Some((options, updatedCode)) =>
            PreprocessedSource.InMemory(
              Right(f.path),
              f.subPath,
              updatedCode,
              0,
              Some(options),
              Some(inferredClsName)
            )
        }
        Some(Seq(source))

      case v: Inputs.VirtualScalaFile =>
        val content = new String(v.content, StandardCharsets.UTF_8)
        val (options, updatedContent) = process(content, v.source)
          .getOrElse((BuildOptions(), content))
        val s = PreprocessedSource.InMemory(
          Left(v.source),
          v.subPath,
          updatedContent,
          0,
          Some(options),
          None
        )
        Some(Seq(s))

      case _ =>
        None
    }

  def process(path: os.Path): Option[(BuildOptions, String)] = {
    val printablePath =
      if (path.startsWith(Os.pwd)) path.relativeTo(Os.pwd).toString
      else path.toString
    val content = os.read(path)
    process(content, printablePath)
  }
  def process(content: String, printablePath: String): Option[(BuildOptions, String)] = {

    val afterUsing = processUsing(content, printablePath)
    val afterProcessImports = processSpecialImports(
      afterUsing.map(_._2).getOrElse(content),
      printablePath
    )

    if (afterUsing.isEmpty && afterProcessImports.isEmpty) None
    else {
      val allOptions    = afterUsing.map(_._1).toSeq ++ afterProcessImports.map(_._1).toSeq
      val summedOptions = allOptions.foldLeft(BuildOptions())(_ orElse _)
      val lastContent = afterProcessImports
        .map(_._2)
        .orElse(afterUsing.map(_._2))
        .getOrElse(content)
      Some((summedOptions, lastContent))
    }
  }

  private def processUsing(
    content: String,
    printablePath: String
  ): Option[(BuildOptions, String)] = {

    val processor = {
      val reporter = new DirectivesOutputStreamReporter(System.err) // TODO Get that via a logger
      val settings = new Settings
      val context  = new Context(reporter, settings)
      new UsingDirectivesProcessor(context)
    }

    val contentChars = content.toCharArray
    val directives   = processor.extract(contentChars)

    val updatedOptions = DirectivesProcessor.process(directives.getFlattenedMap.asScala.toMap)

    val codeOffset = directives.getCodeOffset()

    val updatedContentOpt =
      if (codeOffset > 0)
        Some {
          val headerBytes = contentChars
            .iterator
            .take(codeOffset)
            .map(c => if (c.isControl) c else ' ')
            .toArray
          val mainBytes = contentChars.drop(codeOffset)
          new String(headerBytes ++ mainBytes)
        }
      else None

    if (updatedContentOpt.isEmpty) None
    else Some((updatedOptions, updatedContentOpt.getOrElse(content)))
  }

  private def processSpecialImports(
    content: String,
    printablePath: String
  ): Option[(BuildOptions, String)] = {

    import fastparse._
    import scalaparse._
    import scala.build.internal.ScalaParse._

    val res = parse(content, Header(_))

    val indicesOrFailingIdx0 = res.fold((_, idx, _) => Left(idx), (value, _) => Right(value))

    val indicesOrErrorMsg = indicesOrFailingIdx0 match {
      case Left(failingIdx) =>
        val newCode = content.take(failingIdx)
        val res1    = parse(newCode, Header(_))
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

    val importTrees = indicesOrErrorMsg
      .right
      .toSeq
      .iterator
      .flatMap(_.iterator)
      .flatMap {
        case (start, end) =>
          val code      = content.substring(start, end) // .trim // meh
          val importRes = parse(code, ImportSplitter(_))
          importRes.fold((_, _, _) => Iterator.empty, (trees, _) => trees.iterator).map { tree =>
            tree.copy(start = start + tree.start, end = start + tree.end)
          }
      }
      .toVector

    val dependencyTrees = importTrees.filter { t =>
      val firstSegmentOpt = t.prefix.headOption
      firstSegmentOpt.contains("$ivy") || firstSegmentOpt.contains("$dep")
    }

    if (dependencyTrees.isEmpty) None
    else {
      // replace statements like
      //   import $ivy.`foo`,
      // by
      //   import $ivy.A   ,
      // Ideally, we should just wipe those statements, and take care of keeping 'import' and ','
      // for standard imports.
      val buf = content.toCharArray
      for (t <- dependencyTrees) {
        val substitute = (t.prefix(0) + ".A").padTo(t.end - t.start, ' ')
        assert(substitute.length == (t.end - t.start))
        System.arraycopy(substitute.toArray, 0, buf, t.start, substitute.length)
      }
      val newCode = new String(buf)
      val deps    = dependencyTrees.map(_.prefix.drop(1).mkString("."))
      val options = BuildOptions(
        classPathOptions = ClassPathOptions(
          extraDependencies = deps.map(parseDependency)
        )
      )
      Some((options, newCode))
    }
  }

  private def parseDependency(str: String): AnyDependency =
    DependencyParser.parse(str) match {
      case Left(msg)  => sys.error(s"Malformed dependency '$str': $msg")
      case Right(dep) => dep
    }
}
