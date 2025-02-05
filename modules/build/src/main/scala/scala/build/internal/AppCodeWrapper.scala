package scala.build.internal

case class AppCodeWrapper(scalaVersion: String) extends CodeWrapper {
  override def mainClassObject(className: Name) = className

  def apply(
    code: String,
    pkgName: Seq[Name],
    indexedWrapperName: Name,
    extraCode: String,
    scriptPath: String
  ) = {
    val wrapperObjectName = indexedWrapperName.backticked

    val mainObject = WrapperUtils.mainObjectInScript(scalaVersion, code)
    val invokeMain = mainObject match {
      case None       => ""
      case Some(name) => s"\n$name.main(args)"
    }
    val packageDirective =
      if (pkgName.isEmpty) "" else s"package ${AmmUtil.encodeScalaSourcePath(pkgName)}" + "\n"
    val top = AmmUtil.normalizeNewlines(
      s"""$packageDirective
         |
         |object $wrapperObjectName extends App {
         |val scriptPath = \"\"\"$scriptPath\"\"\"$invokeMain
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
