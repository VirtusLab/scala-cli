package scala.build.internal

abstract class CodeWrapper {
  def wrapperPath: Seq[Name] = Nil
  def apply(
    code: String,
    pkgName: Seq[Name],
    indexedWrapperName: Name,
    extraCode: String
  ): (String, String, Int)

  def wrapCode(pkgName: Seq[Name], indexedWrapperName: Name, code: String) = {

    // we need to normalize topWrapper and bottomWrapper in order to ensure
    // the snippets always use the platform-specific newLine
    val extraCode0 = "/*</generated>*/"
    val (topWrapper, bottomWrapper, userCodeNestingLevel) =
      apply(code, pkgName, indexedWrapperName, extraCode0)
    val (topWrapper0, bottomWrapper0) =
      (topWrapper + "/*<script>*/", "/*</script>*/ /*<generated>*/" + bottomWrapper)
    val importsLen = topWrapper0.length

    (topWrapper0 + code + bottomWrapper0, importsLen, userCodeNestingLevel)
  }

}
