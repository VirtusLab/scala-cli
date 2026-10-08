package scala.cli.signing.integration

class PgpRoundTripTests extends CliSuite {
  test("pgp create + sign + verify round-trip (decrypts secret key via BouncyCastle)") {
    TestInputs(os.rel / "foo.txt" -> "Hello, world!\n").fromRoot { root =>
      val password = "value:1234"
      os.proc(
        TestUtil.cli,
        "pgp",
        "create",
        "--email",
        "test@example.com",
        "--password",
        password,
        "--dest",
        "key"
      ).call(cwd = root)
      val secretKey = root / "key.skr"
      assert(os.exists(secretKey) && os.exists(root / "key.pub"))

      os.proc(
        TestUtil.cli,
        "pgp",
        "sign",
        "--secret-key",
        s"file:$secretKey",
        "--password",
        password,
        "foo.txt"
      ).call(cwd = root)
      assert(os.exists(root / "foo.txt.asc"))

      os.proc(
        TestUtil.cli,
        "pgp",
        "verify",
        "--key",
        "key.pub",
        "foo.txt.asc"
      ).call(cwd = root)
    }
  }
}
