package scala.cli.commands.publish

import com.eed3si9n.expecty.Expecty.expect

import scala.publish.upload.HttpURLConnectionUpload

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
}
