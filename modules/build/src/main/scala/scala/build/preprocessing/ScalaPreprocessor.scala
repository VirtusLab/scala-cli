package scala.build.preprocessing

import com.virtuslab.using_directives.custom.model.UsingDirectiveKind
import dependency.AnyDependency
import dependency.parser.DependencyParser

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.errors.*
import scala.build.internal.Util
import scala.build.options.{BuildOptions, BuildRequirements, ClassPathOptions, ShadowingSeq}
import scala.build.preprocessing.directives.*
import scala.build.{Inputs, Logger, Position, Positioned}

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

  val usingDirectiveHandlers: Seq[UsingDirectiveHandler] = Seq(
    UsingCompilerPluginDirectiveHandler,
    UsingCustomJarDirectiveHandler,
    UsingDependencyDirectiveHandler,
    UsingJavaHomeDirectiveHandler,
    UsingJavaOptionsDirectiveHandler,
    UsingJavaPropsDirectiveHandler,
    UsingMainClassDirectiveHandler,
    UsingOptionDirectiveHandler,
    UsingPackagingDirectiveHandler,
    UsingPlatformDirectiveHandler,
    UsingPublishContextualDirectiveHandler,
    UsingPublishDirectiveHandler,
    UsingRepositoryDirectiveHandler,
    UsingResourcesDirectiveHandler,
    UsingScalaJsOptionsDirectiveHandler,
    UsingScalaNativeOptionsDirectiveHandler,
    UsingScalaVersionDirectiveHandler,
    UsingTestFrameworkDirectiveHandler
  )

  val requireDirectiveHandlers: Seq[RequireDirectiveHandler] = Seq(
    RequirePlatformsDirectiveHandler,
    RequireScalaVersionDirectiveHandler,
    RequireScopeDirectiveHandler
  )

  def preprocess(
    input: Inputs.SingleElement,
    logger: Logger
  ): Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case f: Inputs.ScalaFile =>
        val res = either {
          val content   = value(PreprocessingUtil.maybeRead(f.path))
          val scopePath = ScopePath.fromPath(f.path)
          val source = value(process(content, Right(f.path), scopePath / os.up, logger)) match {
            case None =>
              PreprocessedSource.OnDisk(f.path, None, None, Nil, None)
            case Some(ProcessingOutput(
                  requirements,
                  scopedRequirements,
                  options,
                  Some(updatedCode)
                )) =>
              PreprocessedSource.InMemory(
                Right((f.subPath, f.path)),
                f.subPath,
                updatedCode,
                0,
                Some(options),
                Some(requirements),
                scopedRequirements,
                None,
                scopePath
              )
            case Some(ProcessingOutput(requirements, scopedRequirements, options, None)) =>
              PreprocessedSource.OnDisk(
                f.path,
                Some(options),
                Some(requirements),
                scopedRequirements,
                None
              )
          }
          Seq(source)
        }
        Some(res)

      case v: Inputs.VirtualScalaFile =>
        val res = either {
          val relPath = if (v.isStdin) os.sub / "stdin.scala" else v.subPath
          val content = new String(v.content, StandardCharsets.UTF_8)
          val (requirements, scopedRequirements, options, updatedContentOpt) =
            value(
              process(content, Left(v.source), v.scopePath / os.up, logger)
            ).map {
              case ProcessingOutput(reqs, scopedReqs, opts, updatedContent) =>
                (reqs, scopedReqs, opts, updatedContent)
            }.getOrElse((BuildRequirements(), Nil, BuildOptions(), None))
          val s = PreprocessedSource.InMemory(
            originalPath = Left(v.source),
            relPath = relPath,
            updatedContentOpt.getOrElse(content),
            ignoreLen = 0,
            options = Some(options),
            requirements = Some(requirements),
            scopedRequirements,
            mainClassOpt = None,
            scopePath = v.scopePath
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

    import fastparse.*

    import scala.build.internal.ScalaParse.*

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
        val substitute = (t.prefix.head + ".A").padTo(t.end - t.start, ' ')
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
          extraDependencies = ShadowingSeq.from(deps)
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
      value(ExtractedDirectives.from(contentChars, path, logger, UsingDirectiveKind.values(), cwd))

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

    val unusedDirectives = updatedRequirements.unused

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
      unusedDirectives match {
        case Seq() =>
          Right(StrictDirectivesProcessingOutput(
            updatedRequirements.global,
            updatedOptions.global,
            updatedRequirements.scoped,
            updatedContentOpt
          ))
        case Seq(h, t*) =>
          val errors = ::(
            handleUnusedValues(ScopedDirective(h, path, cwd)),
            t.map(d => handleUnusedValues(ScopedDirective(d, path, cwd))).toList
          )
          Left(CompositeBuildException(errors))
      }
    }
  }

  private def handleUnusedValues(
    scopedDirective: ScopedDirective
  ): BuildException = {
    val values =
      DirectiveUtil.concatAllValues(DirectiveUtil.getGroupedValues(scopedDirective))
    new UnusedDirectiveError(
      scopedDirective.directive.key,
      values.map(_.positioned.value),
      values.flatMap(_.positioned.positions)
    )
  }

  val changeToSpecialCommentMsg =
    "Using directive using plain comments are deprecated. Please use a special comment syntax: '//> ...' or '/*> ... */'"

  private def parseDependency(str: String, pos: Position): Either[BuildException, AnyDependency] =
    DependencyParser.parse(str) match {
      case Left(msg)  => Left(new DependencyFormatError(str, msg, positionOpt = Some(pos)))
      case Right(dep) => Right(dep)
    }
}
