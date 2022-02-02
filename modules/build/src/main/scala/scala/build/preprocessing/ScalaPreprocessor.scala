package scala.build.preprocessing

import com.virtuslab.using_directives.config.Settings
import com.virtuslab.using_directives.custom.model.{
  UsingDirectiveKind,
  UsingDirectiveSyntax,
  UsingDirectives
}
import com.virtuslab.using_directives.custom.utils.ast.{UsingDef, UsingDefs}
import com.virtuslab.using_directives.{Context, UsingDirectivesProcessor}
import dependency.AnyDependency
import dependency.parser.DependencyParser

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException, DependencyFormatError}
import scala.build.internal.{AmmUtil, Util}
import scala.build.options.{BuildOptions, BuildRequirements, ClassPathOptions}
import scala.build.preprocessing.directives._
import scala.build.{Inputs, Logger, Position, Positioned}
import scala.jdk.CollectionConverters._
import scala.build.options.collections.BuildOptionsConverterImplicits._

case object ScalaPreprocessor extends Preprocessor {

  private case class StrictDirectivesProcessingOutput(
    globalReqs: BuildRequirements,
    globalUsings: BuildOptions,
    scopedReqs: Seq[Scoped[BuildRequirements]],
    strippedContent: Option[String]
  ) {
    def isEmpty: Boolean = globalReqs == BuildRequirements.monoid.zero &&
      globalUsings == BuildOptions.monoid.zero &&
      scopedReqs.isEmpty &&
      strippedContent.isEmpty
  }

  private case class SpecialImportsProcessingOutput(
    reqs: BuildRequirements,
    opts: BuildOptions,
    content: String
  )

  case class ProcessingOutput(
    globalReqs: BuildRequirements,
    scopedReqs: Seq[Scoped[BuildRequirements]],
    opts: BuildOptions,
    updatedContent: Option[String]
  )

  val usingDirectiveHandlers = Seq(
    UsingDependencyDirectiveHandler,
    UsingScalaVersionDirectiveHandler,
    UsingRepositoryDirectiveHandler,
    UsingPlatformDirectiveHandler,
    UsingOptionDirectiveHandler,
    UsingJavaOptionsDirectiveHandler,
    UsingJavaPropsDirectiveHandler,
    UsingScalaJsOptionsDirectiveHandler,
    UsingScalaNativeOptionsDirectiveHandler,
    UsingJavaHomeDirectiveHandler,
    UsingTestFrameworkDirectiveHandler,
    UsingCustomJarDirectiveHandler,
    UsingResourcesDirectiveHandler,
    UsingCompilerPluginDirectiveHandler,
    UsingMainClassDirectiveHandler
  )

  val requireDirectiveHandlers = Seq[RequireDirectiveHandler](
    RequireScalaVersionDirectiveHandler,
    RequirePlatformsDirectiveHandler,
    RequireScopeDirectiveHandler
  )

  def preprocess(
    input: Inputs.SingleElement,
    logger: Logger
  ): Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case f: Inputs.ScalaFile =>
        val inferredClsName = {
          val (pkg, wrapper) = AmmUtil.pathToPackageWrapper(f.subPath)
          (pkg :+ wrapper).map(_.raw).mkString(".")
        }
        val res = either {
          val content   = value(PreprocessingUtil.maybeRead(f.path))
          val scopePath = ScopePath.fromPath(f.path)
          val source = value(process(content, Right(f.path), scopePath / os.up, logger)) match {
            case None =>
              PreprocessedSource.OnDisk(f.path, None, None, Nil, Some(inferredClsName))
            case Some(ProcessingOutput(
                  requirements,
                  scopedRequirements,
                  options,
                  Some(updatedCode)
                )) =>
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
            case Some(ProcessingOutput(requirements, scopedRequirements, options, None)) =>
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
            value(
              process(content, Left(v.source), v.scopePath / os.up, logger)
            ).map {
              case ProcessingOutput(reqs, scopedReqs, opts, updatedContent) =>
                (reqs, scopedReqs, opts, updatedContent)
            }.getOrElse((BuildRequirements(), Nil, BuildOptions(), None))
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
    scopeRoot: ScopePath,
    logger: Logger
  ): Either[BuildException, Option[ProcessingOutput]] = either {
    val (content0, isSheBang) = SheBang.ignoreSheBangLines(content)
    val afterStrictUsing: StrictDirectivesProcessingOutput =
      value(processStrictUsing(content0, path, scopeRoot, logger))

    val afterProcessImports: Option[SpecialImportsProcessingOutput] = value {
      processSpecialImports(
        afterStrictUsing.strippedContent.getOrElse(content0),
        path
      )
    }

    if (afterStrictUsing.isEmpty && afterProcessImports.isEmpty) None
    else {
      val allRequirements    = afterProcessImports.map(_.reqs).toSeq :+ afterStrictUsing.globalReqs
      val summedRequirements = allRequirements.foldLeft(BuildRequirements())(_ orElse _)
      val allOptions = afterStrictUsing.globalUsings +:
        afterProcessImports.map(_.opts).toSeq
      val summedOptions = allOptions.foldLeft(BuildOptions())(_ orElse _)
      val lastContentOpt = afterProcessImports
        .map(_.content)
        .orElse(afterStrictUsing.strippedContent)
        .orElse(if (isSheBang) Some(content0) else None)

      val scopedRequirements = afterStrictUsing.scopedReqs
      Some(ProcessingOutput(summedRequirements, scopedRequirements, summedOptions, lastContentOpt))
    }
  }

  private def processSpecialImports(
    content: String,
    path: Either[String, os.Path]
  ): Either[BuildException, Option[SpecialImportsProcessingOutput]] = either {

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
          extraDependencies = deps.toDependencyMap()
        )
      )
      Some(SpecialImportsProcessingOutput(BuildRequirements(), options, newCode))
    }
  }

  private def processStrictUsing(
    content: String,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger
  ): Either[BuildException, StrictDirectivesProcessingOutput] = either {

    val contentChars = content.toCharArray
    val ExtractedDirectives(codeOffset, directives0) =
      value(extractUsingDirectives(contentChars, path, logger))

    val updatedOptions = value {
      DirectivesProcessor.process(
        directives0,
        usingDirectiveHandlers,
        path,
        cwd,
        logger
      )
    }

    val directives1 = updatedOptions.unused

    val updatedRequirements = value {
      DirectivesProcessor.process(
        directives1,
        requireDirectiveHandlers,
        path,
        cwd,
        logger
      )
    }

    val directives2 = updatedRequirements.unused

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

    value {
      if (directives2.nonEmpty) {
        val errors = directives2.map(d =>
          new DefaultDirectiveHandler[Nothing].handleValues(d, path, cwd, logger)
        ).collect {
          case Left(e) => e
        }
        Left(CompositeBuildException(errors))
      }
      else
        Right(StrictDirectivesProcessingOutput(
          updatedRequirements.global,
          updatedOptions.global,
          updatedRequirements.scoped,
          updatedContentOpt
        ))
    }
  }

  case class ExtractedDirectives(offset: Int, directives: Seq[StrictDirective])

  val changeToSpecialCommentMsg =
    "Using directive using plain comments are deprecated. Please use a special comment syntax: '//> ...' or '/*> ... */'"

  def extractUsingDirectives(
    contentChars: Array[Char],
    path: Either[String, os.Path],
    logger: Logger
  ): Either[BuildException, ExtractedDirectives] = {
    val processor = {
      val reporter = new DirectivesOutputStreamReporter(System.err) // TODO Get that via a logger
      val settings = new Settings
      settings.setAllowStartWithoutAt(true)
      settings.setAllowRequire(false)
      val context = new Context(reporter, settings)
      new UsingDirectivesProcessor(context)
    }
    val all = processor.extract(contentChars, true, true).asScala

    def byKind(kind: UsingDirectiveKind) = all.find(_.getKind == kind).get

    def getDirectives(directives: UsingDirectives) =
      directives.getAst() match {
        case ud: UsingDefs =>
          ud.getUsingDefs().asScala
        case _ =>
          Nil
      }

    val codeDirectives           = byKind(UsingDirectiveKind.Code)
    val specialCommentDirectives = byKind(UsingDirectiveKind.SpecialComment)
    val plainCommentDirectives   = byKind(UsingDirectiveKind.PlainComment)

    def reportWarning(msg: String, values: Seq[UsingDef], before: Boolean = true): Unit =
      values.foreach { v =>
        val astPos = v.getPosition()
        val (start, end) =
          if (before) (0, astPos.getColumn())
          else (astPos.getColumn(), astPos.getColumn() + v.getSyntax.getKeyword.size)
        val position = Position.File(path, (astPos.getLine(), start), (astPos.getLine(), end))
        logger.diagnostic(msg, positions = Seq(position))
      }

    val usedDirectives =
      if (!codeDirectives.getFlattenedMap().isEmpty()) {
        val msg =
          s"This using directive is ignored. File contains directives outside comments and those has higher precedence."
        reportWarning(
          msg,
          getDirectives(plainCommentDirectives) ++ getDirectives(specialCommentDirectives)
        )
        codeDirectives
      }
      else if (!specialCommentDirectives.getFlattenedMap().isEmpty()) {
        val msg =
          s"This using directive is ignored. $changeToSpecialCommentMsg"
        reportWarning(msg, getDirectives(plainCommentDirectives))
        specialCommentDirectives
      }
      else {
        reportWarning(changeToSpecialCommentMsg, getDirectives(plainCommentDirectives))
        plainCommentDirectives
      }

    // All using directives should use just `using` keyword, no @using or require
    reportWarning(
      "Deprecated using directive syntax, please use keyword `using`.",
      getDirectives(usedDirectives).filter(_.getSyntax() != UsingDirectiveSyntax.Using),
      before = false
    )

    val flattened = usedDirectives.getFlattenedMap.asScala.toSeq
    val strictDirectives =
      flattened.map { case (k, l) => StrictDirective(k.getPath.asScala.mkString("."), l.asScala) }

    val offset =
      if (usedDirectives.getKind() != UsingDirectiveKind.Code) 0
      else usedDirectives.getCodeOffset()
    Right(ExtractedDirectives(offset, strictDirectives))
  }

  private def parseDependency(str: String, pos: Position): Either[BuildException, AnyDependency] =
    DependencyParser.parse(str) match {
      case Left(msg)  => Left(new DependencyFormatError(str, msg, positionOpt = Some(pos)))
      case Right(dep) => Right(dep)
    }
}
