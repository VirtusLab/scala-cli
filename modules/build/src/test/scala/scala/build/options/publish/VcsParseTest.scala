package scala.build.options.publish

import scala.build.Positioned
import scala.build.errors.{BuildException, MalformedInputError}

class VcsParseTest extends munit.FunSuite {
  test("valid GitHub") {
    val actual = Vcs.parse(Positioned.none("github:VirtusLab/scala-cli"))
    val expected: Either[BuildException, Vcs] = Right(Vcs(
      "https://github.com/VirtusLab/scala-cli.git",
      "scm:git:github.com/VirtusLab/scala-cli.git",
      "scm:git:git@github.com:VirtusLab/scala-cli.git"
    ))

    assertEquals(expected, actual)
  }

  test("invalid GitHub: missing /") {
    val actual: Either[BuildException, Vcs] = Vcs.parse(Positioned.none("github:scala-cli"))
    val expected                            =
      Left(new MalformedInputError("github-vcs", "github:scala-cli", "github:org/project", Nil))
    assert {
      actual match {
        case Left(_: BuildException) => true
        case _                       => sys.error("incorrect type")
      }
    }
    assertEquals(expected.toString, actual.toString)
  }

  test("invalid GitHub: too many /") {
    val actual: Either[BuildException, Vcs] =
      Vcs.parse(Positioned.none("github:github.com/VirtusLab/scala-cli"))
    val expected = Left(new MalformedInputError(
      "github-vcs",
      "github:github.com/VirtusLab/scala-cli",
      "github:org/project",
      Nil
    ))
    assert {
      actual match {
        case Left(_: BuildException) => true
        case _                       => sys.error("incorrect type")
      }
    }
    assertEquals(expected.toString, actual.toString)
  }

  test("valid generic") {
    val actual = Vcs.parse(Positioned.none(
      "https://github.com/VirtusLab/scala-cli.git|scm:git:github.com/VirtusLab/scala-cli.git|scm:git:git@github.com:VirtusLab/scala-cli.git"
    ))
    val expected: Either[BuildException, Vcs] = Right(Vcs(
      "https://github.com/VirtusLab/scala-cli.git",
      "scm:git:github.com/VirtusLab/scala-cli.git",
      "scm:git:git@github.com:VirtusLab/scala-cli.git"
    ))

    assertEquals(expected, actual)
  }

  test("invalid generic: missing |") {
    val actual: Either[BuildException, Vcs] = Vcs.parse(Positioned.none(
      "https://github.com/VirtusLab/scala-cli|scm:git:github.com/VirtusLab/scala-cli.git"
    ))
    val expected = Left(new MalformedInputError(
      "vcs",
      "https://github.com/VirtusLab/scala-cli|scm:git:github.com/VirtusLab/scala-cli.git",
      "url|connection|developer-connection",
      Nil
    ))

    assert {
      actual match {
        case Left(_: BuildException) => true
        case _                       => sys.error("incorrect type")
      }
    }
    assertEquals(expected.toString, actual.toString)
  }

  test("invalid generic: extra |") {
    val actual: Either[BuildException, Vcs] = Vcs.parse(Positioned.none("a|b|c|d"))
    val expected                            =
      Left(new MalformedInputError("vcs", "a|b|c|d", "url|connection|developer-connection", Nil))

    assert {
      actual match {
        case Left(_: BuildException) => true
        case _                       => sys.error("incorrect type")
      }
    }
    assertEquals(expected.toString, actual.toString)
  }

  test("invalid generic: gibberish") {
    val actual: Either[BuildException, Vcs] = Vcs.parse(Positioned.none("sfrgt pagdhn"))
    val expected                            = Left(new MalformedInputError(
      "vcs",
      "sfrgt pagdhn",
      "url|connection|developer-connection",
      Nil
    ))

    assert {
      actual match {
        case Left(_: BuildException) => true
        case _                       => sys.error("incorrect type")
      }
    }
    assertEquals(expected.toString, actual.toString)
  }
}
