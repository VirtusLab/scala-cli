package scala.cli.commands.publish

import com.eed3si9n.expecty.Expecty.expect

import java.io.{OutputStream, PrintStream}

import scala.cli.internal.CliLogger
import scala.publish.checksum.logger.{BatchChecksumLogger, InteractiveChecksumLogger}
import scala.publish.signing.logger.{BatchSignerLogger, InteractiveSignerLogger, SignerLogger}
import scala.publish.upload.logger.{BatchUploadLogger, InteractiveUploadLogger}

class PublishLoggersTests extends munit.FunSuite {

  private def logger(verbosity: Int, interactive: Boolean): CliLogger =
    new CliLogger(
      verbosity = verbosity,
      quiet = verbosity < 0,
      progress = Some(interactive),
      out = new PrintStream(OutputStream.nullOutputStream())
    )

  private enum Kind {
    case Interactive, Batch, Silent
  }

  private def kinds(l: CliLogger): Seq[Kind] = Seq(
    PublishUtils.signerLogger(l) match {
      case _: InteractiveSignerLogger => Kind.Interactive
      case _: BatchSignerLogger       => Kind.Batch
      case other                      =>
        expect(other == SignerLogger.nop)
        Kind.Silent
    },
    PublishUtils.checksumLogger(l) match {
      case _: InteractiveChecksumLogger => Kind.Interactive
      case _: BatchChecksumLogger       => Kind.Batch
      case _                            => Kind.Silent
    },
    PublishUtils.uploadLogger(l, dummy = false, isLocal = false) match {
      case _: InteractiveUploadLogger => Kind.Interactive
      case _: BatchUploadLogger       => Kind.Batch
      case _                          => Kind.Silent
    }
  )

  test("interactive loggers when progress can be updated in place") {
    expect(kinds(logger(verbosity = 0, interactive = true)).forall(_ == Kind.Interactive))
  }

  test("batch loggers when output isn't interactive") {
    expect(kinds(logger(verbosity = 0, interactive = false)).forall(_ == Kind.Batch))
  }

  test("no progress output in quiet mode") {
    for (interactive <- Seq(true, false))
      expect(kinds(logger(verbosity = -1, interactive = interactive)).forall(_ == Kind.Silent))
  }

  test("only HTTP(S) repositories are remote") {
    expect(PublishUtils.isHttpRepository("https://repo1.maven.org/maven2"))
    expect(PublishUtils.isHttpRepository("http://localhost:8081/repository"))
    expect(!PublishUtils.isHttpRepository("file:///home/me/.m2/repository"))
  }
}
