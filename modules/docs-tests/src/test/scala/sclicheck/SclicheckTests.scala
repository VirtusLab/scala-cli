package sclicheck

class SclicheckTests extends munit.FunSuite:
  test("Run regex") {
    assert(None == clue(CompileBlock.unapplySeq("``scala compile")))
    assert(
      Some(Seq("```", "scala", "compile", null)) == clue(
        CompileBlock.unapplySeq("```scala compile")
      )
    )
    assert(
      Some(Seq("```", "scala", "fail", null)) == clue(CompileBlock.unapplySeq("```scala fail"))
    )
    assert(
      Some(Seq("````", "markdown", "compile", null)) == clue(
        CompileBlock.unapplySeq("````markdown compile")
      )
    )
    assert(
      Some(Seq("````", "markdown", "fail", "a.md")) == clue(
        CompileBlock.unapplySeq("````markdown fail  title=a.md")
      )
    )
    assert(
      None == clue(
        CompileBlock.unapplySeq("``scala fail  title=a.sc")
      )
    )
    assert(
      Some(Seq("```", "scala", "fail", "a.sc")) == clue(
        CompileBlock.unapplySeq("```scala fail  title=a.sc")
      )
    )
  }
