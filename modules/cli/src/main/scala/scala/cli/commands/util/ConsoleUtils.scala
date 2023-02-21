package scala.cli.commands.util

object ConsoleUtils {
  import Console.*
  val ansiFormattingKeys: Set[String] = Set(RESET, BOLD, UNDERLINED, REVERSED, INVISIBLE)
  val ansiColors: Set[String]         = Set(BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE)
  val ansiBoldColors: Set[String] =
    Set(BLACK_B, RED_B, GREEN_B, YELLOW_B, BLUE_B, MAGENTA_B, CYAN_B, WHITE_B)
  val allAnsiColors: Set[String]  = ansiColors ++ ansiBoldColors
  val allConsoleKeys: Set[String] = allAnsiColors ++ ansiFormattingKeys

  extension (s: String) {
    def noConsoleKeys: String =
      allConsoleKeys.fold(s)((acc, consoleKey) => acc.replace(consoleKey, ""))
  }
}
