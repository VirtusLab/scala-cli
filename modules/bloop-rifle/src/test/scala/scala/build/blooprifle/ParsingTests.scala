package scala.build.blooprifle

import com.eed3si9n.expecty.Expecty.expect

import scala.build.blooprifle.VersionUtil.{jvmRelease, parseBloopAbout}

class ParsingTests extends munit.FunSuite {

  implicit class BV(s: String) {
    implicit def v = BloopVersion(s)
    implicit def p = parseBloopAbout(_)
  }

  test("bloop version comparisons test") {
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

  val jreBloopOutput =
    """|bloop v1.4.11
       |
       |Using Scala v2.12.8 and Zinc v1.3.0-M4+47-d881fa2f
       |Running on Java JRE v11.0.13 (/usr/local/openjdk-11)
       |  -> Doesn't support debugging user code, runtime doesn't implement Java Debug Interface (JDI).
       |Maintained by the Scala Center and the community.
       |""".stripMargin

  val jdkBloopOutput =
    """|bloop v1.4.11
       |
       |Using Scala v2.12.8 and Zinc v1.3.0-M4+47-d881fa2f
       |Running on Java JDK v16.0.2 (/usr/lib/jvm/java-16-openjdk-amd64)
       |  -> Supports debugging user code, Java Debug Interface (JDI) is available.
       |Maintained by the Scala Center and the community.""".stripMargin

  test("parse jre bloop about") {
    expect(jreBloopOutput.p == Some(BloopServerRuntimeInfo(
      BloopVersion("1.4.11"),
      11,
      "/usr/local/openjdk-11"
    )))
  }

  test("parse jdk bloop about") {
    expect(jdkBloopOutput.p == Some(BloopServerRuntimeInfo(
      BloopVersion("1.4.11"),
      16,
      "/usr/lib/jvm/java-16-openjdk-amd64"
    )))
  }

}
