//> using lib "org.scalameta::munit::0.7.29"

class SclicheckTest extends munit.FunSuite:
  test("Run regex"){
    assert(
      Some(Seq("scala", "compile", null)) == clue(CompileBlock.unapplySeq("```scala compile"))
    )
    assert(
      Some(Seq("scala", "fail", null)) == clue(CompileBlock.unapplySeq("```scala fail"))
    )
    assert(
      Some(Seq("scala", "fail", "a.sc")) == clue(CompileBlock.unapplySeq("```scala fail  title=a.sc"))
    )
  }