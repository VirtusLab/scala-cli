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
    val ds = directives("//> using scala 3\n")
    assertEquals(ds.length, 1)
    assertEquals(ds.head.key, "scala")
    assertEquals(ds.head.values.length, 1)
    val v = ds.head.values.head
    assert(v.isInstanceOf[DirectiveValue.StringVal], s"expected StringVal, got $v")
    assertEquals(v.asInstanceOf[DirectiveValue.StringVal].value, "3")
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

  test("positions are correct for single directive") {
    // "//> using scala 3"
    //  0123456789012345678
    //            ^key  ^value
    val ds = directives("//> using scala 3\n")
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

  // -----------------------------------------------------------------------
  // Issue #2382: directive-like text in comments produces no errors
  // -----------------------------------------------------------------------

  test("issue 2382: directive-like text in all comment types produces no directives or errors") {
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

  // -----------------------------------------------------------------------
  // Issue #3019: directives after package/code emit warnings
  // -----------------------------------------------------------------------

  test("issue 3019: package before directives causes directives to be ignored with warning") {
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
