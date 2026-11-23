package cli.tests

import com.eed3si9n.expecty.Expecty.expect

import scala.build.internal.CoursierUserAgent
import scala.build.tests.TestLogger
import scala.cli.commands.shared.CoursierOptions

class CoursierUserAgentTests extends TestUtil.ScalaCliSuite {
  test("coursier cache from the CLI options sends the Scala CLI user agent") {
    val logger  = TestLogger()
    val options = Seq(
      CoursierOptions(),
      CoursierOptions(offline = Some(true)),
      CoursierOptions(ttl = Some("1h"), cache = Some(os.temp.dir().toString))
    )
    for opts <- options do
      expect(opts.coursierCache(logger).userAgent.contains(CoursierUserAgent.value))
  }
}
