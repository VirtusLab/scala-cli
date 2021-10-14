package scala.build.preprocessing

import com.virtuslab.using_directives.config.Settings
import com.virtuslab.using_directives.{Context, UsingDirectivesProcessor}
import dependency.AnyDependency
import dependency.parser.DependencyParser

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
import scala.build.errors.{
  BuildException,
  CompositeBuildException,
  DependencyFormatError,
  FileNotFoundException,
  UnusedDirectiveError
}
import scala.build.internal.{AmmUtil, Util}
import scala.build.options.{BuildOptions, BuildRequirements, ClassPathOptions}
import scala.build.preprocessing.directives._
import scala.build.{Inputs, Position, Positioned}
import scala.jdk.CollectionConverters._

case object ScalaPreprocessor extends Preprocessor {

  val usingDirectiveHandlers = Seq(
    UsingDependencyDirectiveHandler,
    UsingScalaVersionDirectiveHandler,
    UsingRepositoryDirectiveHandler,
    UsingPlatformDirectiveHandler,
    UsingOptionDirectiveHandler,
    UsingJavaOptionsDirectiveHandler,
    UsingJavaHomeDirectiveHandler,
    UsingTestFrameworkDirectiveHandler,
    UsingCustomJarDirectiveHandler,
    UsingResourcesDirectiveHandler
  )

  val requireDirectiveHandlers = Seq[RequireDirectiveHandler](
    RequireScalaVersionDirectiveHandler,
    RequirePlatformsDirectiveHandler,
    RequireScopeDirectiveHandler
  )

  private def defaultCharSet = StandardCharsets.UTF_8

  private def maybeRead(f: os.Path): Either[BuildException, String] =
    if (os.isFile(f)) Right(os.read(f, defaultCharSet))
    else Left(new FileNotFoundException(f))

  def preprocess(input: Inputs.SingleElement)
    : Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case f: Inputs.ScalaFile =>
        val inferredClsName = {
          val (pkg, wrapper) = AmmUtil.pathToPackageWrapper(f.subPath)
          (pkg :+ wrapper).map(_.raw).mkString(".")
        }
        val res = either {
          val content   = value(maybeRead(f.path))
          val scopePath = ScopePath.fromPath(f.path)
          val source = value(process(content, Right(f.path), scopePath / os.up)) match {
            case None =>
              PreprocessedSource.OnDisk(f.path, None, None, Nil, Some(inferredClsName))
            case Some((requirements, scopedRequirements, options, Some(updatedCode))) =>
              PreprocessedSource.InMemory(
                Right(f.path),
                f.subPath,
                updatedCode,
                0,
                Some(options),
                Some(requirements),
                scopedRequirements,
                Some(inferredClsName),
                scopePath
              )
            case Some((requirements, scopedRequirements, options, None)) =>
              PreprocessedSource.OnDisk(
                f.path,
                Some(options),
                Some(requirements),
                scopedRequirements,
                Some(inferredClsName)
              )
          }
          Seq(source)
        }
        Some(res)

      case v: Inputs.VirtualScalaFile =>
        val res = either {
          val content = new String(v.content, StandardCharsets.UTF_8)
          val (requirements, scopedRequirements, options, updatedContentOpt) =
            value(process(content, Left(v.source), v.scopePath / os.up))
              .getOrElse((BuildRequirements(), Nil, BuildOptions(), None))
          val s = PreprocessedSource.InMemory(
            Left(v.source),
            v.subPath,
            updatedContentOpt.getOrElse(content),
            0,
            Some(options),
            Some(requirements),
            scopedRequirements,
            None,
            v.scopePath
          )
          Seq(s)
        }
        Some(res)

      case _ =>
        None
    }

  def process(
    content: String,
    path: Either[String, os.Path],
    scopeRoot: ScopePath
  ): Either[BuildException, Option[(
    BuildRequirements,
    Seq[Scoped[BuildRequirements]],
    BuildOptions,
    Option[String]
  )]] = either {

    val afterStrictUsing = value(processStrictUsing(content, scopeRoot))
    val afterUsing = value {
      processUsing(path, afterStrictUsing.map(_._2).getOrElse(content), scopeRoot)
        .sequence
    }
    val afterProcessImports = value {
      processSpecialImports(
        afterUsing.flatMap(_._4).orElse(afterStrictUsing.map(_._2)).getOrElse(content),
        path
      )
    }

    if (afterStrictUsing.isEmpty && afterUsing.isEmpty && afterProcessImports.isEmpty) None
    else {
      val allRequirements    = afterUsing.map(_._1).toSeq ++ afterProcessImports.map(_._1).toSeq
      val summedRequirements = allRequirements.foldLeft(BuildRequirements())(_ orElse _)
      val allOptions = afterStrictUsing.map(_._1).toSeq ++
        afterUsing.map(_._3).toSeq ++
        afterProcessImports.map(_._2).toSeq
      val summedOptions = allOptions.foldLeft(BuildOptions())(_ orElse _)
      val lastContentOpt = afterProcessImports
        .map(_._3)
        .orElse(afterUsing.flatMap(_._4))
        .orElse(afterStrictUsing.map(_._2))
      val scopedRequirements = afterUsing.map(_._2).getOrElse(Nil)
      Some((summedRequirements, scopedRequirements, summedOptions, lastContentOpt))
    }
  }

  private def directivesBuildOptions(
    directives: Seq[Directive],
    cwd: ScopePath
  ): Either[BuildException, BuildOptions] = {
    val results = directives
      .filter(_.tpe == Directive.Using)
      .map { dir =>
        val fromHandlersOpt = usingDirectiveHandlers
          .iterator
          .flatMap(_.handle(dir, cwd).iterator)
          .take(1)
          .toList
          .headOption

        fromHandlersOpt.getOrElse {
          Left(new UnusedDirectiveError(dir))
        }
      }

    results
      .sequence
      .left.map(CompositeBuildException(_))
      .map { allOptions =>
        allOptions.foldLeft(BuildOptions())(_ orElse _)
      }
  }

  private def directivesBuildRequirements(
    directives: Seq[Directive],
    scopeRoot: ScopePath
  ): Either[
    BuildException,
    (BuildRequirements, Seq[Scoped[BuildRequirements]])
  ] = {
    val results = directives
      .filter(_.tpe == Directive.Require)
      .map { dir =>
        val fromHandlersOpt = requireDirectiveHandlers
          .iterator
          .flatMap(_.handle(dir, scopeRoot).iterator)
          .take(1)
          .toList
          .headOption

        fromHandlersOpt match {
          case None =>
            Left(new UnusedDirectiveError(dir))
          case Some(Right(reqs)) =>
            val value = dir.scope match {
              case None => (reqs, Nil)
              case Some(sc) =>
                val scopePath = scopeRoot / os.RelPath(sc).asSubPath
                (BuildRequirements(), Seq(Scoped(scopePath, reqs)))
            }
            Right(value)
          case Some(Left(err)) =>
            Left(err)
        }
      }

    results
      .sequence
      .left.map(CompositeBuildException(_))
      .map { allValues =>
        val allReqs       = allValues.map(_._1)
        val allScopedReqs = allValues.flatMap(_._2)
        (allReqs.foldLeft(BuildRequirements())(_ orElse _), allScopedReqs)
      }
  }

  private def processUsing(
    path: Either[String, os.Path],
    content: String,
    scopeRoot: ScopePath
  ): Option[Either[
    BuildException,
    (
      BuildRequirements,
      Seq[Scoped[BuildRequirements]],
      BuildOptions,
      Option[String]
    )
  ]] =
    // TODO Warn about unrecognized directives
    // TODO Report via some diagnostics malformed directives
    TemporaryDirectivesParser.parseDirectives(path, content).map {
      case (directives, updatedContentOpt) =>
        val tuple = (
          directivesBuildRequirements(directives, scopeRoot),
          directivesBuildOptions(directives, scopeRoot),
          Right(updatedContentOpt)
        )
        tuple
          .traverseN
          .left.map(CompositeBuildException(_))
          .map {
            case ((reqs, scopedReqs), options, contentOpt) =>
              (reqs, scopedReqs, options, contentOpt)
          }
    }

  private def processSpecialImports(
    content: String,
    path: Either[String, os.Path]
  ): Either[BuildException, Option[(BuildRequirements, BuildOptions, String)]] = either {

    import fastparse._
    import scala.build.internal.ScalaParse._

    val res = parse(content, Header(_))

    val indicesOrFailingIdx0 = res.fold((_, idx, _) => Left(idx), (value, _) => Right(value))

    val indicesOrErrorMsg = indicesOrFailingIdx0 match {
      case Left(failingIdx) =>
        val newCode = content.take(failingIdx)
        val res1    = parse(newCode, Header(_))
        res1 match {
          case f: Parsed.Failure =>
            val msg = formatFastparseError(Util.printablePath(path), content, f)
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
      (firstSegmentOpt.contains("$ivy") || firstSegmentOpt.contains("$dep")) &&
      t.prefix.lengthCompare(1) > 0
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
      val newCode   = new String(buf)
      val toFilePos = Position.Raw.filePos(path, content)
      val deps = value {
        dependencyTrees
          .map { t =>
            val pos      = toFilePos(Position.Raw(t.start, t.end))
            val strDep   = t.prefix.drop(1).mkString(".")
            val maybeDep = parseDependency(strDep, pos)
            maybeDep.map(dep => Positioned(Seq(pos), dep))
          }
          .sequence
          .left.map(CompositeBuildException(_))
      }
      val options = BuildOptions(
        classPathOptions = ClassPathOptions(
          extraDependencies = deps
        )
      )
      Some((BuildRequirements(), options, newCode))
    }
  }

  private def processStrictUsing(
    content: String,
    cwd: ScopePath
  ): Either[BuildException, Option[(BuildOptions, String)]] = either {

    val processor = {
      val reporter = new DirectivesOutputStreamReporter(System.err) // TODO Get that via a logger
      val settings = new Settings
      settings.setAllowStartWithoutAt(false)
      settings.setAllowRequire(false)
      val context = new Context(reporter, settings)
      new UsingDirectivesProcessor(context)
    }

    val contentChars = content.toCharArray
    val directives   = processor.extract(contentChars)

    val directives0 = directives
      .getFlattenedMap
      .asScala
      .map {
        case (k, l) => k -> l.asScala
      }
      .toMap
    val updatedOptions = value {
      DirectivesProcessor.process(
        directives0,
        usingDirectiveHandlers ++ requireDirectiveHandlers,
        cwd
      )
    }
    val codeOffset = directives.getCodeOffset()
    val updatedContentOpt =
      if (codeOffset > 0) {
        val headerBytes = contentChars
          .iterator
          .take(codeOffset)
          .map(c => if (c.isControl) c else ' ')
          .toArray
        val mainBytes      = contentChars.drop(codeOffset)
        val updatedContent = new String(headerBytes ++ mainBytes)
        if (updatedContent == content) None
        else Some(updatedContent)
      }
      else None

    if (updatedContentOpt.isEmpty) None
    else Some((updatedOptions, updatedContentOpt.getOrElse(content)))
  }

  private def parseDependency(str: String, pos: Position): Either[BuildException, AnyDependency] =
    DependencyParser.parse(str) match {
      case Left(msg)  => Left(new DependencyFormatError(str, msg, positionOpt = Some(pos)))
      case Right(dep) => Right(dep)
    }
}
