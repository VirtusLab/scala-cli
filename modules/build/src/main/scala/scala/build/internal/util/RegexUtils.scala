package scala.build.internal.util

import java.util.regex.Pattern

object RegexUtils {

  /** Based on junit-interface [GlobFilter.
    * compileGlobPattern](https://github.com/sbt/junit-interface/blob/f8c6372ed01ce86f15393b890323d96afbe6d594/src/main/java/com/novocode/junit/GlobFilter.java#L37)
    *
    * @return
    *   Pattern allows to regex input which contains only *, for example `*foo*` match to
    *   `MyTests.foo`
    */
  def globPattern(expr: String): Pattern = {
    val a = expr.split("\\*", -1)
    val b = new StringBuilder()
    for (i <- 0 until a.length) {
      if (i != 0) b.append(".*")
      if (a(i).nonEmpty) b.append(Pattern.quote(a(i).replaceAll("\n", "\\n")))
    }
    Pattern.compile(b.toString)
  }
}
