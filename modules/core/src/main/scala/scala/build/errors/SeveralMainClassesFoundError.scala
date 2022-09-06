package scala.build.errors

import scala.build.Position

final class SeveralMainClassesFoundError(
  mainClasses: ::[String],
  commandString: String,
  positions: Seq[Position]
) extends MainClassError(
      {
        val sortedMainClasses = mainClasses.sorted
        val mainClassesString = sortedMainClasses.mkString(", ")
        s"""Found several main classes: $mainClassesString
           |${sortedMainClasses.headOption.map(mc =>
            s"""You can run one of them by passing it with the --main-class option, e.g.
               |  ${Console.BOLD}$commandString --main-class $mc${Console.RESET}
               |""".stripMargin
          ).getOrElse("")}
           |You can pick the main class interactively by passing the --interactive option.
           |  ${Console.BOLD}$commandString --interactive${Console.RESET}""".stripMargin
      },
      positions = positions
    )
