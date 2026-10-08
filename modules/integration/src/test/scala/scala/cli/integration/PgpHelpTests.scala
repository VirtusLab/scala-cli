package scala.cli.integration

class PgpHelpTests extends ScalaCliSuite {
  val helpFlag = "--help"

  for (subCommand <- Seq("create", "key-id", "sign", "verify"))
    test(s"pgp $subCommand $helpFlag") {
      TestInputs.empty.fromRoot { root =>
        os.proc(TestUtil.cli, "--power", "pgp", subCommand, helpFlag).call(cwd = root)
      }
    }
}
