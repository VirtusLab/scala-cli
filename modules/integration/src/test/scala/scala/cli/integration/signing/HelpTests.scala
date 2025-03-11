package scala.cli.signing.integration

class HelpTests extends CliSuite {
  val helpFlag = "--help"

  test(s"root $helpFlag") {
    TestInputs.empty.fromRoot { root =>
      os.proc(TestUtil.cli, helpFlag).call(cwd = root)
    }
  }
  test(s"pgp create $helpFlag") {
    TestInputs.empty.fromRoot { root =>
      os.proc(TestUtil.cli, "pgp", "create", helpFlag).call(cwd = root)
    }
  }
  test(s"pgp key-id $helpFlag") {
    TestInputs.empty.fromRoot { root =>
      os.proc(TestUtil.cli, "pgp", "key-id", helpFlag).call(cwd = root)
    }
  }
  test(s"pgp sign $helpFlag") {
    TestInputs.empty.fromRoot { root =>
      os.proc(TestUtil.cli, "pgp", "sign", helpFlag).call(cwd = root)
    }
  }
  test(s"pgp verify $helpFlag") {
    TestInputs.empty.fromRoot { root =>
      os.proc(TestUtil.cli, "pgp", "verify", helpFlag).call(cwd = root)
    }
  }

}
