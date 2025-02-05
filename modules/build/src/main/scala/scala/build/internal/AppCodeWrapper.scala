package scala.build.internal

case class AppCodeWrapper(scalaVersion: String, log: String => Unit) extends CodeWrapper {
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
    val invokeMain = mainObject match
      case WrapperUtils.ScriptMainMethod.Exists(name) => s"\n$name.main(args)"
      case otherwise =>
        otherwise.warningMessage.foreach(log)
        ""
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
