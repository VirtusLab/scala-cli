package scala.cli.commands.publish

import com.eed3si9n.expecty.Expecty.expect
import coursier.maven.MavenRepository

import java.nio.charset.StandardCharsets
import java.time.Instant

import scala.cli.errors.UploadError
import scala.concurrent.ExecutionContext
import scala.publish.Content
import scala.publish.fileset.{FileSet, Path}
import scala.publish.upload.logger.UploadLogger
import scala.publish.upload.{FileUpload, HttpURLConnectionUpload}

class PublishUploadTests extends munit.FunSuite {

  test("connection and response timeouts are passed as connect and read timeouts") {
    PublishUtils.httpUpload(
      connectionTimeoutSeconds = Some(12),
      responseTimeoutSeconds = Some(34)
    ) match {
      case upload: HttpURLConnectionUpload =>
        expect(upload.connectTimeoutMsOpt == Some(12000))
        expect(upload.readTimeoutMsOpt == Some(34000))
      case other => fail(s"Unexpected upload: $other")
    }
  }

  test("no timeouts by default") {
    PublishUtils.httpUpload(connectionTimeoutSeconds = None, responseTimeoutSeconds = None) match {
      case upload: HttpURLConnectionUpload =>
        expect(upload.connectTimeoutMsOpt.isEmpty)
        expect(upload.readTimeoutMsOpt.isEmpty)
      case other => fail(s"Unexpected upload: $other")
    }
  }

  test("errors writing to a local repository are reported with their cause") {
    val tmpDir = os.temp.dir(prefix = "scala-cli-publish-upload-tests")
    try {
      // a path component of the target is a regular file, so that writing fails on any OS
      val notADirectory = tmpDir / "not-a-directory"
      os.write(notADirectory, "")
      val repoDir = notADirectory / "repo"
      val path    = Path(Seq("org", "example", "demo", "1.0", "demo-1.0.pom"))
      val fileSet = FileSet(
        Seq(path -> Content.InMemory(Instant.EPOCH, "pom".getBytes(StandardCharsets.UTF_8)))
      )
      val errors = FileUpload(repoDir.toNIO)
        .uploadFileSet(
          repository = MavenRepository(repoDir.toNIO.toUri.toASCIIString),
          fileSet = fileSet,
          logger = new UploadLogger {},
          parallel = None
        )
        .unsafeRun()(using ExecutionContext.global)
      errors.toList match {
        case h :: t =>
          val message = new UploadError(::(h, t)).getMessage
          expect(message.contains(path.repr))
          expect(message.contains(notADirectory.toString))
        case Nil => fail("Expected writing to the repository to fail")
      }
    }
    finally os.remove.all(tmpDir)
  }
}
