package scala.build.internal

abstract class CodeWrapper {
  def wrapperPath: Seq[Name] = Nil
  def apply(
    code: String,
    pkgName: Seq[Name],
    indexedWrapperName: Name,
    extraCode: String,
    scriptPath: String
  ): (String, String)

  def wrapCode(
    pkgName: Seq[Name],
    indexedWrapperName: Name,
    code: String,
    scriptPath: String
  ): (String, WrapperParams) = {

    // we need to normalize topWrapper and bottomWrapper in order to ensure
    // the snippets always use the platform-specific newLine
    val extraCode0 = "/*</generated>*/"
    val (topWrapper, bottomWrapper) =
      apply(code, pkgName, indexedWrapperName, extraCode0, scriptPath)

    val nl = System.lineSeparator()
    val (topWrapper0, bottomWrapper0) =
      (
        topWrapper + "/*<script>*/" + nl,
        nl + "/*</script>*/ /*<generated>*/" + bottomWrapper
      )

    val wrapperParams = WrapperParams(topWrapper0.linesIterator.size, code.linesIterator.size)

    (topWrapper0 + code + bottomWrapper0, wrapperParams)
  }

}

object CodeWrapper {
  def mainClassObject(className: Name): Name =
    Name(className.raw ++ "_sc")
}

case class WrapperParams(topWrapperLineCount: Int, userCodeLineCount: Int)
