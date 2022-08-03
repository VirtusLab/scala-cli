package scala.build.preprocessing.mdsandbox

import scala.build.Inputs

object MdRunner {
  /**
    * Generates an executor for given `fileName`
    */
  def markdownExecutor(fileName: String): String = s"`Markdown$$${fileName.replace('.', '$')}`.execute()"

  /**
    * Creates a virtual Scala file, which executes snippets from other Markdown files
    *
    * @param inputs inputs given to Scala-CLI by the user
    * @return created virtual Scala file
    */
  def generateRunnerFile(inputs: Inputs): Inputs.VirtualScalaFile = {
    val flatInputs: Seq[Inputs.SingleElement] = inputs.flattened()

    val content: Array[Byte] = flatInputs
      .collect{
        case Inputs.MarkdownFile(base, subPath) => markdownExecutor(subPath.toString)
      }
      .mkString("object Markdown$Runner {def main(args: Array[String]): Unit = {", "; ", "}}")
      .getBytes()
    val source: String = "markdown_runner.scala"

    Inputs.VirtualScalaFile(content, source)
  }
}
