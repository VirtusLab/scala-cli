package scala.cli.commands.publish

import com.eed3si9n.expecty.Expecty.expect

import java.io.{ByteArrayOutputStream, PrintStream}
import java.nio.charset.StandardCharsets
import java.time.Instant

import scala.cli.internal.CliLogger
import scala.publish.Content
import scala.publish.checksum.ChecksumType
import scala.publish.fileset.{FileSet, Path}
import scala.publish.upload.HttpURLConnectionUpload

class PublishUtilsTests extends munit.FunSuite {

  private def cliLogger(verbosity: Int): CliLogger =
    new CliLogger(
      verbosity = verbosity,
      quiet = false,
      progress = Some(false),
      out = new PrintStream(new ByteArrayOutputStream)
    )

  test("HTTP upload uses the connection timeout for connecting and the response one for reading") {
    PublishUtils.httpUpload(
      connectionTimeoutSeconds = Some(7),
      responseTimeoutSeconds = Some(42)
    ) match {
      case upload: HttpURLConnectionUpload =>
        expect(upload.connectTimeoutMsOpt == Some(7000))
        expect(upload.readTimeoutMsOpt == Some(42000))
      case other => fail(s"Expected a HttpURLConnectionUpload, got $other")
    }
  }

  test("HTTP upload leaves unset timeouts to the defaults") {
    PublishUtils.httpUpload(connectionTimeoutSeconds = None, responseTimeoutSeconds = None) match {
      case upload: HttpURLConnectionUpload =>
        expect(upload.connectTimeoutMsOpt.isEmpty)
        expect(upload.readTimeoutMsOpt.isEmpty)
      case other => fail(s"Expected a HttpURLConnectionUpload, got $other")
    }
  }

  for {
    (root, expectedIsLocal) <- Seq(
      "https://repo.example.com/maven2"                                                    -> false,
      "http://localhost:8081/repository/maven"                                             -> false,
      "file:///home/user/.m2/repository"                                                   -> true,
      "file:///home/user/.ivy2/local"                                                      -> true,
      "file:///C:/Users/user/.m2/repository"                                               -> true,
      "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2" -> false
    )
  }
    test(s"isLocalRepository($root) == $expectedIsLocal") {
      expect(PublishUtils.isLocalRepository(root) == expectedIsLocal)
    }

  private def perFileLines(verbosity: Int): (String, String) = {
    val path    = Path(Seq("org", "example", "demo", "1.0", "demo-1.0.jar"))
    val fileSet = FileSet(Seq(path -> Content.InMemory(Instant.EPOCH, Array.emptyByteArray)))
    val id      = new Object

    val signerOut    = new ByteArrayOutputStream
    val signerLogger = PublishUtils.signerLogger(signerOut, cliLogger(verbosity))
    signerLogger.start()
    signerLogger.signing(id, fileSet)
    signerLogger.signingElement(id, path)
    signerLogger.signedElement(id, path, None)
    signerLogger.signed(id, fileSet)
    signerLogger.stop(keep = true)

    val checksumOut    = new ByteArrayOutputStream
    val checksumLogger = PublishUtils.checksumLogger(checksumOut, cliLogger(verbosity))
    checksumLogger.start()
    checksumLogger.computingSet(id, fileSet)
    checksumLogger.computing(id, ChecksumType.MD5, path.repr)
    checksumLogger.computed(id, ChecksumType.MD5, path.repr, None)
    checksumLogger.computedSet(id, fileSet)
    checksumLogger.stop(keep = true)

    (
      new String(signerOut.toByteArray, StandardCharsets.UTF_8),
      new String(checksumOut.toByteArray, StandardCharsets.UTF_8)
    )
  }

  test("signer and checksum loggers print per-file lines at verbosity >= 2") {
    val (signerOutput, checksumOutput) = perFileLines(verbosity = 2)
    expect(signerOutput.contains("Signing org/example/demo/1.0/demo-1.0.jar"))
    expect(checksumOutput.contains(
      s"Computing ${ChecksumType.MD5.name} checksum of org/example/demo/1.0/demo-1.0.jar"
    ))
  }

  test("signer and checksum loggers don't print per-file lines at default verbosity") {
    val (signerOutput, checksumOutput) = perFileLines(verbosity = 0)
    expect(!signerOutput.contains("Signing "))
    expect(!checksumOutput.contains("Computing "))
  }
}
