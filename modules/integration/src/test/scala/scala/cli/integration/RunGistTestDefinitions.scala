package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

trait RunGistTestDefinitions { this: RunTestDefinitions =>
  def escapedUrls(url: String): String =
    if (Properties.isWin) "\"" + url + "\""
    else url

  for {
    useFileDirective <- Seq(true, false)
    useFileDirectiveMessage = if (useFileDirective) " (//> using file)" else ""
  } {
    def testInputs(url: String) =
      if (useFileDirective) TestInputs(os.rel / "input.scala" -> s"//> using file $url")
      else emptyInputs
    def args(url: String) = if (useFileDirective) Seq(".") else Seq(escapedUrls(url))
    test(s"Script URL$useFileDirectiveMessage") {
      val url =
        "https://gist.github.com/alexarchambault/f972d941bc4a502d70267cfbbc4d6343/raw/b0285fa0305f76856897517b06251970578565af/test.sc"
      val expectedMessage = "Hello from GitHub Gist"
      testInputs(url).fromRoot { root =>
        val output = os.proc(TestUtil.cli, "run", extraOptions, args(url))
          .call(cwd = root)
          .out.trim()
        expect(output == expectedMessage)
      }
    }

    test(s"Scala URL$useFileDirectiveMessage") {
      val url =
        "https://gist.github.com/alexarchambault/f972d941bc4a502d70267cfbbc4d6343/raw/2691c01984c9249936a625a42e29a822a357b0f6/Test.scala"
      val message = "Hello from Scala GitHub Gist"
      testInputs(url).fromRoot { root =>
        val output = os.proc(TestUtil.cli, extraOptions, args(url))
          .call(cwd = root)
          .out.trim()
        expect(output == message)
      }
    }

    test(s"Java URL$useFileDirectiveMessage") {
      val url =
        "https://gist.github.com/alexarchambault/f972d941bc4a502d70267cfbbc4d6343/raw/2691c01984c9249936a625a42e29a822a357b0f6/Test.java"
      val message = "Hello from Java GitHub Gist"
      testInputs(url).fromRoot { root =>
        val output = os.proc(TestUtil.cli, extraOptions, args(url))
          .call(cwd = root)
          .out.trim()
        expect(output == message)
      }
    }

    if (!useFileDirective) // TODO: add support for gists in using file directives
      test(s"Github Gists Script URL$useFileDirectiveMessage") {
        TestUtil.retryOnCi() {
          val url =
            "https://gist.github.com/alexarchambault/7b4ec20c4033690dd750ffd601e540ec"
          val message = "Hello"
          testInputs(url).fromRoot { root =>
            val output = os.proc(TestUtil.cli, extraOptions, args(url))
              .call(cwd = root)
              .out.trim()
            expect(output == message)
          }
        }
      }
  }
}
