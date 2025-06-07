package scala.build.internal

abstract class CodeWrapper {
  def apply(
    code: String,
    pkgName: Seq[Name],
    indexedWrapperName: Name,
    extraCode: String,
    scriptPath: String
  ): (String, String)

  def mainClassObject(className: Name): Name

  def wrapCode(
    pkgName: Seq[Name],
    indexedWrapperName: Name,
    code: String,
    scriptPath: String
  ): (String, WrapperParams) = {

    // we need to normalize topWrapper and bottomWrapper in order to ensure
    // the snippets always use the platform-specific newLine
    val extraCode0                  = "/*</generated>*/"
    val (topWrapper, bottomWrapper) =
      apply(code, pkgName, indexedWrapperName, extraCode0, scriptPath)

    // match lineSeparator to existing code
    val nl = code.indexOf("\n") match {
      case n if n > 0 && code(n - 1) == '\r' => System.lineSeparator()
      case _                                 => "\n"
    }
    val (topWrapper0, bottomWrapper0) =
      (
        topWrapper + "/*<script>*/" + nl,
        nl + "/*</script>*/ /*<generated>*/" + bottomWrapper
      )

    val mainClassName =
      (pkgName :+ mainClassObject(indexedWrapperName)).map(_.encoded).mkString(".")

    val wrapperParams =
      WrapperParams(topWrapper0.linesIterator.size, code.linesIterator.size, mainClassName)

    (topWrapper0 + code + bottomWrapper0, wrapperParams)
  }
}

case class WrapperParams(topWrapperLineCount: Int, userCodeLineCount: Int, mainClass: String)
