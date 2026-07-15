package scala.build.internal

/** Script code wrapper that solves problem of deadlocks when using threads. The code is placed in a
  * class instance constructor, the created object is kept in 'mainObjectCode'.script to support
  * running interconnected scripts using Scala CLI <br> <br> Incompatible with Scala 2 - it uses
  * Scala 3 feature 'export'<br> Incompatible with native JS members - the wrapper is a class
  */
case class ClassCodeWrapper(
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

    val mainObject     = WrapperUtils.mainObjectInScript(scalaVersion, code)
    val mainInvocation = mainObject match
      case WrapperUtils.ScriptMainMethod.Exists(name) => s"script.$name.main(args)"
      case otherwise                                  =>
        otherwise.warningMessage.foreach(log)
        s"val _ = script.hashCode()"

    val names            = WrapperUtils.ScriptWrapperNames(useDollarNames)
    val name             = mainClassObject(indexedWrapperName).backticked
    val wrapperClassName =
      scala.build.internal.Name(indexedWrapperName.raw ++ names.classSuffix).backticked
    val mainObjectCode = WrapperUtils.scriptMainObjectCode(
      names = names,
      objectName = name,
      mainInvocation = mainInvocation,
      extraBody = s"lazy val script = new $wrapperClassName",
      exportLine = Some(s"export $name.script as `${indexedWrapperName.raw}`")
    )

    val packageDirective =
      if (pkgName.isEmpty) "" else s"package ${AmmUtil.encodeScalaSourcePath(pkgName)}" + "\n"

    val top = AmmUtil.normalizeNewlines(
      s"""$packageDirective
         |
         |final class $wrapperClassName {
         |def args = $name.${names.argsAccessor}
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
