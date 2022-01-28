package scala.build.preprocessing

import java.nio.charset.StandardCharsets

import scala.build.errors.{BuildException, FileNotFoundException}

object PreprocessingUtil {

  private def defaultCharSet = StandardCharsets.UTF_8

  def maybeRead(f: os.Path): Either[BuildException, String] =
    if (os.isFile(f)) Right(os.read(f, defaultCharSet))
    else Left(new FileNotFoundException(f))
}
