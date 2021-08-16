package scala.build.preprocessing

import dependency.AnyDependency
import dependency.parser.DependencyParser

import java.nio.charset.StandardCharsets

import scala.build.{Inputs, Sources}
import scala.build.internal.AmmUtil
import scala.build.options.{BuildOptions, ClassPathOptions}

case object ScalaPreprocessor extends Preprocessor {
  def preprocess(input: Inputs.SingleElement): Option[Seq[PreprocessedSource]] =
    input match {
      case f: Inputs.ScalaFile =>
        val inferredClsName = {
          val (pkg, wrapper) = AmmUtil.pathToPackageWrapper(Nil, f.subPath)
          (pkg :+ wrapper).map(_.raw).mkString(".")
        }
        val source = Sources.process(f.path) match {
          case None =>
            PreprocessedSource.OnDisk(f.path, None, Some(inferredClsName))
          case Some((deps, updatedCode)) =>
            val options = BuildOptions(classPathOptions = ClassPathOptions(
              extraDependencies = deps.map(ScalaPreprocessor.parseDependency)
            ))
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
        val (deps, updatedContent) = Sources.process(content, v.source).getOrElse((Nil, content))
        val options = BuildOptions(classPathOptions = ClassPathOptions(
          extraDependencies = deps.map(ScalaPreprocessor.parseDependency)
        ))
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

  private def parseDependency(str: String): AnyDependency =
    DependencyParser.parse(str) match {
      case Left(msg) => sys.error(s"Malformed dependency '$str': $msg")
      case Right(dep) => dep
    }
}
