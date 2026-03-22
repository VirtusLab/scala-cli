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

  test("parse a simple directive") {
    val ds = directives("//> using scala 3\n")
    assertEquals(ds.length, 1)
    assertEquals(ds.head.key, "scala")
    assertEquals(ds.head.values.length, 1)
  }

  test("parse key value") {
    val ds = directives("//> using scala 3\n")
    val v  = ds.head.values.head
    v match
      case sv: DirectiveValue.StringVal => assertEquals(sv.value, "3")
      case _                            => fail(s"expected StringVal, got $v")
  }

  test("parse dotted key") {
    val ds = directives("//> using test.dep munit::munit:1.0.0\n")
    assertEquals(ds.head.key, "test.dep")
  }

  test("parse multiple values") {
    val ds     = directives("//> using dep com.lihaoyi::os-lib:0.11.4 com.lihaoyi::upickle:3.1.0\n")
    val values = ds.head.values
    assertEquals(values.length, 2)
  }

  test("parse quoted string value") {
    val ds = directives("""//> using scalacOption "-Xfatal-warnings"""" + "\n")
    val v  = ds.head.values.head
    v match
      case sv: DirectiveValue.StringVal =>
        assertEquals(sv.value, "-Xfatal-warnings")
        assert(sv.isQuoted)
      case _ => fail(s"expected StringVal, got $v")
  }

  test("parse boolean true") {
    val ds = directives("//> using publish.doc true\n")
    val v  = ds.head.values.head
    v match
      case bv: DirectiveValue.BoolVal => assertEquals(bv.value, true)
      case _                          => fail(s"expected BoolVal, got $v")
  }

  test("parse boolean false") {
    val ds = directives("//> using publish.doc false\n")
    val v  = ds.head.values.head
    v match
      case bv: DirectiveValue.BoolVal => assertEquals(bv.value, false)
      case _                          => fail(s"expected BoolVal, got $v")
  }

  test("directive with no values produces EmptyVal") {
    val ds = directives("//> using toolkit\n")
    assertEquals(ds.head.values.length, 1)
    ds.head.values.head match
      case _: DirectiveValue.EmptyVal => ()
      case v                          => fail(s"expected EmptyVal, got $v")
  }

  test("multiple directives") {
    val src = "//> using scala 3\n//> using dep foo\n"
    val ds  = directives(src)
    assertEquals(ds.length, 2)
    assertEquals(ds(0).key, "scala")
    assertEquals(ds(1).key, "dep")
  }

  // -----------------------------------------------------------------------
  // Comma deprecation
  // -----------------------------------------------------------------------

  test("comma separator emits deprecation warning") {
    val ws = warnings("//> using dep a, b\n")
    assert(ws.nonEmpty, s"expected warnings, got none")
    assert(ws.exists(_.message.contains("deprecated")))
  }

  test("each comma as separator emits its own deprecation warning") {
    val ws              = warnings("//> using dep a, b, c\n")
    val deprecationMsgs = ws.filter(_.message.contains("Use of commas as separators"))
    assertEquals(deprecationMsgs.length, 2)
  }

  test("comma separator still parses both values") {
    val ds = directives("//> using dep a, b\n")
    assertEquals(ds.head.values.length, 2)
  }

  test("embedded comma (no space) does NOT emit warning") {
    val ws = warnings("//> using packaging.graalvmArgs --enable-url-protocols=http,https\n")
    assertEquals(ws.length, 0)
  }

  // -----------------------------------------------------------------------
  // Position tracking
  // -----------------------------------------------------------------------

  test("key position line is correct") {
    val ds = directives("//> using scala 3\n")
    assertEquals(ds.head.keyPosition.line, 0)
  }

  test("key position column is correct") {
    // "//> using scala 3"
    //  0123456789...
    // `scala` starts at column 10
    val ds = directives("//> using scala 3\n")
    assertEquals(ds.head.keyPosition.column, 10)
  }

  test("value position column is correct") {
    // "//> using scala 3"
    //                ^16
    val ds   = directives("//> using scala 3\n")
    val vPos = ds.head.values.head.pos
    assertEquals(vPos.column, 16)
  }

  test("key position offset is correct") {
    val ds     = directives("//> using scala 3\n")
    val offset = ds.head.keyPosition.offset
    assertEquals(offset, 10) // first line starts at 0, column 10
  }

  test("value position on second line has correct line number") {
    val src = "//> using scala 3\n//> using dep foo\n"
    val ds  = directives(src)
    assertEquals(ds(1).keyPosition.line, 1)
  }

  test("value position on second line has correct offset") {
    val src       = "//> using scala 3\n//> using dep foo\n"
    val ds        = directives(src)
    val depOffset = ds(1).keyPosition.offset
    // "//> using scala 3\n" is 18 chars, then "//> using " is 10 more = offset 28
    assertEquals(depOffset, 28)
  }

  // -----------------------------------------------------------------------
  // Diagnostics
  // -----------------------------------------------------------------------

  test("directive after code emits warning") {
    val src = "val x = 1\n//> using scala 3\n"
    val ws  = warnings(src)
    assert(ws.nonEmpty)
  }

  test("directive after code is not included in results") {
    val src = "val x = 1\n//> using scala 3\n"
    val ds  = directives(src)
    assertEquals(ds.length, 0)
  }

  // -----------------------------------------------------------------------
  // codeOffset
  // -----------------------------------------------------------------------

  test("codeOffset after single directive") {
    val src = "//> using scala 3\nval x = 1\n"
    val r   = parse(src)
    assertEquals(r.codeOffset, 18)
  }

  test("codeOffset with no directives is 0") {
    val src = "val x = 1\n"
    val r   = parse(src)
    assertEquals(r.codeOffset, 0)
  }

  test("codeOffset at end of file when only directives") {
    val src = "//> using scala 3\n"
    val r   = parse(src)
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
    val ds = directives("//> using dep com.lihaoyi::os-lib:0.11.4\n")
    ds.head.values.head match
      case sv: DirectiveValue.StringVal =>
        assertEquals(sv.value, "com.lihaoyi::os-lib:0.11.4")
      case v => fail(s"expected StringVal, got $v")
  }
