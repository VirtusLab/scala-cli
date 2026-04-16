package scala.cli.parse

import munit.FunSuite

class LexerTests extends FunSuite:

  private def lex(line: String, lineNum: Int = 0): Seq[Token] =
    Lexer.tokenize(line, lineNum, 0)

  private def lexTokens(line: String): Seq[Token] =
    lex(line).dropRight(1) // drop trailing Newline

  test("`using` is tokenized as Using, not Ident") {
    val tokens = lexTokens("//> using scala 3")
    assert(
      tokens.head match { case _: Token.Using => true; case _ => false },
      s"got ${tokens.head}"
    )
  }

  test("tokenize bare identifiers") {
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

  test("column positions of key and value are correct") {
    // "//> using dep foo"
    //            ^10   ^14
    val tokens = lexTokens("//> using dep foo")
    val depPos = tokens.collectFirst { case Token.Ident("dep", p) => p }.get
    assertEquals(depPos.column, 10)
    val fooPos = tokens.collectFirst { case Token.Ident("foo", p) => p }.get
    assertEquals(fooPos.column, 14)
  }

  test("line position is preserved") {
    val tokens = Lexer.tokenize("//> using dep foo", lineNum = 3, lineStartOffset = 100)
    val depPos = tokens.collectFirst { case Token.Ident("dep", p) => p }.get
    assertEquals(depPos.line, 3)
  }

  test("trailing Newline token is always emitted") {
    val tokens = lex("//> using scala 3")
    assert(tokens.last match { case _: Token.Newline => true; case _ => false })
  }

  test("empty directive line still produces Newline") {
    val tokens = lex("//> using")
    assert(tokens.last match { case _: Token.Newline => true; case _ => false })
  }

  test("backtick-quoted identifier strips backticks") {
    val tokens1 = lexTokens("//> using `native-gc`")
    assertEquals(tokens1.collect { case Token.Ident(v, _) => v }, Seq("native-gc"))

    val tokens2 = lexTokens("//> using `native-mode`")
    assertEquals(tokens2.collect { case Token.Ident(v, _) => v }, Seq("native-mode"))
  }

  test("invalid unicode escape produces LexError instead of crashing") {
    val tokens = lexTokens("//> using dep \"\\uZZZZ\"")
    assert(tokens.exists { case _: Token.LexError => true; case _ => false }, s"tokens=$tokens")
    val err = tokens.collectFirst { case Token.LexError(msg, _) => msg }.get
    assert(err.contains("Invalid unicode escape"), s"error message: $err")
  }

  test("empty backtick identifier produces LexError") {
    val tokens = lexTokens("//> using `` foo")
    assert(tokens.exists { case _: Token.LexError => true; case _ => false }, s"tokens=$tokens")
    val err = tokens.collectFirst { case Token.LexError(msg, _) => msg }.get
    assert(err.contains("Empty backtick identifier"), s"error message: $err")
  }

  test("empty quoted string produces StringLit with empty value") {
    val tokens = lexTokens("//> using dep \"\"")
    val strs   = tokens.collect { case Token.StringLit(v, _) => v }
    assertEquals(strs, Seq(""))
  }

  test("backtick-quoted value is parsed as bare Ident") {
    val tokens = lexTokens("//> using dep `com.lihaoyi::os-lib:0.11.4`")
    val idents = tokens.collect { case Token.Ident(v, _) => v }
    assert(idents.contains("com.lihaoyi::os-lib:0.11.4"), s"idents=$idents")
  }

  test("comma with no spaces produces single token") {
    val tokens = lexTokens("//> using dep a,b")
    val idents = tokens.collect { case Token.Ident(v, _) => v }
    assertEquals(idents.filter(_ != "dep"), Seq("a,b"))
    assert(
      !tokens.exists { case _: Token.Comma => true; case _ => false },
      "should not produce Comma token"
    )
  }

  test("space before comma but not after produces two tokens") {
    val tokens = lexTokens("//> using dep a ,b")
    val idents = tokens.collect { case Token.Ident(v, _) => v }
    assertEquals(idents.filter(_ != "dep"), Seq("a", ",b"))
    assert(
      !tokens.exists { case _: Token.Comma => true; case _ => false },
      "should not produce Comma token"
    )
  }

  test("double comma produces bare token ending with comma then Comma separator") {
    val tokens = lexTokens("//> using dep a,, b")
    val idents = tokens.collect { case Token.Ident(v, _) => v }
    assertEquals(idents.filter(_ != "dep"), Seq("a,", "b"))
    assert(
      tokens.exists { case _: Token.Comma => true; case _ => false },
      "second comma should be a separator"
    )
  }

  test("bare value touching quoted string emits LexError") {
    val tokens = lexTokens("""//> using dep a,"b"""")
    assert(tokens.exists { case _: Token.LexError => true; case _ => false }, s"tokens=$tokens")
    val err = tokens.collectFirst { case Token.LexError(msg, _) => msg }.get
    assert(err.contains("Whitespace is required"), s"error message: $err")
  }
