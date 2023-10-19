package scala.build.internal

/** Script code wrapper compatible with Scala 2 and JS native members <br> <br> When using Scala 3
  * or/and not using JS native prefer [[ClassCodeWrapper]], since it prevents deadlocks when running
  * threads from script
  */
case object ObjectCodeWrapper extends CodeWrapper {
  def apply(
    code: String,
    pkgName: Seq[Name],
    indexedWrapperName: Name,
    extraCode: String,
    scriptPath: String
  ) = {
    val mainObjectName = CodeWrapper.mainClassObject(indexedWrapperName).backticked
    val wrapperObjectName = indexedWrapperName.backticked
    val aliasedWrapperName = wrapperObjectName + "$$alias"

    val wrapperReference = if (wrapperObjectName == "main")
      s"$aliasedWrapperName.alias" // https://github.com/VirtusLab/scala-cli/issues/314
    else wrapperObjectName

    val packageDirective =
      if (pkgName.isEmpty) "" else s"package ${AmmUtil.encodeScalaSourcePath(pkgName)}" + "\n"

    val mainObject = AmmUtil.normalizeNewlines(
      s"""object $mainObjectName {
      |  def main(args: Array[String]): Unit = {
      |    val _ = $wrapperReference.run(args)
      |  }
      |}
      |""".stripMargin)


    val aliasObject = if (wrapperObjectName == "main")
      AmmUtil.normalizeNewlines(
      s"""object $aliasedWrapperName {
        |  val alias = $wrapperObjectName
        |}
        |""".stripMargin)
    else ""

    // This is copied from Scala 2 implementation of App trait, but with no main method
    val scalaCliAppTrait =
      s"""@scala.annotation.nowarn("cat=deprecation&msg=DelayedInit semantics can be surprising")
        |trait ScalaCliApp_$wrapperObjectName extends DelayedInit {
        |  protected final def args: Array[String] = _args
        |  private[this] var _args: Array[String] = _
        |
        |  private[this] val initCode = new scala.collection.mutable.ListBuffer[() => Unit]
        |
        |  override def delayedInit(body: => Unit): Unit = {
        |    initCode += (() => body)
        |  }
        |
        |  def run(args: Array[String]): Unit = {
        |    _args = args
        |    for (proc <- initCode) proc()
        |  }
        |}
        |""".stripMargin


    val top = AmmUtil.normalizeNewlines(
      s"""$packageDirective
      |
      |$scalaCliAppTrait
      |
      |object $wrapperObjectName extends ScalaCliApp_$wrapperObjectName {
      |def scriptPath = \"\"\"$scriptPath\"\"\"
      |""".stripMargin)
    val bottom = AmmUtil.normalizeNewlines(
      s"""
      |$extraCode
      |}
      |
      |$mainObject
      |
      |$aliasObject
      |""".stripMargin)

    (top, bottom)
  }
}
