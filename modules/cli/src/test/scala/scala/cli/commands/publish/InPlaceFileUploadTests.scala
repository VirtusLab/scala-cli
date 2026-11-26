package scala.cli.commands.publish

import com.eed3si9n.expecty.Expecty.expect
import coursier.maven.MavenRepository

import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

import scala.concurrent.ExecutionContext
import scala.publish.Content
import scala.publish.fileset.{FileSet, Path}
import scala.publish.upload.logger.UploadLogger

class InPlaceFileUploadTests extends munit.FunSuite {

  test("existing files are overwritten in place") {
    val tmpDir = os.temp.dir(prefix = "scala-cli-in-place-file-upload-tests")
    try {
      val repoDir = tmpDir / "repo"
      val relPath = os.rel / "org" / "example" / "demo" / "1.0" / "demo-1.0.jar"
      val dest    = repoDir / relPath
      os.write(dest, "old content", createFolders = true)
      def fileKey        = Files.readAttributes(dest.toNIO, classOf[BasicFileAttributes]).fileKey()
      val initialFileKey = fileKey
      // file keys (inode numbers) aren't available on Windows
      assume(initialFileKey != null, "file keys not supported on this file system")

      val source = tmpDir / "demo-1.0.jar"
      os.write(source, "new content")
      val fileSet = FileSet(Seq(Path(relPath.segments) -> Content.File(source.toNIO)))
      val errors  = InPlaceFileUpload(repoDir.toNIO)
        .uploadFileSet(
          repository = MavenRepository(repoDir.toNIO.toUri.toASCIIString),
          fileSet = fileSet,
          logger = new UploadLogger {},
          parallel = None
        )
        .unsafeRun()(using ExecutionContext.global)

      expect(errors.isEmpty)
      expect(os.read(dest) == "new content")
      expect(fileKey == initialFileKey)
    }
    finally os.remove.all(tmpDir)
  }
}
