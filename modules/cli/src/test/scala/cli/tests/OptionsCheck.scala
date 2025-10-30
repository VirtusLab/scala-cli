package scala.cli.tests

import cli.tests.TestUtil

import scala.cli.ScalaCliCommands
import scala.cli.commands.shared.HasGlobalOptions

class OptionsCheck extends TestUtil.ScalaCliSuite {
  for (command <- new ScalaCliCommands("scala-cli", "scala-cli", "Scala CLI").commands)
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
