package scala.cli.commands

import caseapp.Name
import caseapp.core.app.Command
import caseapp.core.parser.Parser
import caseapp.core.util.Formatter
import caseapp.core.{Arg, Error}

object RestrictedCommandsParser {
  def isExperimentalOrRestricted(a: Arg) =
    a.tags.exists(_.name == tags.restricted) || a.tags.exists(_.name == tags.experimental)

  def level(a: Arg) =
    a.tags.flatMap(t => tags.levelFor(t.name)).headOption match
      case Some(opt) =>
        if (
          a.noHelp && opt != SpecificationLevel.EXPERIMENTAL && opt != SpecificationLevel.RESTRICTED
        )
          println(s"$a defines level as ${a.name.name} but is hidden")
        opt
      case None if a.noHelp || a.group.exists(_.name == "Help") => SpecificationLevel.INTERNAL
      case None =>
        println(s"No level for ${a.name.name} from ${a.group}")
        SpecificationLevel.INTERNAL

  def apply[T, TD](parser: Parser.Aux[T, TD]): Parser.Aux[T, TD] = new Parser[T] {

    type D = TD

    private def isArgSupported(a: Arg): Boolean =
      scala.cli.ScalaCli.allowRestrictedFeatures || !isExperimentalOrRestricted(a)

    def args: Seq[caseapp.core.Arg] = parser.args.filter(isArgSupported)

    def get(
      d: D,
      nameFormatter: caseapp.core.util.Formatter[caseapp.Name]
    ): Either[caseapp.core.Error, T] =
      parser.get(d, nameFormatter)

    def init: D = parser.init

    def withDefaultOrigin(origin: String): caseapp.core.parser.Parser.Aux[T, D] =
      RestrictedCommandsParser(parser.withDefaultOrigin(origin))

    override def step(
      args: List[String],
      index: Int,
      d: D,
      nameFormatter: Formatter[Name]
    ): Either[(Error, Arg, List[String]), Option[(D, Arg, List[String])]] =
      parser.step(args, index, d, nameFormatter) match {
        case Right(Some(_, arg, _)) if !isArgSupported(arg) =>
          Left((
            Error.UnrecognizedArgument(
              s"`${args(index)}` option is not supported in `scala` command.\n  Please run it with `scala-cli` command or with `--power` flag."
            ),
            arg,
            Nil
          ))
        case other =>
          other
      }
  }
}
