package scala.cli.internal

import java.nio.charset.Charset
import java.nio.file.{FileAlreadyExistsException, Files, Path}

// initially adapted from https://github.com/coursier/coursier/blob/d9a0fcc1af4876bec7f19a18f2c93d808e06df8d/modules/env/src/main/scala/coursier/env/ProfileUpdater.scala#L44-L137

object ProfileFileUpdater {

  private def startEndIndices(start: String, end: String, content: String): Option[(Int, Int)] = {
    val startIdx = content.indexOf(start)
    if (startIdx >= 0) {
      val endIdx = content.indexOf(end, startIdx + 1)
      if (endIdx >= 0)
        Some(startIdx, endIdx + end.length)
      else
        None
    }
    else
      None
  }

  def addToProfileFile(
    file: Path,
    title: String,
    addition: String,
    charset: Charset
  ): Boolean = {

    def updated(content: String): Option[String] = {
      val start = s"# >>> $title >>>\n"
      val end   = s"# <<< $title <<<\n"
      val withTags = "\n" +
        start +
        addition.stripSuffix("\n") + "\n" +
        end
      if (content.contains(withTags))
        None
      else
        Some {
          startEndIndices(start, end, content) match {
            case None =>
              content + withTags
            case Some((startIdx, endIdx)) =>
              content.take(startIdx) +
                withTags +
                content.drop(endIdx)
          }
        }
    }

    var updatedSomething = false
    val contentOpt = Some(file)
      .filter(Files.exists(_))
      .map(f => new String(Files.readAllBytes(f), charset))
    for (updatedContent <- updated(contentOpt.getOrElse(""))) {
      Option(file.getParent).map(createDirectories(_))
      Files.write(file, updatedContent.getBytes(charset))
      updatedSomething = true
    }
    updatedSomething
  }

  def removeFromProfileFile(
    file: Path,
    title: String,
    charset: Charset
  ): Boolean = {

    def updated(content: String): Option[String] = {
      val start = s"# >>> $title >>>\n"
      val end   = s"# <<< $title <<<\n"
      startEndIndices(start, end, content).map {
        case (startIdx, endIdx) =>
          content.take(startIdx).stripSuffix("\n") +
            content.drop(endIdx)
      }
    }

    var updatedSomething = false
    val contentOpt = Some(file)
      .filter(Files.exists(_))
      .map(f => new String(Files.readAllBytes(f), charset))
    for (updatedContent <- updated(contentOpt.getOrElse(""))) {
      Option(file.getParent).map(createDirectories(_))
      Files.write(file, updatedContent.getBytes(charset))
      updatedSomething = true
    }
    updatedSomething
  }

  private def createDirectories(path: Path): Unit =
    try Files.createDirectories(path)
    catch {
      // Ignored, see https://bugs.openjdk.java.net/browse/JDK-8130464
      case _: FileAlreadyExistsException if Files.isDirectory(path) =>
    }

}
