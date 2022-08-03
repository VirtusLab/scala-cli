package scala.build.preprocessing.mdsandbox

import scala.collection.mutable

object MarkdownSnippet {
  private val allowIndentedFence: Boolean = false

  case class Fence(
    info: Seq[String],
    body: String,
    startLine: Int,  // start of fenced body EXCLUDING backticks
    endLine: Int     // same as above
  ) {
    override def toString(): String = s"Fence[$info, lines $startLine-$endLine]{${body.replace("\n", "\\n")}}"

    /** 
      * @return `true` if this snippet should be ignored, `false` otherwise 
      */
    def shouldIgnore: Boolean = info(0) != "scala" || info.contains("ignore")

    /**
      * @return `true` if this snippet should have its scope reset, `false` otherwise
      */
    def resetScope: Boolean = info.contains("reset")

    /**
      * @return `true` if this snippet is a test snippet, `false` otherwise
      */
    def isTest: Boolean = info.contains("test")

    /**
      * @return `true` if this snippet is a raw snippet, `false` otherwise
      */
    def isRaw: Boolean = info.contains("raw")

    /**
      * Extracts `key=value` pairs from snippet description. For example:
      * {{{```scala a=apple b=banana.png c=lemon=lemon a=melon}}}
      * will return following mappings
      * - a -> "apple" (not "melon", first value counts)
      * - b -> "banana.png"
      * - c -> "lemon=lemon"
      *
      * @return `Map` containing all extracted `key -> value` mappings
      */
    def modifiers: Map[String, String] = Map.from(
      info
      .filter(_.contains('='))
      .map(s => (
        s.takeWhile(_ != '='), s.dropWhile(_ != '=').drop(1)))
      .distinctBy(_._1)
    )
  }

  private case class StartedFence(
    info: String,
    tickStartLine: Int,  // fence start INCLUDING backticks
    backticks: String,
    indent: Int
  )

  /**
    * Closes started code-fence
    *
    * @param started [[StartedFence]] representing this code-fence's start
    * @param tickEndLine number of the line where closing backticks are
    * @param lines input file sliced into lines
    * @return [[Fence]] representing whole closed code-fence
    */
  private def closeFence(started: StartedFence, tickEndLine: Int, lines: Array[String]): Fence = {
    val start: Int = started.tickStartLine + 1
    val bodyLines: Array[String] = lines.slice(start, tickEndLine)
    Fence(
      started.info.split("\\s+").toList,  // strip info by whitespaces
      bodyLines.tail.foldLeft(bodyLines.head)((body, line) => body.:++("\n"+line)),
      start,  // snippet has to begin in the new line
      tickEndLine - 1  // ending backticks have to be placed below the snippet
    )
  }

  /**
    * Finds all code snippets in given input
    *
    * @param md Markdown file in a `String` format
    * @return list of all found snippets
    */
  def findFences(md: String): Seq[Fence] = {
    var startedFenceOpt: Option[StartedFence] = None
    val fences = mutable.ListBuffer.empty[Fence]
    val lines: Array[String] = md.split("\n\r?")
    for (i <- 0 until lines.length) {
      val line = lines(i)
      startedFenceOpt match {
        case Some(s) => {
          val start: Int = line.indexOf(s.backticks)
          if (start == s.indent && line.forall(c => c == '`' || c.isWhitespace)) {
            fences += closeFence(s, i, lines)
            startedFenceOpt = None
          }
        } case None => {
          val start: Int = line.indexOf("```")
          if (start == 0 || (start > 0 && allowIndentedFence)) {  // doesn't allow snippet indent
            val fence = line.substring(start)
            val backticks: String = fence.takeWhile(_ == '`')
            val info: String = fence.substring(backticks.length)
            startedFenceOpt = Some(StartedFence(info, i, backticks, start))
          }
        }
      }
    }
    startedFenceOpt match {  // snippet can be ended with EOF
      case Some(s) => {
        fences += closeFence(s, lines.length, lines)
        startedFenceOpt = None
      }
      case None =>
    }

    fences.toList.filter(fence => !fence.shouldIgnore)
  }
}
