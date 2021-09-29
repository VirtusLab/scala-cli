package scala.build.preprocessing

import dependency.AnyDependency
import dependency.parser.DependencyParser

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
import scala.build.errors.{
  BuildException,
  CompositeBuildException,
  InvalidDirectiveError,
  UnusedDirectiveError
}
import scala.build.internal.AmmUtil
import scala.build.options.{BuildOptions, BuildRequirements, ClassPathOptions}
import scala.build.preprocessing.directives._
import scala.build.{Inputs, Os}

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
    RequirePlatformsDirectiveHandler
  )

  def preprocess(input: Inputs.SingleElement)
    : Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case f: Inputs.ScalaFile =>
        val inferredClsName = {
          val (pkg, wrapper) = AmmUtil.pathToPackageWrapper(f.subPath)
          (pkg :+ wrapper).map(_.raw).mkString(".")
        }
        val res = either {
          val printablePath =
            if (f.path.startsWith(Os.pwd)) f.path.relativeTo(Os.pwd).toString
            else f.path.toString
          val content = os.read(f.path)
          val source = value(process(content, printablePath)) match {
            case None =>
              PreprocessedSource.OnDisk(f.path, None, None, Some(inferredClsName))
            case Some((requirements, options, Some(updatedCode))) =>
              PreprocessedSource.InMemory(
                Right(f.path),
                f.subPath,
                updatedCode,
                0,
                Some(options),
                Some(requirements),
                Some(inferredClsName)
              )
            case Some((requirements, options, None)) =>
              PreprocessedSource.OnDisk(
                f.path,
                Some(options),
                Some(requirements),
                Some(inferredClsName)
              )
          }
          Seq(source)
        }
        Some(res)

      case v: Inputs.VirtualScalaFile =>
        val res = either {
          val content = new String(v.content, StandardCharsets.UTF_8)
          val (requirements, options, updatedContentOpt) = value(process(content, v.source))
            .getOrElse((BuildRequirements(), BuildOptions(), None))
          val s = PreprocessedSource.InMemory(
            Left(v.source),
            v.subPath,
            updatedContentOpt.getOrElse(content),
            0,
            Some(options),
            Some(requirements),
            None
          )
          Seq(s)
        }
        Some(res)

      case _ =>
        None
    }

  def process(
    content: String,
    printablePath: String
  ): Either[BuildException, Option[(BuildRequirements, BuildOptions, Option[String])]] = either {

    val afterUsing = value {
      processUsing(content)
        .sequence
    }
    val afterProcessImports =
      processSpecialImports(afterUsing.flatMap(_._3).getOrElse(content), printablePath)

    if (afterUsing.isEmpty && afterProcessImports.isEmpty) None
    else {
      val allRequirements    = afterUsing.map(_._1).toSeq ++ afterProcessImports.map(_._1).toSeq
      val summedRequirements = allRequirements.foldLeft(BuildRequirements())(_ orElse _)
      val allOptions         = afterUsing.map(_._2).toSeq ++ afterProcessImports.map(_._2).toSeq
      val summedOptions      = allOptions.foldLeft(BuildOptions())(_ orElse _)
      val lastContentOpt = afterProcessImports
        .map(_._3)
        .orElse(afterUsing.flatMap(_._3))
      Some((summedRequirements, summedOptions, lastContentOpt))
    }
  }

  private def directivesBuildOptions(directives: Seq[Directive])
    : Either[BuildException, BuildOptions] = {
    val results = directives
      .filter(_.tpe == Directive.Using)
      .map { dir =>
        val fromHandlersOpt = usingDirectiveHandlers
          .iterator
          .flatMap(_.handle(dir).iterator)
          .toStream
          .headOption

        fromHandlersOpt match {
          case None =>
            Left(new UnusedDirectiveError(dir))
          case Some(Right(options)) =>
            Right(options)
          case Some(Left(err)) =>
            Left(new InvalidDirectiveError(dir, err))
        }
      }

    results
      .sequence
      .left.map(CompositeBuildException(_))
      .map { allOptions =>
        allOptions.foldLeft(BuildOptions())(_ orElse _)
      }
  }

  private def directivesBuildRequirements(directives: Seq[Directive])
    : Either[BuildException, BuildRequirements] = {
    val results = directives
      .filter(_.tpe == Directive.Require)
      .map { dir =>
        val fromHandlersOpt = requireDirectiveHandlers
          .iterator
          .flatMap(_.handle(dir).iterator)
          .toStream
          .headOption

        fromHandlersOpt match {
          case None =>
            Left(new UnusedDirectiveError(dir))
          case Some(Right(reqs)) =>
            Right(reqs)
          case Some(Left(err)) =>
            Left(new InvalidDirectiveError(dir, err))
        }
      }

    results
      .sequence
      .left.map(CompositeBuildException(_))
      .map { allReqs =>
        allReqs.foldLeft(BuildRequirements())(_ orElse _)
      }
  }

  private def processUsing(
    content: String
  ): Option[Either[BuildException, (BuildRequirements, BuildOptions, Option[String])]] =
    // TODO Warn about unrecognized directives
    // TODO Report via some diagnostics malformed directives
    TemporaryDirectivesParser.parseDirectives(content).map {
      case (directives, updatedContentOpt) =>
        val tuple = (
          directivesBuildRequirements(directives),
          directivesBuildOptions(directives),
          Right(updatedContentOpt)
        )
        tuple
          .traverseN
          .left.map(CompositeBuildException(_))
    }

  private def processSpecialImports(
    content: String,
    printablePath: String
  ): Option[(BuildRequirements, BuildOptions, String)] = {

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
      Some((BuildRequirements(), options, newCode))
    }
  }

  private def parseDependency(str: String): AnyDependency =
    DependencyParser.parse(str) match {
      case Left(msg)  => sys.error(s"Malformed dependency '$str': $msg")
      case Right(dep) => dep
    }
}
