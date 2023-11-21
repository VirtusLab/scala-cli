package scala.build.internal

/** Script code wrapper that solves problem of deadlocks when using threads. The code is placed in a
  * class instance constructor, the created object is kept in 'mainObjectCode'.script to support
  * running interconnected scripts using Scala CLI <br> <br> Incompatible with Scala 2 - it uses
  * Scala 3 feature 'export'<br> Incompatible with native JS members - the wrapper is a class
  */
case object ClassCodeWrapper extends CodeWrapper {

  override def mainClassObject(className: Name): Name =
    Name(className.raw ++ "_sc")
  def apply(
    code: String,
    pkgName: Seq[Name],
    indexedWrapperName: Name,
    extraCode: String,
    scriptPath: String
  ) = {
    val name             = mainClassObject(indexedWrapperName).backticked
    val wrapperClassName = Name(indexedWrapperName.raw ++ "$_").backticked
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
                                    |
                                    |  lazy val script = new $wrapperClassName
                                    |
                                    |  def main(args: Array[String]): Unit = {
                                    |    args$$set(args)
                                    |    val _ = script.hashCode() // hashCode to clear scalac warning about pure expression in statement position
                                    |  }
                                    |}
                                    |
                                    |export $name.script as `${indexedWrapperName.raw}`
                                    |""".stripMargin)

    val packageDirective =
      if (pkgName.isEmpty) "" else s"package ${AmmUtil.encodeScalaSourcePath(pkgName)}" + "\n"

    val top = AmmUtil.normalizeNewlines(
      s"""$packageDirective
         |
         |final class $wrapperClassName {
         |def args = $name.args$$
         |def scriptPath = \"\"\"$scriptPath\"\"\"
         |""".stripMargin
    )
    val bottom = AmmUtil.normalizeNewlines(
      s"""$extraCode
         |}
         |
         |$mainObjectCode
         |""".stripMargin
    )

    (top, bottom)
  }
}
