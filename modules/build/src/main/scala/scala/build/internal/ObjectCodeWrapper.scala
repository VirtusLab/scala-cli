package scala.build.internal

/** Script code wrapper compatible with Scala 2 and JS native members <br> <br> When using Scala 3
  * or/and not using JS native prefer [[ClassCodeWrapper]], since it prevents deadlocks when running
  * threads from script
  */
case class ObjectCodeWrapper(
  scalaVersion: String,
  log: String => Unit,
  useDollarNames: Boolean = false
) extends CodeWrapper {

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
    val names              = WrapperUtils.ScriptWrapperNames(useDollarNames)
    val name               = mainClassObject(indexedWrapperName).backticked
    val aliasedWrapperName = name + names.aliasSuffix
    val realScript         =
      if (name == "main_sc")
        s"$aliasedWrapperName.alias" // https://github.com/VirtusLab/scala-cli/issues/314
      else s"${indexedWrapperName.backticked}"

    val funHashCodeMethod = mainObject match
      case WrapperUtils.ScriptMainMethod.Exists(name) => s"$realScript.$name.main(args)"
      case otherwise                                  =>
        otherwise.warningMessage.foreach(log)
        s"val _ = $realScript.hashCode()"
    val mainObjectCode = WrapperUtils.scriptMainObjectCode(
      names = names,
      objectName = name,
      mainInvocation = funHashCodeMethod
    )

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
         |def args = $name.${names.argsAccessor}
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
