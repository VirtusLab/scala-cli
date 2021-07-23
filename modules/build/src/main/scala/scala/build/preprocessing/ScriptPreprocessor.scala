package scala.build.preprocessing

import dependency.AnyDependency
import dependency.parser.DependencyParser

import java.nio.charset.StandardCharsets

import scala.build.{Inputs, Os, Sources}
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

  private def preprocess(
    reportingPath: Either[String, os.Path],
    content: String,
    printablePath: String,
    codeWrapper: CodeWrapper,
    subPath: os.SubPath
  ) = {

    val (pkg, wrapper) = AmmUtil.pathToPackageWrapper(Nil, subPath)

    val (deps, updatedCode) = Sources.process(content, printablePath).getOrElse((Nil, content))

    val (code, topWrapperLen, _) = codeWrapper.wrapCode(
      pkg,
      wrapper,
      updatedCode
    )

    val deps0 = deps.map(ScriptPreprocessor.parseDependency)

    val className = (pkg :+ wrapper).map(_.raw).mkString(".")
    val options = BuildOptions(classPathOptions = ClassPathOptions(
      extraDependencies = deps0
    ))

    val components = className.split('.')
    val relPath = os.rel / components.init.toSeq / s"${components.last}.scala"
    PreprocessedSource.InMemory(reportingPath, relPath, code, topWrapperLen, Some(options), Some(className))
  }

  private def parseDependency(str: String): AnyDependency =
    DependencyParser.parse(str) match {
      case Left(msg) => sys.error(s"Malformed dependency '$str': $msg")
      case Right(dep) => dep
    }

}
