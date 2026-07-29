package scala.cli.commands.publish

import coursier.publish.Content
import coursier.publish.checksum.ChecksumType
import coursier.publish.checksum.logger.ChecksumLogger
import coursier.publish.fileset.{FileSet, Path}

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

class PublishChecksumTests extends munit.FunSuite {

  test("checksums are not generated for .asc signature files") {
    val now     = Instant.parse("2024-01-02T03:04:05Z")
    val jarPath = Path(Seq("org", "example", "demo", "1.0", "demo-1.0.jar"))
    val pomPath = Path(Seq("org", "example", "demo", "1.0", "demo-1.0.pom"))
    val fileSet = FileSet(
      Seq(
        (jarPath, Content.InMemory(now, "jar-bytes".getBytes(StandardCharsets.UTF_8))),
        (
          jarPath.mapLast(_ + ".asc"),
          Content.InMemory(now, "jar-sig".getBytes(StandardCharsets.UTF_8))
        ),
        (pomPath, Content.InMemory(now, "pom-bytes".getBytes(StandardCharsets.UTF_8))),
        (
          pomPath.mapLast(_ + ".asc"),
          Content.InMemory(now, "pom-sig".getBytes(StandardCharsets.UTF_8))
        )
      )
    )

    val pool = Executors.newFixedThreadPool(1)
    try {
      val checksums = PublishUtils.computeChecksums(
        fileSet = fileSet,
        types = Seq(ChecksumType.MD5, ChecksumType.SHA1),
        now = now,
        pool = ExecutionContext.fromExecutorService(pool),
        logger = new ChecksumLogger {}
      )

      val names = checksums.elements.map(_._1.elements.last).toSet

      assert(names.contains("demo-1.0.jar.md5"))
      assert(names.contains("demo-1.0.jar.sha1"))
      assert(names.contains("demo-1.0.pom.md5"))
      assert(names.contains("demo-1.0.pom.sha1"))
      assert(
        names.forall(!_.contains(".asc.")),
        s"checksums must not be generated for .asc files, got: $names"
      )
    }
    finally
      pool.shutdown()
  }
}
