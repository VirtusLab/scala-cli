package scala.build.internal

import scala.build.internal.util.WarningMessages

object WrapperUtils {

  enum ScriptMainMethod:
    case Exists(name: String)
    case Multiple(names: Seq[String])
    case ToplevelStatsPresent
    case NoMain

    def warningMessage: Option[String] =
      this match
        case ScriptMainMethod.Multiple(names) =>
          Some(WarningMessages.multipleMainObjectsInScript(names))
        case ScriptMainMethod.ToplevelStatsPresent => Some(
            WarningMessages.mixedToplvelAndObjectInScript
          )
        case _ => None

  def mainObjectInScript(scalaVersion: String, code: String): ScriptMainMethod =
    import scala.meta.*

    val scriptDialect =
      if scalaVersion.startsWith("3") then dialects.Scala3Future else dialects.Scala213Source3

    given Dialect = scriptDialect.withAllowToplevelStatements(true).withAllowToplevelTerms(true)
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
      case Nil => ScriptMainMethod.NoMain
      case seq =>
        ScriptMainMethod.Multiple(seq)

}
