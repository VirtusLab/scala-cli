package scala.build.internal

/** Script code wrapper compatible with Scala 2 and JS native members <br> <br> When using Scala 3
  * or/and not using JS native prefer [[ClassCodeWrapper]], since it prevents deadlocks when running
  * threads from script
  */
case class ObjectCodeWrapper(scalaVersion: String, log: String => Unit) extends CodeWrapper {

  override def mainClassObject(className: Name): Name =
    Name(className.raw ++ "_sc")
  def apply(
    code: String,
    pkgName: Seq[Name],
    indexedWrapperName: Name,
    extraCode: String,
    scriptPath: String
  ) = {
    val mainObject         = WrapperUtils.mainObjectInScript(scalaVersion, code)
    val name               = mainClassObject(indexedWrapperName).backticked
    val aliasedWrapperName = name + "$$alias"
    val realScript =
      if (name == "main_sc")
        s"$aliasedWrapperName.alias" // https://github.com/VirtusLab/scala-cli/issues/314
      else s"${indexedWrapperName.backticked}"

    val funHashCodeMethod = mainObject match
      case WrapperUtils.ScriptMainMethod.Exists(name) => s"$realScript.$name.main(args)"
      case otherwise =>
        otherwise.warningMessage.foreach(log)
        s"val _ = $realScript.hashCode()"
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
                                    |    $funHashCodeMethod // hashCode to clear scalac warning about pure expression in statement position
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

    val top = AmmUtil.normalizeNewlines(
      s"""$packageDirective
         |
         |object ${indexedWrapperName.backticked} {
         |def args = $name.args$$
         |def scriptPath = \"\"\"$scriptPath\"\"\"
         |""".stripMargin
    )

    val bottom = AmmUtil.normalizeNewlines(
      s"""$extraCode
         |}
         |$aliasObject
         |$mainObjectCode
         |""".stripMargin
    )

    (top, bottom)
  }
}
