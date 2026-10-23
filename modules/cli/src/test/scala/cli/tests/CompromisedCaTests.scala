package cli.tests

import java.io.IOException
import java.net.URI
import javax.net.ssl.{HttpsURLConnection, SSLException}

import scala.concurrent.duration.*

/** Verifies that the JDK running the tests (i.e. the one used on the CI) doesn't trust the
  * compromised eMudhra intermediate CAs (CVE-2026-91403).
  *
  * See https://www.scala-lang.org/blog/jdk-certificate-vulnerability.html
  */
class CompromisedCaTests extends TestUtil.ScalaCliSuite:
  override def munitTimeout: Duration = 5.minutes

  private val testUrls =
    for
      validation <- List("dv", "ov", "ev")
      aaa        <- List("", "-aaa")
      generation <- List("g3b", "g3c")
    yield s"https://$validation-$generation$aaa-emudhra.we-have-a-problem.com/"

  test("JDK does not trust the compromised CAs"):
    val failures = testUrls.flatMap(verifyTls)
    assert(
      failures.isEmpty,
      s"""Compromised CAs are trusted by the JDK, or a network error was encountered:
         |${failures.mkString(System.lineSeparator())}""".stripMargin
    )

  /** @return
    *   `None` when the TLS handshake got rejected, an error message otherwise
    */
  private def verifyTls(url: String): Option[String] =
    val conn = URI.create(url).toURL.openConnection().asInstanceOf[HttpsURLConnection]
    conn.setConnectTimeout(10000)
    conn.setReadTimeout(15000)
    try
      val status = conn.getResponseCode
      // handshake succeeded -> the certificate chain was accepted
      Some(s"VULNERABLE (HTTP $status) $url")
    catch
      case e: IOException =>
        findSslException(e) match
          case Some(_) => None
          // DNS, timeout, connection refused etc. -> says nothing about TLS
          case None => Some(s"ERROR ($e) $url")
    finally conn.disconnect()

  // the SSL exception may be wrapped, so walk the cause chain
  private def findSslException(t: Throwable): Option[SSLException] =
    Iterator
      .iterate(t)(_.getCause)
      .takeWhile(_ != null)
      .collectFirst { case ssl: SSLException => ssl }
