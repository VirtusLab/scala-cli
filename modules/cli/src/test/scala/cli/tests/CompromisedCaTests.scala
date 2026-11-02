package cli.tests

import scala.concurrent.duration.*
import scala.util.Properties

/** Verifies that the JDK set up on the host (i.e. the one used on the CI) doesn't trust the
  * compromised eMudhra intermediate CAs (CVE-2026-91403).
  *
  * Tests are forked on their own JVM, so the check is run in a subprocess on the host JDK, which
  * makes it use that JDK's trust store and certificate validation.
  *
  * See https://www.scala-lang.org/blog/jdk-certificate-vulnerability.html
  */
class CompromisedCaTests extends TestUtil.ScalaCliSuite:
  override def munitTimeout: Duration = 5.minutes

  private lazy val hostJavaHome: os.Path =
    val home = Option(System.getenv("SCALA_CLI_TESTS_HOST_JAVA_HOME"))
      .getOrElse(fail("SCALA_CLI_TESTS_HOST_JAVA_HOME is not set"))
    os.Path(home, os.pwd)

  // a host with a legitimate certificate chain, which the JDK must trust
  private val controlUrl = "https://repo1.maven.org/maven2/"

  private val testUrls =
    for
      validation <- List("dv", "ov", "ev")
      aaa        <- List("", "-aaa")
      generation <- List("g3b", "g3c")
    yield s"https://$validation-$generation$aaa-emudhra.we-have-a-problem.com/"

  test("JDK does not trust the compromised CAs"):
    val results                = checkTls(controlUrl :: testUrls)
    def resultFor(url: String) = results.getOrElse(url, TlsResult.Error("no output from the check"))
    val javaInfo               = s"(JDK: $hostJavaHome)"
    // if TLS doesn't work at all (e.g. no network, an intercepting proxy, a broken trust store),
    // the compromised hosts would get rejected too, which proves nothing
    resultFor(controlUrl) match
      case TlsResult.Trusted(_) =>
      case other => fail(s"TLS handshake with the control host failed $javaInfo: $other")
    val failures = testUrls.flatMap: url =>
      resultFor(url) match
        case TlsResult.Untrusted       => None
        case TlsResult.Trusted(status) => Some(s"VULNERABLE (HTTP $status) $url")
        // DNS, timeouts, expired certificates, protocol errors etc. say nothing about the CAs
        case TlsResult.Error(e) => Some(s"ERROR ($e) $url")
    assert(
      failures.isEmpty,
      s"""Compromised CAs are trusted by the JDK, or an unexpected error was encountered $javaInfo:
         |${failures.mkString(System.lineSeparator())}""".stripMargin
    )

  private enum TlsResult:
    // the TLS handshake succeeded, so the certificate chain was accepted
    case Trusted(status: String)
    // "PKIX path building failed": no trust anchor for the chain
    case Untrusted
    case Error(message: String)

  /** Runs the TLS handshakes on the host JDK, with a single-file Java program (no compilation step
    * or class path needed).
    */
  private def checkTls(urls: List[String]): Map[String, TlsResult] =
    val dir    = os.temp.dir(prefix = "scala-cli-tests-compromised-ca-")
    val source = dir / "CheckTls.java"
    os.write(source, checkTlsJavaSource)
    val javaCmd = hostJavaHome / "bin" / (if Properties.isWin then "java.exe" else "java")
    val res     = os.proc(javaCmd, source, urls).call(cwd = dir, check = false, stderr = os.Pipe)
    assert(
      res.exitCode == 0,
      s"""Running the TLS check on $javaCmd failed:
         |${res.err.text()}""".stripMargin
    )
    val results = res.out.lines().map(_.split('\t')).collect:
      case Array("TRUSTED", url, status) => url -> TlsResult.Trusted(status)
      case Array("UNTRUSTED", url)       => url -> TlsResult.Untrusted
      case Array("ERROR", url, message)  => url -> TlsResult.Error(message)
    results.toMap

  private val checkTlsJavaSource =
    """import java.io.IOException;
      |import java.net.URI;
      |import java.security.cert.CertPathBuilderException;
      |import javax.net.ssl.HttpsURLConnection;
      |
      |public class CheckTls {
      |  public static void main(String[] args) {
      |    for (String url : args) System.out.println(check(url));
      |  }
      |
      |  static String check(String url) {
      |    HttpsURLConnection conn = null;
      |    try {
      |      conn = (HttpsURLConnection) URI.create(url).toURL().openConnection();
      |      conn.setConnectTimeout(10000);
      |      conn.setReadTimeout(15000);
      |      return "TRUSTED\t" + url + "\t" + conn.getResponseCode();
      |    } catch (IOException e) {
      |      // the certificate path exception is wrapped, so walk the causes
      |      for (Throwable t = e; t != null; t = t.getCause())
      |        if (t instanceof CertPathBuilderException) return "UNTRUSTED\t" + url;
      |      return "ERROR\t" + url + "\t" + String.valueOf(e).replace('\t', ' ').replace('\n', ' ');
      |    } finally {
      |      if (conn != null) conn.disconnect();
      |    }
      |  }
      |}
      |""".stripMargin
