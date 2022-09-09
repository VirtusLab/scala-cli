package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class MetaCheck extends ScalaCliSuite {

  override def group: ScalaCliSuite.TestGroup = ScalaCliSuite.TestGroup.First

  /*
   * We don't run tests with --scala 3.… any more, and only rely on those
   * with no --scala option.
   * The test here ensures the default version is indeed Scala 3.
   */
  test("Scala 3 is the default") {
    val testInputs = TestInputs(
      os.rel / "PrintScalaVersion.scala" ->
        """// https://gist.github.com/romanowski/de14691cab7340134e197419bc48919a
          |
          |object PrintScalaVersion extends App {
          |  def props(url: java.net.URL): java.util.Properties = {
          |    val properties = new java.util.Properties()
          |    val is = url.openStream()
          |    try {
          |      properties.load(is)
          |      properties
          |    } finally is.close()
          |  }
          |
          |  def scala2Version: String =
          |    props(getClass.getResource("/library.properties")).getProperty("version.number")
          |
          |  def checkScala3(res: java.util.Enumeration[java.net.URL]): String =
          |    if (!res.hasMoreElements) scala2Version else {
          |      val manifest = props(res.nextElement)
          |      manifest.getProperty("Specification-Title") match {
          |        case "scala3-library-bootstrapped" =>
          |          manifest.getProperty("Implementation-Version")
          |        case _ => checkScala3(res)
          |      }
          |    }
          |  val manifests = getClass.getClassLoader.getResources("META-INF/MANIFEST.MF")
          |
          |  val scalaVersion = checkScala3(manifests)
          |
          |  println(scalaVersion)
          |}
          |""".stripMargin
    )
    testInputs.fromRoot { root =>
      // --ttl 0s so that we are sure we use the latest supported Scala versions listing
      val res          = os.proc(TestUtil.cli, ".", "--ttl", "0s").call(cwd = root)
      val scalaVersion = res.out.trim()
      expect(scalaVersion == Constants.scala3)
    }
  }
}
