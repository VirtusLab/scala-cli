package scala.build.preprocessing

import dependency.AnyDependency
import dependency.parser.DependencyParser

import java.nio.charset.StandardCharsets
import java.util.Locale

import scala.build.{Inputs, Os, Sources}
import scala.build.internal.AmmUtil
import scala.build.options.{BuildOptions, BuildRequirements, ClassPathOptions, ScalaOptions}
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
            PreprocessedSource.OnDisk(f.path, None, None, Some(inferredClsName))
          case Some((requirements, options, updatedCode)) =>
            PreprocessedSource.InMemory(
              Right(f.path),
              f.subPath,
              updatedCode,
              0,
              Some(options),
              Some(requirements),
              Some(inferredClsName)
            )
        }
        Some(Seq(source))

      case v: Inputs.VirtualScalaFile =>
        val content = new String(v.content, StandardCharsets.UTF_8)
        val (requirements, options, updatedContent) = process(content, v.source)
          .getOrElse((BuildRequirements(), BuildOptions(), content))
        val s = PreprocessedSource.InMemory(
          Left(v.source),
          v.subPath,
          updatedContent,
          0,
          Some(options),
          Some(requirements),
          None
        )
        Some(Seq(s))

      case _ =>
        None
    }

  def process(path: os.Path): Option[(BuildRequirements, BuildOptions, String)] = {
    val printablePath =
      if (path.startsWith(Os.pwd)) path.relativeTo(Os.pwd).toString
      else path.toString
    val content = os.read(path)
    process(content, printablePath)
  }
  def process(
    content: String,
    printablePath: String
  ): Option[(BuildRequirements, BuildOptions, String)] = {

    val afterUsing = processUsing(content, printablePath)
    val afterProcessImports =
      processSpecialImports(afterUsing.map(_._3).getOrElse(content), printablePath)

    if (afterUsing.isEmpty && afterProcessImports.isEmpty) None
    else {
      val allRequirements    = afterUsing.map(_._1).toSeq ++ afterProcessImports.map(_._1).toSeq
      val summedRequirements = allRequirements.foldLeft(BuildRequirements())(_ orElse _)
      val allOptions         = afterUsing.map(_._2).toSeq ++ afterProcessImports.map(_._2).toSeq
      val summedOptions      = allOptions.foldLeft(BuildOptions())(_ orElse _)
      val lastContent = afterProcessImports
        .map(_._3)
        .orElse(afterUsing.map(_._3))
        .getOrElse(content)
      Some((summedRequirements, summedOptions, lastContent))
    }
  }

  private def directivesBuildOptions(directives: Seq[Directive]): BuildOptions =
    directives
      .filter(_.tpe == Directive.Using)
      .map { dir =>
        dir.values match {
          case Seq(depStr) if depStr.split(":").count(_.trim.nonEmpty) == 3 =>
            DependencyParser.parse(depStr) match {
              case Left(err) => sys.error(s"Error parsing dependency '$depStr': $err")
              case Right(dep) =>
                BuildOptions(
                  classPathOptions = ClassPathOptions(
                    extraDependencies = Seq(dep)
                  )
                )
            }
          case Seq("scala", scalaVer) if scalaVer.nonEmpty =>
            BuildOptions(
              scalaOptions = ScalaOptions(
                scalaVersion = Some(scalaVer)
              )
            )
          case _ =>
            sys.error(s"Unrecognized using directive: ${dir.values.mkString(" ")}")
        }
      }
      .foldLeft(BuildOptions())(_ orElse _)

  private def normalizePlatform(p: String): String =
    p.toLowerCase(Locale.ROOT) match {
      case "scala.js" | "scala-js" | "scalajs" | "js" => "js"
      case "scala-native" | "scalanative" | "native"  => "native"
      case "jvm"                                      => "jvm"
      case _                                          => p
    }
  private def isPlatform(p: String): Option[BuildRequirements.Platform] =
    p match {
      case "jvm"    => Some(BuildRequirements.Platform.JVM)
      case "js"     => Some(BuildRequirements.Platform.JS)
      case "native" => Some(BuildRequirements.Platform.Native)
      case _        => None
    }
  private def isPlatformSpec(
    l: List[String],
    acc: Set[BuildRequirements.Platform]
  ): Option[Set[BuildRequirements.Platform]] =
    l match {
      case Nil      => None
      case p :: Nil => isPlatform(p).map(p0 => acc + p0)
      case p :: "|" :: tail =>
        isPlatform(p) match {
          case Some(p0) => isPlatformSpec(tail, acc + p0)
          case None     => None
        }
    }

  private def directivesBuildRequirements(directives: Seq[Directive]): BuildRequirements =
    directives
      .filter(_.tpe == Directive.Require)
      .map { dir =>
        dir.values match {
          case Seq("scala", ">=", minVer) =>
            BuildRequirements(
              scalaVersion = Seq(BuildRequirements.VersionHigherThan(minVer, orEqual = true))
            )
          case Seq("scala", "<=", maxVer) =>
            BuildRequirements(
              scalaVersion = Seq(BuildRequirements.VersionLowerThan(maxVer, orEqual = true))
            )
          case Seq("scala", "==", reqVer) =>
            // FIXME What about things like just '2.12'?
            BuildRequirements(
              scalaVersion = Seq(BuildRequirements.VersionEquals(reqVer, loose = true))
            )
          case other =>
            isPlatformSpec(other.map(normalizePlatform).toList, Set.empty) match {
              case Some(platforms) =>
                BuildRequirements(
                  platform = Some(BuildRequirements.PlatformRequirement(platforms))
                )
              case None =>
                sys.error(s"Unrecognized require directive: ${other.mkString(" ")}")
            }
        }
      }
      .foldLeft(BuildRequirements())(_ orElse _)

  private def processUsing(
    content: String,
    printablePath: String
  ): Option[(BuildRequirements, BuildOptions, String)] =
    TemporaryDirectivesParser.parseDirectives(content).flatMap {
      case (directives, updatedContent) =>
        // TODO Warn about unrecognized directives
        // TODO Report via some diagnostics malformed directives

        TemporaryDirectivesParser.parseDirectives(content).map {
          case (directives, updatedContent) =>
            (
              directivesBuildRequirements(directives),
              directivesBuildOptions(directives),
              updatedContent
            )
        }
    }

  private def processSpecialImports(
    content: String,
    printablePath: String
  ): Option[(BuildRequirements, BuildOptions, String)] = {

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
      Some((BuildRequirements(), options, newCode))
    }
  }

  private def parseDependency(str: String): AnyDependency =
    DependencyParser.parse(str) match {
      case Left(msg)  => sys.error(s"Malformed dependency '$str': $msg")
      case Right(dep) => dep
    }
}
