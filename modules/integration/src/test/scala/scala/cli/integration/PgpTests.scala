package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class PgpTests extends ScalaCliSuite {

  private val pubKeyInputs = TestInputs(
    os.rel / "key.pub" ->
      """-----BEGIN PGP PUBLIC KEY BLOCK-----
        |Version: BCPG v1.68
        |
        |mQENBGKDqDYDCACw/2akQ1h6NHmtcEKQThq7OMdExyJ4WUKWdr+iV3BFx/AZICRd
        |zU/tbDzvhqnwgBQalHfkcO8d1zIhzHhb3Hq3zo3Z0Gd1DcevODU7v2/GJkV/ici2
        |xScYqJAmxkIM8yANiHrDZ17XrZ9m44YcK7qV9Z+PP2SNh3WtDPRzR//vL2yZgYm/
        |KfzTuOzHqmne6Z0dVnc9jZTR6e7kNTHqIPpKvqIxFoqn2t0T6QZafYD1DdDmonyj
        |1ME33vEec2llgJmIGHIVN3EcmQnBtfRP3mEqRZHmUGSAIrdqtLBegum0q8rvEBI4
        |VIbg+xx7/5qnlfCIaCji4r/C2FlkrSiQgr+VABEBAAG0H2FsZXhhbmRyZS5hcmNo
        |YW1iYXVsdEBnbWFpbC5jb22JAS4EEwMCABgFAmKDqDYCGwMECwkIBwYVCAIJCgsC
        |HgEACgkQkU0pjfj6TSARjggAgbTFf1vjvHb1lwYP6jA5g3WFKHFSbh/fid/uOygq
        |FqdD29KoE2tqmC9sE0koYMBpSJK4as5RDBKhliw8j60eBoYjRJZIDoc5oQQ7N7ul
        |n0t5KMwpJ1CJs8G1BnU747Efg89blb3tY6LG4L+vziQ9Uw0d/0qizT1OWxGshqpA
        |PJk4tHwQ3aujGbmVWttietdFVndcB4BB1s7UlGHFtwcAQyz5R8B3dfnaU4czyzM8
        |zp+ltPXjLkuGvI/Bz3f15TZiUr1zEw0/ZAMmDdZvf8tGt8hhAp8n3DXL7HV4azdv
        |iwZUribQnr1MHadTlKmUxz7+VfYNm8gkSRmGBl4YO5dl0rkBDQRig6g2AggApW+O
        |XrTJrWM8asyur3UDOyXpvBZ1PeLIXyTrjVOHSiqqFjI8/P3faG6MQWv9YUN3cYIL
        |SnIX9rAgchRpEWivqaYpj2hfyLOMkDUhw0owPo7vFXAX8uUXK5Fg/krSwGukY9+Z
        |HcjzC9+CTEYZrBcxtziOWoGyOOk1Wepi1EqTWVB/xuY+hHWrhI5ff9bOdQmfcQL3
        |Z9r2nJR3ahe+Qu5ppNGiX2FTLBMl5XwL0pOjonLyd39EsdbCbQvMnLgYwxwKMOht
        |OPyXJAeOolchht50FWerTdf4r8fLndCiYg7hi5i0+GxWZwBj3MXNnkyQ0jjwiO/y
        |p64RL2gJMrFwn7bv5wARAQABiQEfBBgDAgAJBQJig6g3AhsMAAoJEJFNKY34+k0g
        |t1QH/06YazNGeuLzc62Mamnr8kA0AakGJxPZ83rGXcdahBRd9Enga32pcEks6YPI
        |OZBbayEoIf4CasSaz9H/Bn1l91L60AEYeBwj8CFYx2ZZrC+ywdFkgbrVFP0N1Doj
        |TMxim2rAJ8OFH2kczmhaG9HRL6V7kjGpb/tGdVpjgdt4V4NMDGQc4AWTWVCBcQKa
        |3cHnXDgKrCE1inUej1bJ5g2SHm+gMyF7WAgbVi9r1/suu4d1WjJuUEQ28FmjpeEd
        |CXxI+5gGgs4H7rUbTb1DScYsb4/j/Y/7SsOqnmz/SFA9Ej0scSaVviFwS06Q5LZN
        |mnSk2Og334LRowks7+/8CEofK/w=
        |=RTOn
        |-----END PGP PUBLIC KEY BLOCK-----
        |""".stripMargin
  )

  def pgpKeyIdTest(useSigningJvmLauncher: Boolean) =
    pubKeyInputs.fromRoot { root =>
      val signingCliArgs = if (useSigningJvmLauncher) Seq("--force-jvm-signing-cli") else Seq.empty
      val res = os.proc(TestUtil.cli, "pgp", "key-id", signingCliArgs, "key.pub").call(cwd = root)
      val output         = res.out.trim()
      val expectedOutput = "914d298df8fa4d20"
      expect(output == expectedOutput)
    }

  test("pgp key-id") {
    pgpKeyIdTest(false)
  }

  if (TestUtil.isNativeCli)
    test("pgp key-id - use jvm launcher of signing cli for native Scala CLI") {
      pgpKeyIdTest(true)
    }

  test("pgp pull") {
    // random key that I pushed to the default ker server at some point
    val res = os.proc(TestUtil.cli, "pgp", "pull", "0x914d298df8fa4d20")
      .call()
    val output = res.out.trim()
    val start  = "-----BEGIN PGP PUBLIC KEY BLOCK-----"
    val end    = "-----END PGP PUBLIC KEY BLOCK-----"
    val expectedLines = Seq(
      "xsBNBGKDqDYDCACw/2akQ1h6NHmtcEKQThq7OMdExyJ4WUKWdr+iV3BFx/AZICRd",
      "t1QH/06YazNGeuLzc62Mamnr8kA0AakGJxPZ83rGXcdahBRd9Enga32pcEks6YPI",
      "mnSk2Og334LRowks7+/8CEofK/w="
    )
    expect(output.startsWith(start))
    expect(output.endsWith(end))
    val outputLines = output.linesIterator.toSet
    for (expectedLine <- expectedLines)
      expect(outputLines.contains(expectedLine))
  }

  test("pgp push") {
    pubKeyInputs.fromRoot { root =>
      os.proc(TestUtil.cli, "pgp", "push", "key.pub").call(cwd = root)
    }
  }

  if (!TestUtil.isNativeCli)
    test("pgp push with binary") {
      pubKeyInputs.fromRoot { root =>
        os.proc(TestUtil.cli, "pgp", "push", "key.pub", "--force-signing-binary").call(cwd = root)
      }
    }

}
