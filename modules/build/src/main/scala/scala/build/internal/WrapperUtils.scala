package scala.build.internal

import scala.build.internal.util.WarningMessages

object WrapperUtils {

  /** Internal names used by script wrappers for args handling and wrapper class/alias suffixes. */
  final case class ScriptWrapperNames(
    argsOpt0: String,
    argsSet: String,
    argsOpt: String,
    argsArray: String,
    classSuffix: String,
    aliasSuffix: String
  ) {
    def argsAccessor: String = argsArray
  }

  object ScriptWrapperNames {
    def apply(useDollarNames: Boolean): ScriptWrapperNames =
      if useDollarNames then
        ScriptWrapperNames(
          argsOpt0 = "args$opt0",
          argsSet = "args$set",
          argsOpt = "args$opt",
          argsArray = "args$",
          classSuffix = "$_",
          aliasSuffix = "$$alias"
        )
      else
        ScriptWrapperNames(
          argsOpt0 = "scalaCliArgsOpt0",
          argsSet = "scalaCliArgsSet",
          argsOpt = "scalaCliArgsOpt",
          argsArray = "scalaCliArgs",
          classSuffix = "_class",
          aliasSuffix = "_alias"
        )
  }

  def scriptMainObjectCode(
    names: ScriptWrapperNames,
    objectName: String,
    mainInvocation: String,
    extraBody: String = "",
    exportLine: Option[String] = None
  ): String = {
    val extraBody0   = if extraBody.isEmpty then "" else extraBody + "\n  "
    val exportSuffix = exportLine.map(e => s"\n\n$e").getOrElse("")
    AmmUtil.normalizeNewlines(
      s"""|object $objectName {
          |  private var ${names.argsOpt0} = Option.empty[Array[String]]
          |  def ${names.argsSet}(args: Array[String]): Unit = {
          |    ${names.argsOpt0} = Some(args)
          |  }
          |  def ${names.argsOpt}: Option[Array[String]] = ${names.argsOpt0}
          |  def ${names.argsArray}: Array[String] = ${names.argsOpt}.getOrElse {
          |    sys.error("No arguments passed to this script")
          |  }
          |
          |  ${extraBody0}def main(args: Array[String]): Unit = {
          |    ${names.argsSet}(args)
          |    $mainInvocation // hashCode to clear scalac warning about pure expression in statement position
          |  }
          |}$exportSuffix
          |""".stripMargin
    )
  }

  enum ScriptMainMethod:
    case Exists(name: String)
    case Multiple(names: Seq[String])
    case ToplevelStatsPresent
    case ToplevelStatsWithMultiple(names: Seq[String])
    case NoMain

    def warningMessage: List[String] =
      this match
        case ScriptMainMethod.Multiple(names) =>
          List(WarningMessages.multipleMainObjectsInScript(names))
        case ScriptMainMethod.ToplevelStatsPresent => List(
            WarningMessages.mixedToplvelAndObjectInScript
          )
        case ToplevelStatsWithMultiple(names) =>
          List(
            WarningMessages.multipleMainObjectsInScript(names),
            WarningMessages.mixedToplvelAndObjectInScript
          )
        case _ => Nil

  def mainObjectInScript(scalaVersion: String, code: String): ScriptMainMethod =
    import scala.meta.*

    val scriptDialect =
      if scalaVersion.startsWith("3") then dialects.Scala3Future else dialects.Scala213Source3

    given Dialect  = scriptDialect.withAllowToplevelStatements(true).withAllowToplevelTerms(true)
    val parsedCode = code.parse[Source] match
      case Parsed.Success(Source(stats)) => stats
      case _                             => Nil

    // Check if there is a main function defined inside an object
    def checkSignature(defn: Defn.Def) =
      defn.paramClauseGroups match
        case List(Member.ParamClauseGroup(
              Type.ParamClause(Nil),
              List(Term.ParamClause(
                List(Term.Param(
                  Nil,
                  _: Term.Name,
                  Some(Type.Apply.After_4_6_0(
                    Type.Name("Array"),
                    Type.ArgClause(List(Type.Name("String")))
                  )),
                  None
                )),
                None
              ))
            )) => true
        case _ => false

    def noToplevelStatements = parsedCode.forall {
      case _: Term => false
      case _       => true
    }

    def hasMainSignature(templ: Template) = templ.body.stats.exists {
      case defn: Defn.Def =>
        defn.name.value == "main" && checkSignature(defn)
      case _ => false
    }
    def extendsApp(templ: Template) = templ.inits match
      case Init.After_4_6_0(Type.Name("App"), _, Nil) :: Nil => true
      case _                                                 => false
    val potentialMains = parsedCode.collect {
      case Defn.Object(_, objName, templ) if extendsApp(templ) || hasMainSignature(templ) =>
        Seq(objName.value)
    }.flatten

    potentialMains match
      case head :: Nil if noToplevelStatements =>
        ScriptMainMethod.Exists(head)
      case head :: Nil =>
        ScriptMainMethod.ToplevelStatsPresent
      case Nil                         => ScriptMainMethod.NoMain
      case seq if noToplevelStatements =>
        ScriptMainMethod.Multiple(seq)
      case seq =>
        ScriptMainMethod.ToplevelStatsWithMultiple(seq)

}
