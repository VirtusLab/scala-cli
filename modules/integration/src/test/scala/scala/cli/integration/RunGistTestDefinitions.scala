package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

trait RunGistTestDefinitions { _: RunTestDefinitions =>
  def escapedUrls(url: String): String =
    if (Properties.isWin) "\"" + url + "\""
    else url

  protected val scalaScriptUrl =
    "https://gist.github.com/alexarchambault/f972d941bc4a502d70267cfbbc4d6343/raw/b0285fa0305f76856897517b06251970578565af/test.sc"
  protected val scalaScriptMessage = "Hello from GitHub Gist"

  test("Script URL") {
    emptyInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, escapedUrls(scalaScriptUrl))
        .call(cwd = root)
        .out.trim()
      expect(output == scalaScriptMessage)
    }
  }

  test("Scala URL") {
    val url =
      "https://gist.github.com/alexarchambault/f972d941bc4a502d70267cfbbc4d6343/raw/2691c01984c9249936a625a42e29a822a357b0f6/Test.scala"
    val message = "Hello from Scala GitHub Gist"
    emptyInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, escapedUrls(url))
        .call(cwd = root)
        .out.trim()
      expect(output == message)
    }
  }

  test("Java URL") {
    val url =
      "https://gist.github.com/alexarchambault/f972d941bc4a502d70267cfbbc4d6343/raw/2691c01984c9249936a625a42e29a822a357b0f6/Test.java"
    val message = "Hello from Java GitHub Gist"
    emptyInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, escapedUrls(url))
        .call(cwd = root)
        .out.trim()
      expect(output == message)
    }
  }

  test("Github Gists Script URL") {
    TestUtil.retryOnCi() {
      val url =
        "https://gist.github.com/alexarchambault/7b4ec20c4033690dd750ffd601e540ec"
      val message = "Hello"
      emptyInputs.fromRoot { root =>
        val output = os.proc(TestUtil.cli, extraOptions, escapedUrls(url))
          .call(cwd = root)
          .out.trim()
        expect(output == message)
      }
    }
  }
}
