package scala.build.preprocessing

import com.virtuslab.using_directives.custom.model.UsingDirectiveKind
import dependency.AnyDependency
import dependency.parser.DependencyParser

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.errors.*
import scala.build.input.{Inputs, ScalaFile, SingleElement, VirtualScalaFile}
import scala.build.internal.Util
import scala.build.options.{BuildOptions, BuildRequirements, ClassPathOptions, ShadowingSeq}
import scala.build.preprocessing.directives.*
import scala.build.{Logger, Position, Positioned}

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
    UsingJavacOptionsDirectiveHandler,
    UsingJavaOptionsDirectiveHandler,
    UsingJavaPropsDirectiveHandler,
    UsingJvmDirectiveHandler,
    UsingMainClassDirectiveHandler,
    UsingOptionDirectiveHandler,
    UsingPackagingDirectiveHandler,
    UsingPlatformDirectiveHandler,
    UsingPublishContextualDirectiveHandler,
    UsingPublishDirectiveHandler,
    UsingPythonDirectiveHandler,
    UsingRepositoryDirectiveHandler,
    UsingResourcesDirectiveHandler,
    UsingScalaJsOptionsDirectiveHandler,
    UsingScalaNativeOptionsDirectiveHandler,
    UsingScalaVersionDirectiveHandler,
    UsingSourceDirectiveHandler,
    UsingTestFrameworkDirectiveHandler
  )

  val requireDirectiveHandlers: Seq[RequireDirectiveHandler] = Seq(
    RequirePlatformsDirectiveHandler,
    RequireScalaVersionDirectiveHandler,
    RequireScopeDirectiveHandler
  )

  def preprocess(
    input: SingleElement,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e),
    allowRestrictedFeatures: Boolean
  ): Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case f: ScalaFile =>
        val res = either {
          val content   = value(PreprocessingUtil.maybeRead(f.path))
          val scopePath = ScopePath.fromPath(f.path)
          val source =
            value(
              process(
                content,
                Right(f.path),
                scopePath / os.up,
                logger,
                maybeRecoverOnError,
                allowRestrictedFeatures
              )
            ) match {
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

      case v: VirtualScalaFile =>
        val res = either {
          val relPath = v match {
            case v if !v.isStdin && !v.isSnippet => v.subPath
            case v                               => os.sub / v.generatedSourceFileName
          }
          val content = new String(v.content, StandardCharsets.UTF_8)
          val (requirements, scopedRequirements, options, updatedContentOpt) =
            value(
              process(
                content,
                Left(v.source),
                v.scopePath / os.up,
                logger,
                maybeRecoverOnError,
                allowRestrictedFeatures
              )
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
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException],
    allowRestrictedFeatures: Boolean
  ): Either[BuildException, Option[ProcessingOutput]] = either {
    val extractedDirectives = value(ExtractedDirectives.from(
      content.toCharArray,
      path,
      logger,
      UsingDirectiveKind.values(),
      scopeRoot,
      maybeRecoverOnError
    ))
    value(process(
      content,
      extractedDirectives,
      path,
      scopeRoot,
      logger,
      maybeRecoverOnError,
      allowRestrictedFeatures
    ))
  }

  def process(
    content: String,
    extractedDirectives: ExtractedDirectives,
    path: Either[String, os.Path],
    scopeRoot: ScopePath,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException],
    allowRestrictedFeatures: Boolean
  ): Either[BuildException, Option[ProcessingOutput]] = either {
    val (content0, isSheBang) = SheBang.ignoreSheBangLines(content)
    val afterStrictUsing: StrictDirectivesProcessingOutput =
      value(processStrictUsing(
        content0,
        extractedDirectives,
        path,
        scopeRoot,
        logger,
        allowRestrictedFeatures
      ))

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
          .map { t => /// skip ivy ($ivy.`) or dep syntax ($dep.`)
            val pos      = toFilePos(Position.Raw(t.start + "$ivy.`".length, t.end))
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
    extractedDirectives: ExtractedDirectives,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger,
    allowRestrictedFeatures: Boolean
  ): Either[BuildException, StrictDirectivesProcessingOutput] = either {
    val contentChars                                 = content.toCharArray
    val ExtractedDirectives(codeOffset, directives0) = extractedDirectives

    val updatedOptions = value {
      DirectivesProcessor.process(
        directives0,
        usingDirectiveHandlers,
        path,
        cwd,
        logger,
        allowRestrictedFeatures
      )
    }

    val directives1 = updatedOptions.unused

    val updatedRequirements = value {
      DirectivesProcessor.process(
        directives1,
        requireDirectiveHandlers,
        path,
        cwd,
        logger,
        allowRestrictedFeatures
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
