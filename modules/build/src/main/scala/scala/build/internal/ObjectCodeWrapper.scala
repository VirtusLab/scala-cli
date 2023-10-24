package scala.build.internal

/** Script code wrapper compatible with Scala 2 and JS native members <br> <br> When using Scala 3
  * or/and not using JS native prefer [[ClassCodeWrapper]], since it prevents deadlocks when running
  * threads from script
  */
case class ObjectCodeWrapper(useDelayedInit: Boolean) extends CodeWrapper {
  def apply(
    code: String,
    pkgName: Seq[Name],
    indexedWrapperName: Name,
    extraCode: String,
    scriptPath: String
  ) = {
    val mainObjectName     = CodeWrapper.mainClassObject(indexedWrapperName).backticked
    val wrapperObjectName  = indexedWrapperName.backticked
    val aliasedWrapperName = wrapperObjectName + "$$alias"

    val wrapperReference = if (wrapperObjectName == "main")
      s"$aliasedWrapperName.alias" // https://github.com/VirtusLab/scala-cli/issues/314
    else wrapperObjectName

    val packageDirective =
      if (pkgName.isEmpty) "" else s"package ${AmmUtil.encodeScalaSourcePath(pkgName)}" + "\n"

    val mainObject = AmmUtil.normalizeNewlines(
      s"""object $mainObjectName {
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
         |    val _ = $wrapperReference${if useDelayedInit then ".run()" else ".hashCode()"}
         |  }
         |}
         |""".stripMargin
    )

    val aliasObject = if (wrapperObjectName == "main")
      AmmUtil.normalizeNewlines(
        s"""object $aliasedWrapperName {
           |  val alias = $wrapperObjectName
           |}
           |""".stripMargin
      )
    else ""

    val extendsClause = if useDelayedInit then "extends scala.cli.build.ScalaCliApp " else ""

    val top = AmmUtil.normalizeNewlines(
      s"""$packageDirective
         |
         |object $wrapperObjectName $extendsClause{
         |def args = $mainObjectName.args$$
         |def scriptPath = \"\"\"$scriptPath\"\"\"
         |""".stripMargin
    )
    val bottom = AmmUtil.normalizeNewlines(
      s"""
         |$extraCode
         |}
         |
         |$mainObject
         |
         |$aliasObject
         |""".stripMargin
    )

    (top, bottom)
  }

  override def additionalSourceCode: Option[(os.RelPath, String)] = Option.when(useDelayedInit)(
    os.RelPath("delayed-init-wrapper.scala") ->
      // This is copied from Scala 2 implementation of App trait, but with no main method and no argument handling
      """package scala.cli.build
        |
        |trait ScalaCliApp extends DelayedInit {
        |  private[this] val initCode = new scala.collection.mutable.ListBuffer[() => Unit]
        |
        |  override def delayedInit(body: => Unit): Unit = {
        |    initCode += (() => body)
        |  }
        |
        |  def run(): Unit = {
        |    for (proc <- initCode) proc()
        |  }
        |}
        |""".stripMargin
  )
}
