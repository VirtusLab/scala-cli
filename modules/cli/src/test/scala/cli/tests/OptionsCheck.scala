package scala.cli.tests

import scala.cli.ScalaCliCommands
import scala.cli.commands.shared.HasGlobalOptions

class OptionsCheck extends munit.FunSuite {

  for (
    command <-
      (new ScalaCliCommands("scala-cli", "scala-cli", "Scala CLI", isSipScala = false)).commands
  )
    test(s"No duplicated options in ${command.names.head.mkString(" ")}") {
      command.ensureNoDuplicates()
    }

    test(s"--power option present in $command") {
      command.parser.stopAtFirstUnrecognized.parse(Seq("--power")) match {
        case Right((_: HasGlobalOptions, _ +: _)) => fail("Expected --power to be recognized")
        case _                                    => ()
      }
    }
}
