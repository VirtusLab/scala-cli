package scala.build.internal

case object AppCodeWrapper extends CodeWrapper {
  override def mainClassObject(className: Name) = className

  def apply(
    code: String,
    pkgName: Seq[Name],
    indexedWrapperName: Name,
    extraCode: String,
    scriptPath: String
  ) = {
    val wrapperObjectName = indexedWrapperName.backticked

    val packageDirective =
      if (pkgName.isEmpty) "" else s"package ${AmmUtil.encodeScalaSourcePath(pkgName)}" + "\n"
    val top = AmmUtil.normalizeNewlines(
      s"""$packageDirective
         |
         |object $wrapperObjectName extends App {
         |val scriptPath = \"\"\"$scriptPath\"\"\"
         |""".stripMargin
    )
    val bottom = AmmUtil.normalizeNewlines(
      s"""
         |$extraCode
         |}
         |""".stripMargin
    )

    (top, bottom)
  }
}
