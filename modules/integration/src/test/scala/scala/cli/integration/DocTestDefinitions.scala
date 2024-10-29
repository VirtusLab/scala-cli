package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect
import org.jsoup.*

import scala.jdk.CollectionConverters.*

abstract class DocTestDefinitions extends ScalaCliSuite with TestScalaVersionArgs {
  _: TestScalaVersion =>
  protected lazy val extraOptions: Seq[String] = scalaVersionArgs ++ TestUtil.extraOptions

  test("generate static scala doc") {
    val dest = os.rel / "doc-static"
    val inputs = TestInputs(
      os.rel / "lib" / "Messages.scala" ->
        """package lib
          |
          |object Messages {
          |  def msg = "Hello"
          |}
          |""".stripMargin,
      os.rel / "simple.sc" ->
        """val msg = lib.Messages.msg
          |println(msg)
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "doc", extraOptions, ".", "-o", dest).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val expectedDestDocPath = root / dest
      expect(os.isDir(expectedDestDocPath))
      val expectedEntries =
        if (actualScalaVersion.startsWith("2."))
          Seq(
            "index.html",
            "lib/Messages$.html",
            "simple$.html"
          )
        else if (
          actualScalaVersion.coursierVersion >= "3.5.0".coursierVersion ||
          (actualScalaVersion.coursierVersion >= "3.3.4".coursierVersion &&
          actualScalaVersion.coursierVersion < "3.4.0".coursierVersion) ||
          actualScalaVersion.startsWith("3.3.4") ||
          actualScalaVersion.startsWith("3.5")
        )
          Seq(
            "index.html",
            "inkuire-db.json",
            "$lessempty$greater$/simple$_.html",
            "lib/Messages$.html"
          )
        else
          Seq(
            "index.html",
            "inkuire-db.json",
            "_empty_/simple$_.html",
            "lib/Messages$.html"
          )
      val entries =
        os.walk(root / dest).filter(!os.isDir(_)).map { path =>
          path.relativeTo(expectedDestDocPath).toString() -> os.read(path)
        }.toMap
      expect(expectedEntries.forall(e => entries.contains(e)))

      val documentableNameElement =
        Jsoup.parse(entries("index.html")).select(".documentableName").asScala
      documentableNameElement.filter(_.text().contains("lib")).foreach { element =>
        expect(!element.attr("href").startsWith("http"))
      }
    }
  }
}
