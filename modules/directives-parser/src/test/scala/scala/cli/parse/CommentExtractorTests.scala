package scala.cli.parse

import munit.FunSuite

class CommentExtractorTests extends FunSuite:

  private def extract(src: String): ExtractorResult =
    CommentExtractor.extract(src.toCharArray)

  test("simple directive line") {
    val r = extract("//> using scala 3\n")
    assertEquals(r.directiveLines.length, 1)
    assertEquals(r.directiveLines.head.lineNum, 0)
    assertEquals(r.codeOffset, 18)
  }

  test("shebang is skipped on line 0") {
    val src =
      """#!/usr/bin/env scala
        |//> using scala 3
        |""".stripMargin
    val r = extract(src)
    assertEquals(r.directiveLines.length, 1)
    assertEquals(r.directiveLines.head.lineNum, 1)
  }

  test("shebang after line 0 treated as code") {
    val src =
      """//> using scala 3
        |#!/usr/bin/env scala
        |""".stripMargin
    val r = extract(src)
    assertEquals(r.directiveLines.length, 1)
    assertEquals(r.codeOffset, 18) // shebang line is code
  }

  test("blank lines do not affect directive region") {
    val src =
      """
        |//> using scala 3
        |
        |val x = 1
        |""".stripMargin
    val r = extract(src)
    assertEquals(r.directiveLines.length, 1)
  }

  test("line comments are skipped") {
    val src =
      """// a comment
        |//> using scala 3
        |val x = 1
        |""".stripMargin
    val r = extract(src)
    assertEquals(r.directiveLines.length, 1)
  }

  test("block comment before directive is skipped") {
    val src =
      """/* comment */
        |//> using scala 3
        |""".stripMargin
    val r = extract(src)
    assertEquals(r.directiveLines.length, 1)
  }

  test("multi-line block comment is skipped") {
    val src =
      """/*
        | * block
        | */
        |//> using scala 3
        |""".stripMargin
    val r = extract(src)
    assertEquals(r.directiveLines.length, 1)
  }

  test("codeOffset points to start of first code line") {
    val src =
      """//> using scala 3
        |val x = 1
        |""".stripMargin
    val r = extract(src)
    assertEquals(r.codeOffset, 18) // "//> using scala 3\n" is 18 chars
  }

  test("no code -> codeOffset is file length") {
    val src = "//> using scala 3\n"
    val r   = extract(src)
    assertEquals(r.codeOffset, src.length)
  }

  test("directive after code is ignored with warning") {
    val src =
      """val x = 1
        |//> using scala 3
        |""".stripMargin
    val r = extract(src)
    assertEquals(r.directiveLines.length, 0)
    assert(r.diagnostics.exists(_.severity == DiagnosticSeverity.Warning))
  }

  test("multiple directives") {
    val src =
      """//> using scala 3
        |//> using dep com.lihaoyi::os-lib:0.11.4
        |""".stripMargin
    val r = extract(src)
    assertEquals(r.directiveLines.length, 2)
  }

  test("lineStartOffset is correct") {
    val src =
      """//> using scala 3
        |//> using dep foo
        |""".stripMargin
    val r = extract(src)
    assertEquals(r.directiveLines(0).lineStartOffset, 0)
    assertEquals(r.directiveLines(1).lineStartOffset, 18)
  }

  test("directives inside block comment are ignored") {
    val src =
      """/* //> using scala 3 */
        |val x = 1
        |""".stripMargin
    val r = extract(src)
    assertEquals(r.directiveLines.length, 0)
  }

  test("`//> ` without `using` treated as code") {
    val src =
      """//> notUsing foo
        |val x = 1
        |""".stripMargin
    val r = extract(src)
    assertEquals(r.directiveLines.length, 0)
    assertEquals(r.codeOffset, 0) // first line is code
  }

  test("ScalaDoc containing directive-like text is ignored") {
    val src =
      """/** ScalaDoc '//> using scala 3' */
        |val x = 1
        |""".stripMargin
    val r = extract(src)
    assertEquals(r.directiveLines.length, 0)
    assertEquals(r.diagnostics.length, 0)
  }

  test("multi-line ScalaDoc containing directive-like text is ignored") {
    val src =
      """/**
        | * //> using scala 3
        | */
        |val x = 1
        |""".stripMargin
    val r = extract(src)
    assertEquals(r.directiveLines.length, 0)
    assertEquals(r.diagnostics.length, 0)
  }

  test("line comment containing embedded //> using text is skipped") {
    val src =
      """// line comment '//> using scala 3'
        |val x = 1
        |""".stripMargin
    val r = extract(src)
    assertEquals(r.directiveLines.length, 0)
    assertEquals(r.diagnostics.length, 0)
  }

  test("issue 2382: all three comment types with directive-like text produce no errors") {
    val src =
      """// line comment '//> using ...'
        |/* block comment '//> using ...' */
        |/** ScalaDoc '//> using ...' */
        |""".stripMargin
    val r = extract(src)
    assertEquals(r.directiveLines.length, 0)
    assertEquals(r.diagnostics.length, 0)
  }

  test("block comment with directive-like text followed by real directive") {
    val src =
      """/* //> using dep foo */
        |//> using scala 3
        |""".stripMargin
    val r = extract(src)
    assertEquals(r.directiveLines.length, 1)
    assertEquals(r.directiveLines.head.lineNum, 1)
    assertEquals(r.diagnostics.length, 0)
  }

  test("package statement before directive makes directive post-code") {
    val src =
      """package x
        |//> using scala 3
        |""".stripMargin
    val r = extract(src)
    assertEquals(r.directiveLines.length, 0)
    assertEquals(r.codeOffset, 0)
    assertEquals(r.diagnostics.length, 1)
    assertEquals(r.diagnostics.head.severity, DiagnosticSeverity.Warning)
    assert(r.diagnostics.head.message.contains("Ignoring"))
    assert(r.diagnostics.head.message.contains("//> using scala 3"))
  }

  test("multiple directives after code each produce a warning") {
    val src =
      """val x = 1
        |//> using scala 3
        |//> using dep foo
        |""".stripMargin
    val r = extract(src)
    assertEquals(r.directiveLines.length, 0)
    val ws = r.diagnostics.filter(_.severity == DiagnosticSeverity.Warning)
    assertEquals(ws.length, 2)
    assert(ws(0).message.contains("//> using scala 3"))
    assert(ws(1).message.contains("//> using dep foo"))
  }

  test("post-code directive warning has correct position") {
    val src =
      """val x = 1
        |//> using scala 3
        |""".stripMargin
    val r = extract(src)
    val w = r.diagnostics.head
    assertEquals(w.position.map(_.line), Some(1))
    assertEquals(w.position.map(_.offset), Some(10))
  }

  test("mix of valid directives and post-code directives") {
    val src =
      """//> using scala 3
        |val x = 1
        |//> using dep foo
        |""".stripMargin
    val r = extract(src)
    assertEquals(r.directiveLines.length, 1)
    assertEquals(r.directiveLines.head.lineNum, 0)
    val ws = r.diagnostics.filter(_.severity == DiagnosticSeverity.Warning)
    assertEquals(ws.length, 1)
    assert(ws.head.message.contains("//> using dep foo"))
  }
