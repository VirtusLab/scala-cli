package scala.build.preprocessing.mdsandbox

import MarkdownSnippet.Fence

class SnippetPackager(val fileName: String, val snippets: Seq[Fence]) {
  def this(fileName: String, content: String) = this(fileName, MarkdownSnippet.findFences(content))

  val (rawSnippets, processedSnippets) = snippets.partition(f => f.isRaw)
  val (testSnippets, runSnippets) = processedSnippets.partition(f => f.isTest)

  val runObjectIdentifier: String = s"`Markdown$$${fileName.replace('.', '$')}`"
  val testObjectIdentifier: String = s"`Markdown_Test$$${fileName.replace('.', '$')}`"

  /** Generates class name for a snippet with given index */
  def runClassName(index: Int): String = s"Snippet$$$index"

  /** Generates class name for a test snippet with given index */
  def testClassName(index: Int): String = s"`Test$$${fileName.replace('.', '$')}_$index`"

  /** Returns Scala snippets packed into classes and glued together into an object */
  def buildScalaMain(): String = {
    if (runSnippets.isEmpty) s"object $runObjectIdentifier {def execute(): Unit = {}}"  // no snippets
    else (0 until runSnippets.length).foldLeft(
      s"object $runObjectIdentifier {def execute(): Unit = {"
    ) (
      (sum, index) => 
        if (runSnippets(index).resetScope || index == 0) sum :++ s"new ${runClassName(index)}; "
        else sum  // that class hasn't been created
    )
    .:++("} ")
    .:++(buildScalaMain(0, 0))
    .:++("}")
  }

  private def buildScalaMain(index: Int, line: Int): String = {
    if (index >= runSnippets.length) "}"  // close last class
    else {
      val fence: Fence = runSnippets(index)
      val classOpener: String =
        if (index == 0)            s"class ${runClassName(index)} {\n"     // first snippet needs to open a class
        else if (fence.resetScope) s"}; class ${runClassName(index)} {\n"  // if scope is being reset, close previous class and open a new one
        else "\n"
      ("\n" * (fence.startLine - line - 1))                 // padding
        .:++(classOpener)                                   // new class opening (if applicable)
        .:++(fence.body)                                    // snippet body
        .:++("\n")                                          // padding in place of closing backticks
        .:++(buildScalaMain(index + 1, fence.endLine + 1))  // further snippets
    }
  }

  /** Returns test snippets packed into classes and glued together into an object */
  def buildScalaTest(): String = {
    if (testSnippets.isEmpty) s"object $testObjectIdentifier {}"  // no snippets
    else buildScalaTest(0, 0) :++ "}"
  }

  /*
   * TESTING APPROACH A - RAW TESTING
   * ```scala test
   */
  // private def buildScalaTest(index: Int, line: Int): String = {
  //   if (index >= testSnippets.length) ""
  //   else {
  //     val fence: Fence = testSnippets(index)
  //     ("\n" * (fence.startLine - line))      // padding
  //       .:++(fence.body)                     // snippet body
  //       .:++("\n")                           // padding in place of closing backticks
  //       .:++(buildScalaTest(index + 1, fence.endLine + 1))
  //   }
  // }

  /*
   * TESTING APPROACH B - EXPLICITLY DEFINED FRAMEWORK
   * ```scala test lib=org.scalameta::munit::0.7.29 extends=munit.FunSuite
   */
  private def buildScalaTest(index: Int, line: Int): String = {
    if (index >= testSnippets.length) ""
    else {
      val fence: Fence = testSnippets(index)

      val usingLib: String = fence.modifiers.get("lib") match {
        case Some(lib) => s"/*> using lib \"${lib}\" */"
        case None      => ""
      }
      val extensions: Option[String] = fence.modifiers.get("extends")
      val extendsClause: String = if (extensions.isDefined) s" extends ${extensions.get}" else ""

      val classOpener: String =
        if (index == 0)  s"class ${testClassName(index)}$extendsClause {\n"    // first snippet needs to open a class
        else             s"}; class ${testClassName(index)}$extendsClause {\n"  // if scope is being reset, close previous class and open a new one
      ("\n" * (fence.startLine - line - 1))                 // padding
        .:++(usingLib)
        .:++(classOpener)                                   // new class opening
        .:++(fence.body)                                    // snippet body
        .:++("\n")                                          // padding in place of closing backticks
        .:++(buildScalaTest(index + 1, fence.endLine + 1))  // further test snippets
    }
  }

  /*
   * TESTING APPROACH C - HARDCODED FRAMEWORK LIST
   * ```scala test framework=munit
   */
  // private def buildScalaTest(index: Int, line: Int): String = {
  //   if (index >= testSnippets.length) ""
  //   else {
  //     val fence: Fence = testSnippets(index)
  //     val testFrameworks: Map[String, (String, String)] = Map(
  //       "munit" -> ("org.scalameta::munit::0.7.29", "munit.FunSuite")
  //     )
  //     val (usingLib, extendsClause): (String, String) =
  //       fence.modifiers.get("framework") match {
  //         case Some(fw) => testFrameworks.get(fw) match
  //           case Some((lib, ext)) => (s"/*> using lib \"${lib}\" */", s" extends ${ext}")
  //           case None       => ("", "")
  //         case None     => ("", "")
  //       }

  //     val classOpener: String =
  //       if (index == 0)  s"class ${testClassName(index)}$extendsClause {\n"     // first snippet needs to open a class
  //       else             s"}; class ${testClassName(index)}$extendsClause {\n"  // if scope is being reset, close previous class and open a new one
  //     ("\n" * (fence.startLine - line - 1))                 // padding
  //       .:++(usingLib)
  //       .:++(classOpener)                                   // new class opening
  //       .:++(fence.body)                                    // snippet body
  //       .:++("\n")                                          // padding in place of closing backticks
  //       .:++(buildScalaTest(index + 1, fence.endLine + 1))  // further test snippets
  //   }
  // }

  /** Returns raw snippets glued together into one file */
  def buildScalaRaw(): String = {
    if (rawSnippets.isEmpty) ""
    else buildScalaRaw(0, 0)
  }

  private def buildScalaRaw(index: Int, line: Int): String = {
    if (index >= rawSnippets.length) ""
    else {
      val fence: Fence = rawSnippets(index)
      ("\n" * (fence.startLine - line))      // padding
        .:++(fence.body)                     // snippet body
        .:++("\n")                           // padding in place of closing backticks
        .:++(buildScalaRaw(index + 1, fence.endLine + 1))
    }
  }
}
