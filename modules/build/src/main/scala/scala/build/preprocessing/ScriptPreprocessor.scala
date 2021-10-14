package scala.build.preprocessing

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.Inputs
import scala.build.errors.BuildException
import scala.build.internal.{AmmUtil, CodeWrapper, CustomCodeWrapper, Name}
import scala.build.options.{BuildOptions, BuildRequirements}
import scala.util.matching.Regex

final case class ScriptPreprocessor(codeWrapper: CodeWrapper) extends Preprocessor {
  def preprocess(input: Inputs.SingleElement)
    : Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case script: Inputs.Script =>
        val content = os.read(script.path)

        val res = either {
          val preprocessed = value {
            ScriptPreprocessor.preprocess(
              Right(script.path),
              content,
              codeWrapper,
              script.subPath,
              ScopePath.fromPath(script.path)
            )
          }
          preprocessed
        }
        Some(res)

      case script: Inputs.VirtualScript =>
        val content = new String(script.content, StandardCharsets.UTF_8)

        val res = either {
          val preprocessed = value {
            ScriptPreprocessor.preprocess(
              Left(script.source),
              content,
              codeWrapper,
              script.wrapperPath,
              script.scopePath
            )
          }
          preprocessed
        }
        Some(res)

      case _ =>
        None
    }
}

object ScriptPreprocessor {

  private val sheBangRegex: Regex = s"""(^(#!.*(\\r\\n?|\\n)?)+(\\s*!#.*)?)""".r

  private def ignoreSheBangLines(content: String): String =
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
    else
      content

  private def preprocess(
    reportingPath: Either[String, os.Path],
    content: String,
    codeWrapper: CodeWrapper,
    subPath: os.SubPath,
    scopePath: ScopePath
  ): Either[BuildException, List[PreprocessedSource.InMemory]] = either {

    val contentIgnoredSheBangLines = ignoreSheBangLines(content)

    val (pkg, wrapper) = AmmUtil.pathToPackageWrapper(subPath)

    val (requirements, scopedRequirements, options, updatedCodeOpt) =
      value(ScalaPreprocessor.process(contentIgnoredSheBangLines, reportingPath, scopePath / os.up))
        .getOrElse((BuildRequirements(), Nil, BuildOptions(), None))

    val (code, topWrapperLen, _) = codeWrapper.wrapCode(
      pkg,
      wrapper,
      updatedCodeOpt.getOrElse(contentIgnoredSheBangLines)
    )

    val className = (pkg :+ wrapper).map(_.raw).mkString(".")
    val relPath   = os.rel / (subPath / os.up) / s"${subPath.last.stripSuffix(".sc")}.scala"

    val file = PreprocessedSource.InMemory(
      reportingPath,
      relPath,
      code,
      topWrapperLen,
      Some(options),
      Some(requirements),
      scopedRequirements,
      Some(CustomCodeWrapper.mainClassObject(Name(className)).backticked),
      scopePath
    )
    List(file)
  }

}
