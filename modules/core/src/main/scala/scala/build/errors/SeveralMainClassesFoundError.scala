package scala.build.errors

import scala.build.Position

final class SeveralMainClassesFoundError(
  mainClasses: ::[String],
  commandString: String,
  positions: Seq[Position]
) extends MainClassError(
      s"""Found several main classes: ${mainClasses.mkString(", ")}
         |You can run one of them by passing it with the --main-class option, e.g.
         |  ${Console.BOLD}$commandString --main-class ${mainClasses.head}${Console.RESET}
         |
         |You can pick the main class interactively by passing the --interactive option.
         |  ${Console.BOLD}$commandString --interactive${Console.RESET}""".stripMargin,
      positions = positions
    )
