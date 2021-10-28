package scala.build.blooprifle

import com.eed3si9n.expecty.Expecty.expect

import scala.build.blooprifle.VersionUtil.jvmRelease

class ParsingTests extends munit.FunSuite {

  implicit class BV(s: String) {
    implicit def v = BloopVersion(s)
  }

  test("bloop vetrsion comparisons test") {
    expect("1.4.9".v isOlderThan "1.4.10".v)
    expect("1.4.9".v isOlderThan "1.4.9-22".v)
    expect("1.4.9-22".v isOlderThan "1.4.10".v)
    expect(!("1.4.9".v isOlderThan "1.4.9".v))
    expect("1.4.10-2".v isOlderThan "1.4.10-4".v)
    expect("1.4.10-2-abc".v isOlderThan "1.4.10-4-def".v)
  }
  test("jvm release parsing test") {
    def j = jvmRelease _
    expect(j("1.8") == Some(8))
    expect(j("1.8.75") == Some(8))
    expect(j("1.8.64_3") == Some(8))
    expect(j("1.8_3") == Some(8))
    expect(j("9") == Some(9))
    expect(j("14") == Some(14))
    expect(j("17") == Some(17))

  }
}
