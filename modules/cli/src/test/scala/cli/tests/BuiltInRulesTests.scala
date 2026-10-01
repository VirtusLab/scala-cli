package cli.tests

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.commands.fix.BuiltInRules

class BuiltInRulesTests extends TestUtil.ScalaCliSuite {
  test("withoutCommaSeparators removes deprecated comma separators only") {
    val content =
      """#!/usr/bin/env -S scala-cli shebang
        |//> using dep com.lihaoyi::os-lib:0.11.4, com.lihaoyi::pprint:0.9.0
        |//> using options -Werror, -Wunused:imports,privates "-Xfoo, bar"
        |//> using scala 3
        |
        |val s = "a, b"
        |""".stripMargin
    val expected =
      """#!/usr/bin/env -S scala-cli shebang
        |//> using dep com.lihaoyi::os-lib:0.11.4 com.lihaoyi::pprint:0.9.0
        |//> using options -Werror -Wunused:imports,privates "-Xfoo, bar"
        |//> using scala 3
        |
        |val s = "a, b"
        |""".stripMargin
    expect(BuiltInRules.withoutCommaSeparators(content) == expected)
  }

  test("withoutCommaSeparators leaves content without comma separators untouched") {
    val content =
      """//> using dep "tabby:tabby:0.2.3,url=https://example.com/tabby.jar"
        |object Main
        |""".stripMargin
    expect(BuiltInRules.withoutCommaSeparators(content) == content)
  }

  test("withoutCommaSeparators handles non-canonical spacing after //>") {
    val content =
      """//>using dep a:b:1, c:d:2
        |//>   using options -Werror, -deprecation
        |object Main
        |""".stripMargin
    val expected =
      """//>using dep a:b:1 c:d:2
        |//>   using options -Werror -deprecation
        |object Main
        |""".stripMargin
    expect(BuiltInRules.withoutCommaSeparators(content) == expected)
  }

  test("withoutCommaSeparators quotes values ending with a comma") {
    val content =
      """//> using javaOpt -Dfoo=a,, -Dbar=b ,, -Dbaz=c
        |//> using javaOpt -Ddir=C:\dir,, -Dqux=d
        |object Main
        |""".stripMargin
    val expected =
      """//> using javaOpt "-Dfoo=a," -Dbar=b "," -Dbaz=c
        |//> using javaOpt "-Ddir=C:\\dir," -Dqux=d
        |object Main
        |""".stripMargin
    val fixed = BuiltInRules.withoutCommaSeparators(content)
    expect(fixed == expected)
    expect(BuiltInRules.withoutCommaSeparators(fixed) == fixed)
  }
}
