package scala.cli.parse

import munit.FunSuite

class ParserTests extends FunSuite:

  private def parse(src: String): UsingDirectivesResult =
    UsingDirectivesParser.parse(src.toCharArray)

  private def directives(src: String): Seq[UsingDirective] =
    parse(src).directives

  private def warnings(src: String): Seq[UsingDirectiveDiagnostic] =
    parse(src).diagnostics.filter(_.severity == DiagnosticSeverity.Warning)

  // -----------------------------------------------------------------------
  // Basic parsing
  // -----------------------------------------------------------------------

  test("parse simple directive key and value") {
    val ds = directives(
      """//> using scala 3
        |""".stripMargin
    )
    assertEquals(ds.length, 1)
    assertEquals(ds.head.key, "scala")
    assertEquals(ds.head.values.length, 1)
    val v = ds.head.values.head
    v match
      case sv: DirectiveValue.StringVal => assertEquals(sv.value, "3")
      case _                            => fail(s"expected StringVal, got $v")
  }

  test("parse dotted key") {
    val ds = directives(
      """//> using test.dep munit::munit:1.0.0
        |""".stripMargin
    )
    assertEquals(ds.head.key, "test.dep")
  }

  test("parse multiple values") {
    val ds = directives(
      """//> using dep com.lihaoyi::os-lib:0.11.4 com.lihaoyi::upickle:3.1.0
        |""".stripMargin
    )
    val values = ds.head.values
    assertEquals(values.length, 2)
  }

  test("parse quoted string value") {
    val src =
      """//> using scalacOption "-Xfatal-warnings"
        |""".stripMargin
    val ds = directives(src)
    val v  = ds.head.values.head
    v match
      case sv: DirectiveValue.StringVal =>
        assertEquals(sv.value, "-Xfatal-warnings")
        assert(sv.isQuoted)
      case _ => fail(s"expected StringVal, got $v")
  }

  test("parse boolean true") {
    val ds = directives(
      """//> using publish.doc true
        |""".stripMargin
    )
    val v = ds.head.values.head
    v match
      case bv: DirectiveValue.BoolVal => assertEquals(bv.value, true)
      case _                          => fail(s"expected BoolVal, got $v")
  }

  test("parse boolean false") {
    val ds = directives(
      """//> using publish.doc false
        |""".stripMargin
    )
    val v = ds.head.values.head
    v match
      case bv: DirectiveValue.BoolVal => assertEquals(bv.value, false)
      case _                          => fail(s"expected BoolVal, got $v")
  }

  test("directive with no values produces EmptyVal") {
    val ds = directives(
      """//> using toolkit
        |""".stripMargin
    )
    assertEquals(ds.head.values.length, 1)
    ds.head.values.head match
      case _: DirectiveValue.EmptyVal => ()
      case v                          => fail(s"expected EmptyVal, got $v")
  }

  test("multiple directives") {
    val src =
      """//> using scala 3
        |//> using dep foo
        |""".stripMargin
    val ds = directives(src)
    assertEquals(ds.length, 2)
    assertEquals(ds(0).key, "scala")
    assertEquals(ds(1).key, "dep")
  }

  // -----------------------------------------------------------------------
  // Comma deprecation
  // -----------------------------------------------------------------------

  test("comma separator emits deprecation warning") {
    val ws = warnings(
      """//> using dep a, b
        |""".stripMargin
    )
    assert(ws.nonEmpty, s"expected warnings, got none")
    assert(ws.exists(_.message.contains("deprecated")))
  }

  test("each comma as separator emits its own deprecation warning") {
    val ws = warnings(
      """//> using dep a, b, c
        |""".stripMargin
    )
    val deprecationMsgs = ws.filter(_.message.contains("Use of commas as separators"))
    assertEquals(deprecationMsgs.length, 2)
  }

  test("comma separator still parses both values") {
    val ds = directives(
      """//> using dep a, b
        |""".stripMargin
    )
    assertEquals(ds.head.values.length, 2)
  }

  test("embedded comma (no space) does NOT emit warning") {
    val ws = warnings(
      """//> using packaging.graalvmArgs --enable-url-protocols=http,https
        |""".stripMargin
    )
    assertEquals(ws.length, 0)
  }

  // -----------------------------------------------------------------------
  // Position tracking
  // -----------------------------------------------------------------------

  test("positions are correct for single directive") {
    // "//> using scala 3"
    //  0123456789012345678
    //            ^key  ^value
    val ds = directives(
      """//> using scala 3
        |""".stripMargin
    )
    val kp = ds.head.keyPosition
    assertEquals(kp.line, 0)
    assertEquals(kp.column, 10)
    assertEquals(kp.offset, 10)
    val vp = ds.head.values.head.pos
    assertEquals(vp.column, 16)
  }

  test("positions are correct for second directive on next line") {
    val src =
      """//> using scala 3
        |//> using dep foo
        |""".stripMargin
    val ds = directives(src)
    val kp = ds(1).keyPosition
    assertEquals(kp.line, 1)
    // "//> using scala 3\n" is 18 chars, then "//> using " is 10 more = offset 28
    assertEquals(kp.offset, 28)
  }

  // -----------------------------------------------------------------------
  // Diagnostics
  // -----------------------------------------------------------------------

  test("directive after code is ignored with warning") {
    val src =
      """val x = 1
        |//> using scala 3
        |""".stripMargin
    val r = parse(src)
    assertEquals(r.directives.length, 0)
    assert(r.diagnostics.exists(_.severity == DiagnosticSeverity.Warning))
  }

  // -----------------------------------------------------------------------
  // codeOffset
  // -----------------------------------------------------------------------

  test("codeOffset after single directive") {
    val src =
      """//> using scala 3
        |val x = 1
        |""".stripMargin
    val r = parse(src)
    assertEquals(r.codeOffset, 18)
  }

  test("codeOffset with no directives is 0") {
    val src =
      """val x = 1
        |""".stripMargin
    val r = parse(src)
    assertEquals(r.codeOffset, 0)
  }

  test("codeOffset at end of file when only directives") {
    val src =
      """//> using scala 3
        |""".stripMargin
    val r = parse(src)
    assertEquals(r.codeOffset, src.length)
  }

  // -----------------------------------------------------------------------
  // Edge cases
  // -----------------------------------------------------------------------

  test("empty source") {
    val r = parse("")
    assertEquals(r.directives.length, 0)
    assertEquals(r.codeOffset, 0)
  }

  test("source with only blank lines") {
    val r = parse("\n\n\n")
    assertEquals(r.directives.length, 0)
  }

  test("value containing colon") {
    val ds = directives(
      """//> using dep com.lihaoyi::os-lib:0.11.4
        |""".stripMargin
    )
    ds.head.values.head match
      case sv: DirectiveValue.StringVal =>
        assertEquals(sv.value, "com.lihaoyi::os-lib:0.11.4")
      case v => fail(s"expected StringVal, got $v")
  }

  test("directive-like text in all comment types produces no directives or errors") {
    val src =
      """// line comment '//> using ...'
        |/* block comment '//> using ...' */
        |/** ScalaDoc '//> using ...' */
        |""".stripMargin
    val r = parse(src)
    assertEquals(r.directives.length, 0)
    assertEquals(r.diagnostics.length, 0)
  }

  test("ScalaDoc before real directive does not interfere") {
    val src =
      """/** //> using dep fake */
        |//> using scala 3
        |val x = 1
        |""".stripMargin
    val r = parse(src)
    assertEquals(r.directives.length, 1)
    assertEquals(r.directives.head.key, "scala")
    assertEquals(r.diagnostics.length, 0)
  }

  test("package before directives causes directives to be ignored with warning") {
    val src =
      """package x
        |//> using scala 3.4.2
        |//> using dep foo
        |@main def run = println(42)
        |""".stripMargin
    val r = parse(src)
    assertEquals(r.directives.length, 0)
    assertEquals(r.codeOffset, 0)
    val ws = r.diagnostics.filter(_.severity == DiagnosticSeverity.Warning)
    assertEquals(ws.length, 2)
    assert(ws(0).message.contains("//> using scala 3.4.2"))
    assert(ws(1).message.contains("//> using dep foo"))
  }

  test("post-code directive warning includes directive text") {
    val src =
      """val x = 1
        |//> using scala 3
        |""".stripMargin
    val ws = warnings(src)
    assertEquals(ws.length, 1)
    assert(ws.head.message.contains("Ignoring using directive found after Scala code"))
    assert(ws.head.message.contains("//> using scala 3"))
  }

  test("indented directive is parsed correctly end-to-end") {
    val src =
      """  //> using scala 3
        |val x = 1
        |""".stripMargin
    val r = parse(src)
    assertEquals(r.directives.length, 1)
    assertEquals(r.directives.head.key, "scala")
    val v = r.directives.head.values.head
    v match
      case sv: DirectiveValue.StringVal => assertEquals(sv.value, "3")
      case other                        => fail(s"expected StringVal, got $other")
  }

  test("inline block comment in directive line is not supported (key becomes /*)") {
    val src =
      """//> using /* comment */ dep foo
        |""".stripMargin
    val r = parse(src)
    assertEquals(r.directives.length, 1)
    assertEquals(r.directives.head.key, "/*")
  }

  private def errors(src: String): Seq[UsingDirectiveDiagnostic] =
    parse(src).diagnostics.filter(_.severity == DiagnosticSeverity.Error)

  test("`//> using` alone emits error about missing key") {
    val errs = errors(
      """//> using
        |""".stripMargin
    )
    assertEquals(errs.length, 1)
    assert(errs.head.message.contains("Expected a key after `using`"))
  }

  test("`using` as key emits error") {
    val errs = errors(
      """//> using using foo
        |""".stripMargin
    )
    assertEquals(errs.length, 1)
    assert(errs.head.message.contains("Expected a key after `using`"))
    assert(errs.head.message.contains("Using"))
  }

  test("`true` as key emits error") {
    val errs = errors(
      """//> using true foo
        |""".stripMargin
    )
    assertEquals(errs.length, 1)
    assert(errs.head.message.contains("Expected a key after `using`"))
    assert(errs.head.message.contains("BoolLit"))
  }

  test("`false` as key emits error") {
    val errs = errors(
      """//> using false foo
        |""".stripMargin
    )
    assertEquals(errs.length, 1)
    assert(errs.head.message.contains("Expected a key after `using`"))
  }

  test("key with trailing dot is accepted by parser") {
    val ds = directives(
      """//> using foo. bar
        |""".stripMargin
    )
    assertEquals(ds.length, 1)
    assertEquals(ds.head.key, "foo.")
  }

  test("key with leading dot is accepted by parser") {
    val ds = directives(
      """//> using .foo bar
        |""".stripMargin
    )
    assertEquals(ds.length, 1)
    assertEquals(ds.head.key, ".foo")
  }

  test("comma with no spaces is a single value, no warnings") {
    val src =
      """//> using dep a,b
        |""".stripMargin
    val ds = directives(src)
    assertEquals(ds.head.values.length, 1)
    ds.head.values.head match
      case sv: DirectiveValue.StringVal => assertEquals(sv.value, "a,b")
      case v                            => fail(s"expected StringVal, got $v")
    assertEquals(warnings(src).length, 0)
  }

  test("space before comma but not after produces two values") {
    val src =
      """//> using dep a ,b
        |""".stripMargin
    val ds = directives(src)
    assertEquals(ds.head.values.length, 2)
    val vs = ds.head.values.collect { case sv: DirectiveValue.StringVal => sv.value }
    assertEquals(vs, Seq("a", ",b"))
    assertEquals(warnings(src).length, 0)
  }

  test("trailing comma produces 2 values and 2 deprecation warnings") {
    val src =
      """//> using dep a, b,
        |""".stripMargin
    val ds = directives(src)
    assertEquals(ds.head.values.length, 2)
    val vs = ds.head.values.collect { case sv: DirectiveValue.StringVal => sv.value }
    assertEquals(vs, Seq("a", "b"))
    val ws = warnings(src)
    assertEquals(ws.length, 2)
    assert(ws.forall(_.message.contains("deprecated")))
  }

  test("lone comma is treated as a literal value") {
    val src =
      """//> using dep ,
        |""".stripMargin
    val ds = directives(src)
    assertEquals(ds.head.values.length, 1)
    ds.head.values.head match
      case sv: DirectiveValue.StringVal => assertEquals(sv.value, ",")
      case v                            => fail(s"expected StringVal(','), got $v")
    assertEquals(warnings(src).length, 0)
  }

  test("double comma emits warning about value ending with comma") {
    val src =
      """//> using dep a,, b
        |""".stripMargin
    val ds = directives(src)
    val vs = ds.head.values.collect { case sv: DirectiveValue.StringVal => sv.value }
    assertEquals(vs, Seq("a,", "b"))
    val ws = warnings(src)
    assert(ws.exists(_.message.contains("ends with a comma")), s"warnings=$ws")
    assert(ws.exists(_.message.contains("deprecated")), s"warnings=$ws")
  }

  test("comma inside quoted string is preserved literally") {
    val src =
      """//> using dep "a,b"
        |""".stripMargin
    val ds = directives(src)
    assertEquals(ds.head.values.length, 1)
    ds.head.values.head match
      case sv: DirectiveValue.StringVal =>
        assertEquals(sv.value, "a,b")
        assert(sv.isQuoted)
      case v => fail(s"expected quoted StringVal, got $v")
    assertEquals(warnings(src).length, 0)
  }

  test("coursier dep with ,url= is a single value") {
    val src =
      """//> using dep tabby:tabby:0.2.3,url=https://example.com/tabby.jar
        |""".stripMargin
    val ds = directives(src)
    assertEquals(ds.head.values.length, 1)
    ds.head.values.head match
      case sv: DirectiveValue.StringVal =>
        assertEquals(sv.value, "tabby:tabby:0.2.3,url=https://example.com/tabby.jar")
      case v => fail(s"expected StringVal, got $v")
    assertEquals(warnings(src).length, 0)
  }

  test("coursier dep with ,exclude= is a single value") {
    val src =
      """//> using dep com.lihaoyi::os-lib:0.11.3,exclude=com.lihaoyi%%geny
        |""".stripMargin
    val ds = directives(src)
    assertEquals(ds.head.values.length, 1)
    ds.head.values.head match
      case sv: DirectiveValue.StringVal =>
        assertEquals(sv.value, "com.lihaoyi::os-lib:0.11.3,exclude=com.lihaoyi%%geny")
      case v => fail(s"expected StringVal, got $v")
    assertEquals(warnings(src).length, 0)
  }

  test("mixed commas — some as separators, some embedded") {
    val src =
      """//> using dep a,b, c,d
        |""".stripMargin
    val ds = directives(src)
    val vs = ds.head.values.collect { case sv: DirectiveValue.StringVal => sv.value }
    assertEquals(vs, Seq("a,b", "c,d"))
    val ws = warnings(src)
    assertEquals(ws.count(_.message.contains("deprecated")), 1)
  }

  test("quoted values with comma separator between them") {
    val src =
      """//> using javacOpt "source", "1.8"
        |""".stripMargin
    val ds = directives(src)
    assertEquals(ds.head.values.length, 2)
    val vs = ds.head.values.collect { case sv: DirectiveValue.StringVal => sv.value }
    assertEquals(vs, Seq("source", "1.8"))
    assertEquals(warnings(src).count(_.message.contains("deprecated")), 1)
  }

  test("bare value touching quoted string emits error") {
    val src =
      """//> using dep a,"b"
        |""".stripMargin
    val r    = parse(src)
    val errs = r.diagnostics.filter(_.severity == DiagnosticSeverity.Error)
    assert(errs.exists(_.message.contains("Whitespace is required")), s"errors=$errs")
  }
