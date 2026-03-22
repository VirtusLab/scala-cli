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
    val src = "#!/usr/bin/env scala\n//> using scala 3\n"
    val r   = extract(src)
    assertEquals(r.directiveLines.length, 1)
    assertEquals(r.directiveLines.head.lineNum, 1)
  }

  test("shebang after line 0 treated as code") {
    val src = "//> using scala 3\n#!/usr/bin/env scala\n"
    val r   = extract(src)
    assertEquals(r.directiveLines.length, 1)
    assertEquals(r.codeOffset, 18) // shebang line is code
  }

  test("blank lines do not affect directive region") {
    val src = "\n//> using scala 3\n\nval x = 1\n"
    val r   = extract(src)
    assertEquals(r.directiveLines.length, 1)
  }

  test("line comments are skipped") {
    val src = "// a comment\n//> using scala 3\nval x = 1\n"
    val r   = extract(src)
    assertEquals(r.directiveLines.length, 1)
  }

  test("block comment before directive is skipped") {
    val src = "/* comment */\n//> using scala 3\n"
    val r   = extract(src)
    assertEquals(r.directiveLines.length, 1)
  }

  test("multi-line block comment is skipped") {
    val src = "/*\n * block\n */\n//> using scala 3\n"
    val r   = extract(src)
    assertEquals(r.directiveLines.length, 1)
  }

  test("codeOffset points to start of first code line") {
    val src = "//> using scala 3\nval x = 1\n"
    val r   = extract(src)
    assertEquals(r.codeOffset, 18) // "//> using scala 3\n" is 18 chars
  }

  test("no code -> codeOffset is file length") {
    val src = "//> using scala 3\n"
    val r   = extract(src)
    assertEquals(r.codeOffset, src.length)
  }

  test("directive after code emits warning") {
    val src = "val x = 1\n//> using scala 3\n"
    val r   = extract(src)
    assertEquals(r.directiveLines.length, 0)
    assert(r.diagnostics.exists(_.severity == DiagnosticSeverity.Warning))
  }

  test("directive after code is NOT parsed") {
    val src = "val x = 1\n//> using scala 3\n"
    val r   = extract(src)
    assertEquals(r.directiveLines.length, 0)
  }

  test("multiple directives") {
    val src = "//> using scala 3\n//> using dep com.lihaoyi::os-lib:0.11.4\n"
    val r   = extract(src)
    assertEquals(r.directiveLines.length, 2)
  }

  test("lineStartOffset is correct") {
    val src = "//> using scala 3\n//> using dep foo\n"
    val r   = extract(src)
    assertEquals(r.directiveLines(0).lineStartOffset, 0)
    assertEquals(r.directiveLines(1).lineStartOffset, 18)
  }

  test("directives inside block comment are ignored") {
    val src = "/* //> using scala 3 */\nval x = 1\n"
    val r   = extract(src)
    assertEquals(r.directiveLines.length, 0)
  }

  test("`//> ` without `using` treated as code") {
    val src = "//> notUsing foo\nval x = 1\n"
    val r   = extract(src)
    assertEquals(r.directiveLines.length, 0)
    assertEquals(r.codeOffset, 0) // first line is code
  }
