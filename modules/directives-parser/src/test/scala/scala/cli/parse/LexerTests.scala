package scala.cli.parse

import munit.FunSuite

class LexerTests extends FunSuite:

  private def lex(line: String, lineNum: Int = 0): Seq[Token] =
    Lexer.tokenize(line, lineNum, 0)

  private def lexTokens(line: String): Seq[Token] =
    lex(line).dropRight(1) // drop trailing Newline

  test("tokenize `using` keyword") {
    val tokens = lexTokens("//> using dep foo")
    assert(tokens.exists { case _: Token.Using => true; case _ => false })
  }

  test("tokenize bare identifier") {
    val tokens = lexTokens("//> using dep foo")
    val idents = tokens.collect { case Token.Ident(v, _) => v }
    assertEquals(idents, Seq("dep", "foo"))
  }

  test("tokenize dotted key as single Ident") {
    val tokens = lexTokens("//> using test.dep munit")
    val idents = tokens.collect { case Token.Ident(v, _) => v }
    assertEquals(idents, Seq("test.dep", "munit"))
  }

  test("tokenize quoted string") {
    val tokens  = lexTokens("""//> using dep "com.lihaoyi::os-lib:0.11.4"""")
    val strLits = tokens.collect { case Token.StringLit(v, _) => v }
    assertEquals(strLits, Seq("com.lihaoyi::os-lib:0.11.4"))
  }

  test("tokenize boolean true") {
    val tokens = lexTokens("//> using publish.doc true")
    assert(tokens.exists { case Token.BoolLit(true, _) => true; case _ => false })
  }

  test("tokenize boolean false") {
    val tokens = lexTokens("//> using publish.doc false")
    assert(tokens.exists { case Token.BoolLit(false, _) => true; case _ => false })
  }

  test("comma followed by whitespace is a Comma token") {
    val tokens = lexTokens("//> using dep a, b")
    assert(tokens.exists { case _: Token.Comma => true; case _ => false })
    val idents = tokens.collect { case Token.Ident(v, _) => v }
    assertEquals(idents.filter(_ != "dep"), Seq("a", "b"))
  }

  test("comma embedded in value is NOT a separator") {
    val tokens = lexTokens("//> using packaging.graalvmArgs --enable-url-protocols=http,https")
    // the comma inside the value should be consumed as part of the Ident
    val idents = tokens.collect { case Token.Ident(v, _) => v }
    assert(idents.contains("--enable-url-protocols=http,https"), s"idents=$idents")
    assert(!tokens.exists { case _: Token.Comma => true; case _ => false })
  }

  test("unterminated string literal produces LexError") {
    val tokens = lexTokens("""//> using dep "unterminated""")
    assert(tokens.exists { case _: Token.LexError => true; case _ => false }, s"tokens=$tokens")
  }

  test("string escape sequences") {
    val tokens  = lexTokens("""//> using dep "line1\nline2"""")
    val strLits = tokens.collect { case Token.StringLit(v, _) => v }
    assertEquals(strLits, Seq("line1\nline2"))
  }

  test("column position of key") {
    // `//> using dep foo` — `dep` starts at column 10
    val tokens   = lexTokens("//> using dep foo")
    val depToken = tokens.collectFirst { case t @ Token.Ident("dep", _) => t }
    assert(depToken.isDefined, "expected Ident(dep)")
    assertEquals(depToken.get.pos.column, 10)
  }

  test("column position of value") {
    // `//> using dep foo` — `foo` starts at column 14
    val tokens   = lexTokens("//> using dep foo")
    val fooToken = tokens.collectFirst { case t @ Token.Ident("foo", _) => t }
    assert(fooToken.isDefined)
    assertEquals(fooToken.get.pos.column, 14)
  }

  test("line position is preserved") {
    val tokens   = Lexer.tokenize("//> using dep foo", lineNum = 3, lineStartOffset = 100)
    val depToken = tokens.collectFirst { case t @ Token.Ident("dep", _) => t }
    assert(depToken.isDefined)
    assertEquals(depToken.get.pos.line, 3)
  }

  test("trailing Newline token is always emitted") {
    val tokens = lex("//> using scala 3")
    assert(tokens.last match { case _: Token.Newline => true; case _ => false })
  }

  test("empty directive line still produces Newline") {
    val tokens = lex("//> using")
    assert(tokens.last match { case _: Token.Newline => true; case _ => false })
  }

  test("`using` as first ident after `//> ` is a Using token, not Ident") {
    val tokens          = lexTokens("//> using scala 3")
    val firstMeaningful = tokens.head
    assert(
      firstMeaningful match { case _: Token.Using => true; case _ => false },
      s"got $firstMeaningful"
    )
  }

  test("backtick-quoted identifier strips backticks") {
    val tokens = lexTokens("//> using `native-gc`")
    val idents = tokens.collect { case Token.Ident(v, _) => v }
    assertEquals(idents, Seq("native-gc"))
  }

  test("backtick identifier with dash is a valid Ident") {
    val tokens = lexTokens("//> using `native-mode`")
    val idents = tokens.collect { case Token.Ident(v, _) => v }
    assertEquals(idents, Seq("native-mode"))
  }
