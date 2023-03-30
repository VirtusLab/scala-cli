package scala.build.internal

case object CustomCodeWrapper extends CodeWrapper {
  def mainClassObject(className: Name): Name =
    Name(className.raw ++ "_sc")

  private val userCodeNestingLevel = 1
  def apply(
    code: String,
    pkgName: Seq[Name],
    indexedWrapperName: Name,
    extraCode: String
  ) = {
    val name               = mainClassObject(indexedWrapperName).backticked
    val aliasedWrapperName = name + "$$alias"
    val funHashCodeMethod =
      if (name == "main_sc")
        s"$aliasedWrapperName.alias.hashCode()" // https://github.com/VirtusLab/scala-cli/issues/314
      else s"${indexedWrapperName.backticked}.hashCode()"
    // We need to call hashCode (or any other method so compiler does not report a warning)
    val mainObjectCode =
      AmmUtil.normalizeNewlines(s"""|object $name {
                                    |  private var args$$opt0 = Option.empty[Array[String]]
                                    |  def args$$set(args: Array[String]): Unit = {
                                    |    args$$opt0 = Some(args)
                                    |  }
                                    |  def args$$opt: Option[Array[String]] = args$$opt0
                                    |  def args$$: Array[String] = args$$opt.getOrElse {
                                    |    sys.error("No arguments passed to this script")
                                    |  }
                                    |  def main(args: Array[String]): Unit = {
                                    |    args$$set(args)
                                    |    $funHashCodeMethod // hasCode to clear scalac warning about pure expression in statement position
                                    |  }
                                    |}
                                    |""".stripMargin)

    val packageDirective =
      if (pkgName.isEmpty) "" else s"package ${AmmUtil.encodeScalaSourcePath(pkgName)}" + "\n"

    val aliasObject =
      if (name == "main_sc")
        s"""object $aliasedWrapperName {
           |  val alias = ${indexedWrapperName.backticked}
           |}""".stripMargin
      else ""

    // indentation is important in the generated code, so we don't want scalafmt to touch that
    // format: off
    val top = AmmUtil.normalizeNewlines(s"""
$packageDirective


object ${indexedWrapperName.backticked} {
def args = $name.args$$
""")
    val bottom = AmmUtil.normalizeNewlines(s"""
$extraCode
}
$aliasObject
$mainObjectCode
""")
    // format: on

    (top, bottom, userCodeNestingLevel)
  }
}
