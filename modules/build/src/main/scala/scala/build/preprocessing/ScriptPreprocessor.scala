package scala.build.preprocessing

import scala.util.matching.Regex

import java.nio.charset.StandardCharsets

import scala.build.{Inputs, Os}
import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.{AmmUtil, CodeWrapper}
import scala.build.options.BuildOptions
import scala.build.options.BuildRequirements

final case class ScriptPreprocessor(codeWrapper: CodeWrapper) extends Preprocessor {
  def preprocess(input: Inputs.SingleElement)
    : Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case script: Inputs.Script =>
        val content = os.read(script.path)
        val printablePath =
          if (script.path.startsWith(Os.pwd)) script.path.relativeTo(Os.pwd).toString
          else script.path.toString

        val res = either {
          val preprocessed = value {
            ScriptPreprocessor.preprocess(
              Right(script.path),
              content,
              printablePath,
              codeWrapper,
              script.subPath
            )
          }
          Seq(preprocessed)
        }
        Some(res)

      case script: Inputs.VirtualScript =>
        val content = new String(script.content, StandardCharsets.UTF_8)

        val res = either {
          val preprocessed = value {
            ScriptPreprocessor.preprocess(
              Left(script.source),
              content,
              script.source,
              codeWrapper,
              script.wrapperPath
            )
          }
          Seq(preprocessed)
        }
        Some(res)

      case _ =>
        None
    }
}

object ScriptPreprocessor {

  private val sheBangRegex: Regex = s"""(^(#!.*(\\r\\n?|\\n)?)+(\\s*!#.*)?)""".r

  private def ignoreSheBangLines(content: String): String = {
    if (content.startsWith("#!")) {
      val regexMatch = sheBangRegex.findFirstMatchIn(content)
      regexMatch match {
        case Some(firstMatch) =>
          content.replace(
            firstMatch.toString(),
            System.lineSeparator() * firstMatch.toString().split(System.lineSeparator()).length
          )
        case None => content
      }
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
  ): Either[BuildException, PreprocessedSource.InMemory] = either {

    val contentIgnoredSheBangLines = ignoreSheBangLines(content)

    val (pkg, wrapper) = AmmUtil.pathToPackageWrapper(Nil, subPath)

    val (requirements, options, updatedCode) =
      value(ScalaPreprocessor.process(contentIgnoredSheBangLines, printablePath))
        .getOrElse((BuildRequirements(), BuildOptions(), contentIgnoredSheBangLines))

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
      Some(requirements),
      Some(className)
    )
  }

}
