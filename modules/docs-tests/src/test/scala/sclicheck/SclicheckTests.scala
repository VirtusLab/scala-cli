package sclicheck

class SclicheckTests extends munit.FunSuite:
  test("Run regex") {
    assert(clue(CompileBlock.unapplySeq("``scala compile")).isEmpty)
    assert(
      clue(CompileBlock.unapplySeq("```scala compile"))
        .contains(Seq("```", "scala", "compile", null, null))
    )
    assert(
      clue(CompileBlock.unapplySeq("```scala compile power"))
        .contains(Seq("```", "scala", "compile", null, "power"))
    )
    assert(
      clue(CompileBlock.unapplySeq("```scala fail"))
        .contains(Seq("```", "scala", "fail", null, null))
    )
    assert(
      clue(CompileBlock.unapplySeq("````markdown compile"))
        .contains(Seq("````", "markdown", "compile", null, null))
    )
    assert(
      clue(CompileBlock.unapplySeq("````markdown fail  title=a.md"))
        .contains(Seq("````", "markdown", "fail", "a.md", null))
    )
    assert(clue(CompileBlock.unapplySeq("``scala fail  title=a.sc")).isEmpty)
    assert(
      clue(CompileBlock.unapplySeq("```scala fail  title=a.sc"))
        .contains(Seq("```", "scala", "fail", "a.sc", null))
    )
  }
