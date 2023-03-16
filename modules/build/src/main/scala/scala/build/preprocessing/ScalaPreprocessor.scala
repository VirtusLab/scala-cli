package scala.build.preprocessing

import com.virtuslab.using_directives.custom.model.UsingDirectiveKind
import dependency.AnyDependency
import dependency.parser.DependencyParser

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.directives.{HasBuildOptions, HasBuildRequirements}
import scala.build.errors.*
import scala.build.input.{Inputs, ScalaFile, SingleElement, VirtualScalaFile}
import scala.build.internal.Util
import scala.build.options.{
  BuildOptions,
  BuildRequirements,
  ClassPathOptions,
  ShadowingSeq,
  SuppressWarningOptions
}
import scala.build.preprocessing.directives
import scala.build.preprocessing.directives.{DirectiveHandler, DirectiveUtil, ScopedDirective}
import scala.build.{Logger, Position, Positioned}

case object ScalaPreprocessor extends Preprocessor {

  private case class StrictDirectivesProcessingOutput(
    globalReqs: BuildRequirements,
    globalUsings: BuildOptions,
    scopedReqs: Seq[Scoped[BuildRequirements]],
    strippedContent: Option[String],
    directivesPositions: Option[DirectivesPositions]
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
    updatedContent: Option[String],
    directivesPositions: Option[DirectivesPositions]
  )

  val usingDirectiveHandlers: Seq[DirectiveHandler[BuildOptions]] =
    Seq[DirectiveHandler[_ <: HasBuildOptions]](
      directives.CustomJar.handler,
      directives.Dependency.handler,
      directives.JavacOptions.handler,
      directives.JavaOptions.handler,
      directives.JavaProps.handler,
      directives.JavaHome.handler,
      directives.Jvm.handler,
      directives.MainClass.handler,
      directives.Packaging.handler,
      directives.Platform.handler,
      directives.Plugin.handler,
      directives.Publish.handler,
      directives.PublishContextual.Local.handler,
      directives.PublishContextual.CI.handler,
      directives.Python.handler,
      directives.Repository.handler,
      directives.Resources.handler,
      directives.ScalacOptions.handler,
      directives.ScalaJs.handler,
      directives.ScalaNative.handler,
      directives.ScalaVersion.handler,
      directives.Sources.handler,
      directives.Tests.handler,
      directives.Toolkit.handler
    ).map(_.mapE(_.buildOptions))

  val requireDirectiveHandlers: Seq[DirectiveHandler[BuildRequirements]] =
    Seq[DirectiveHandler[_ <: HasBuildRequirements]](
      directives.RequirePlatform.handler,
      directives.RequireScalaVersion.handler,
      directives.RequireScalaVersionBounds.handler,
      directives.RequireScope.handler
    ).map(_.mapE(_.buildRequirements))

  def preprocess(
    input: SingleElement,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e),
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
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
                allowRestrictedFeatures,
                suppressWarningOptions
              )
            ) match {
              case None =>
                PreprocessedSource.OnDisk(f.path, None, None, Nil, None, None)
              case Some(ProcessingOutput(
                    requirements,
                    scopedRequirements,
                    options,
                    Some(updatedCode),
                    directivesPositions
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
                  scopePath,
                  directivesPositions
                )
              case Some(ProcessingOutput(
                    requirements,
                    scopedRequirements,
                    options,
                    None,
                    directivesPositions
                  )) =>
                PreprocessedSource.OnDisk(
                  f.path,
                  Some(options),
                  Some(requirements),
                  scopedRequirements,
                  None,
                  directivesPositions
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
          val (requirements, scopedRequirements, options, updatedContentOpt, directivesPositions) =
            value(
              process(
                content,
                Left(v.source),
                v.scopePath / os.up,
                logger,
                maybeRecoverOnError,
                allowRestrictedFeatures,
                suppressWarningOptions
              )
            ).map {
              case ProcessingOutput(reqs, scopedReqs, opts, updatedContent, dirsPositions) =>
                (reqs, scopedReqs, opts, updatedContent, dirsPositions)
            }.getOrElse((BuildRequirements(), Nil, BuildOptions(), None, None))
          val s = PreprocessedSource.InMemory(
            originalPath = Left(v.source),
            relPath = relPath,
            updatedContentOpt.getOrElse(content),
            ignoreLen = 0,
            options = Some(options),
            requirements = Some(requirements),
            scopedRequirements,
            mainClassOpt = None,
            scopePath = v.scopePath,
            directivesPositions = directivesPositions
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
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  ): Either[BuildException, Option[ProcessingOutput]] = either {
    val (contentWithNoShebang, _) = SheBang.ignoreSheBangLines(content)
    val extractedDirectives = value(ExtractedDirectives.from(
      contentWithNoShebang.toCharArray,
      path,
      logger,
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
      allowRestrictedFeatures,
      suppressWarningOptions
    ))
  }

  def process(
    content: String,
    extractedDirectives: ExtractedDirectives,
    path: Either[String, os.Path],
    scopeRoot: ScopePath,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException],
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  ): Either[BuildException, Option[ProcessingOutput]] = either {
    val (content0, isSheBang) = SheBang.ignoreSheBangLines(content)
    val afterStrictUsing: StrictDirectivesProcessingOutput =
      value(processStrictUsing(
        content0,
        extractedDirectives,
        path,
        scopeRoot,
        logger,
        allowRestrictedFeatures,
        suppressWarningOptions
      ))

    value {
      checkForAmmoniteImports(
        afterStrictUsing.strippedContent.getOrElse(content0),
        path
      )
    }

    if (afterStrictUsing.isEmpty) None
    else {
      val allRequirements    = Seq(afterStrictUsing.globalReqs)
      val summedRequirements = allRequirements.foldLeft(BuildRequirements())(_ orElse _)
      val allOptions         = Seq(afterStrictUsing.globalUsings)
      val summedOptions      = allOptions.foldLeft(BuildOptions())(_ orElse _)
      val lastContentOpt = afterStrictUsing.strippedContent
        .orElse(if (isSheBang) Some(content0) else None)
      val directivesPositions = afterStrictUsing.directivesPositions

      val scopedRequirements = afterStrictUsing.scopedReqs
      Some(ProcessingOutput(
        summedRequirements,
        scopedRequirements,
        summedOptions,
        lastContentOpt,
        directivesPositions
      ))
    }
  }

  private def checkForAmmoniteImports(
    content: String,
    path: Either[String, os.Path]
  ): Either[BuildException, Unit] = {

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
            (start, tree.copy(start = start + tree.start, end = start + tree.end))
          }
      }
      .toVector

    val dependencyTrees = importTrees.filter { case (_, t) =>
      val firstSegmentOpt = t.prefix.headOption
      (firstSegmentOpt.contains("$ivy") || firstSegmentOpt.contains("$dep")) &&
      t.prefix.lengthCompare(1) > 0
    }

    if (dependencyTrees.nonEmpty) {
      val toFilePos = Position.Raw.filePos(path, content)
      val exceptions = for {
        (importStart, t) <- dependencyTrees
        pos           = toFilePos(Position.Raw(importStart, t.end))
        dep           = t.prefix.drop(1).mkString(".")
        newImportText = s"//> using dep \"$dep\""
      } yield new UnsupportedAmmoniteImportError(Seq(pos), newImportText)

      Left(CompositeBuildException(exceptions))
    }
    else Right(())
  }

  private def processStrictUsing(
    content: String,
    extractedDirectives: ExtractedDirectives,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger,
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  ): Either[BuildException, StrictDirectivesProcessingOutput] = either {
    val contentChars = content.toCharArray

    val ExtractedDirectives(directives0, directivesPositions) = extractedDirectives

    val updatedOptions = value {
      DirectivesProcessor.process(
        directives0,
        usingDirectiveHandlers,
        path,
        cwd,
        logger,
        allowRestrictedFeatures,
        suppressWarningOptions
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
        allowRestrictedFeatures,
        suppressWarningOptions
      )
    }

    val unusedDirectives = updatedRequirements.unused

    value {
      unusedDirectives match {
        case Seq() =>
          Right(StrictDirectivesProcessingOutput(
            updatedRequirements.global,
            updatedOptions.global,
            updatedRequirements.scoped,
            strippedContent = None,
            directivesPositions
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
      DirectiveUtil.concatAllValues(scopedDirective)
    new UnusedDirectiveError(
      scopedDirective.directive.key,
      values.map(_.value),
      values.flatMap(_.positions)
    )
  }

  private def parseDependency(str: String, pos: Position): Either[BuildException, AnyDependency] =
    DependencyParser.parse(str) match {
      case Left(msg)  => Left(new DependencyFormatError(str, msg, positionOpt = Some(pos)))
      case Right(dep) => Right(dep)
    }
}
