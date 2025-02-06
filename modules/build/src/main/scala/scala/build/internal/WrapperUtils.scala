package scala.build.internal

object WrapperUtils {

  def mainObjectInScript(scalaVersion: String, code: String): Option[String] =
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
    parsedCode.collect {
      case Defn.Object(_, objName, templ) =>
        templ.body.stats.find {
          case defn: Defn.Def =>
            defn.name.value == "main" && checkSignature(defn)
          case _ => false
        }.map(_ => objName.value)
    }.flatten.headOption
}
