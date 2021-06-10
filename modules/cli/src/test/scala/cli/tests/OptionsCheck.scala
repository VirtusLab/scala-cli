package scala.cli.tests

import scala.cli.ScalaCli

class OptionsCheck extends munit.FunSuite {

  for (command <- ScalaCli.commands)
    test(s"No duplicated options in ${command.names.head.mkString(" ")}") {
      command.ensureNoDuplicates()
    }

}
