package scala.build.preprocessing

import dependency.AnyDependency
import dependency.parser.DependencyParser
import scalaparse.Scala.Pattern
import scala.util.matching.Regex

import java.nio.charset.StandardCharsets

import scala.build.{Inputs, Os}
import scala.build.internal.{AmmUtil, CodeWrapper, Name}
import scala.build.options.{BuildOptions, ClassPathOptions}

final case class ScriptPreprocessor(codeWrapper: CodeWrapper) extends Preprocessor {
  def preprocess(input: Inputs.SingleElement): Option[Seq[PreprocessedSource]] =
    input match {
      case script: Inputs.Script =>
        val content = os.read(script.path)
        val printablePath =
          if (script.path.startsWith(Os.pwd)) script.path.relativeTo(Os.pwd).toString
          else script.path.toString

        val preprocessed = ScriptPreprocessor.preprocess(
          Right(script.path),
          content,
          printablePath,
          codeWrapper,
          script.subPath
        )
        Some(Seq(preprocessed))

      case script: Inputs.VirtualScript =>
        val content = new String(script.content, StandardCharsets.UTF_8)

        val preprocessed = ScriptPreprocessor.preprocess(
          Left(script.source),
          content,
          script.source,
          codeWrapper,
          script.wrapperPath
        )
        Some(Seq(preprocessed))

      case _ =>
        None
    }
}

object ScriptPreprocessor {

  private val sheBangRegex: Regex = s"""((#!.*)|(!#.*))""".r

  private def ignoreSheBangLines(content: String): String = {
    if (content.startsWith("#!")) {
      sheBangRegex.replaceAllIn(content, "")
    }
    else {
      content
    }
  }

  private def preprocess(
    reportingPath: Either[String, os.Path],
    content: String,
    printablePath: String,
    codeWrapper: CodeWrapper,
    subPath: os.SubPath
  ) = {

    val contentIgnoredSheBangLines = ignoreSheBangLines(content)

    val (pkg, wrapper) = AmmUtil.pathToPackageWrapper(Nil, subPath)

    val (options, updatedCode) =
      ScalaPreprocessor.process(contentIgnoredSheBangLines, printablePath)
        .getOrElse((BuildOptions(), contentIgnoredSheBangLines))

    val (code, topWrapperLen, _) = codeWrapper.wrapCode(
      pkg,
      wrapper,
      updatedCode
    )

    val className = (pkg :+ wrapper).map(_.raw).mkString(".")

    val components = className.split('.')
    val relPath    = os.rel / components.init.toSeq / s"${components.last}.scala"
    PreprocessedSource.InMemory(
      reportingPath,
      relPath,
      code,
      topWrapperLen,
      Some(options),
      Some(className)
    )
  }

}
