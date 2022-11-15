package scala.build.input

import scala.build.preprocessing.ScopePath
import scala.util.matching.Regex

sealed abstract class Element extends Product with Serializable

sealed trait SingleElement extends Element

sealed trait AnyScript extends Element

sealed abstract class OnDisk extends Element {
  def path: os.Path
}

sealed abstract class Virtual extends SingleElement {
  def content: Array[Byte]

  def source: String

  def subPath: os.SubPath = {
    val idx = source.lastIndexOf('/')
    os.sub / source.drop(idx + 1)
  }

  def scopePath: ScopePath =
    ScopePath(Left(source), subPath)
}

object Virtual {
  def apply(path: String, content: Array[Byte]): Virtual = {
    val wrapperPath = os.sub / path.split("/").last
    if path.endsWith(".scala") then VirtualScalaFile(content, path)
    else if path.endsWith(".java") then VirtualJavaFile(content, path)
    else if path.endsWith(".sc") then VirtualScript(content, path, wrapperPath)
    else if path.endsWith(".md") then VirtualMarkdownFile(content, path, wrapperPath)
    else VirtualData(content, path)
  }
}

sealed abstract class VirtualSourceFile extends Virtual {
  def isStdin: Boolean = source.startsWith("<stdin>")

  def isSnippet: Boolean = source.startsWith("<snippet>")

  protected def generatedSourceFileName(fileSuffix: String): String =
    if (isStdin) s"stdin$fileSuffix"
    else if (isSnippet) s"${source.stripPrefix("<snippet>-")}$fileSuffix"
    else s"virtual$fileSuffix"
}

sealed trait SingleFile extends OnDisk with SingleElement

sealed trait SourceFile extends SingleFile {
  def subPath: os.SubPath
}

sealed trait Compiled extends Element

sealed trait AnyScalaFile    extends Compiled
sealed trait AnyJavaFile     extends Compiled
sealed trait AnyMarkdownFile extends Compiled

sealed trait ScalaFile extends AnyScalaFile {
  def base: os.Path

  def subPath: os.SubPath

  def path: os.Path = base / subPath
}

final case class Script(base: os.Path, subPath: os.SubPath)
    extends OnDisk with SourceFile with AnyScalaFile with AnyScript {
  lazy val path: os.Path = base / subPath
}

final case class SourceScalaFile(base: os.Path, subPath: os.SubPath)
    extends OnDisk with SourceFile with ScalaFile

final case class ProjectScalaFile(base: os.Path, subPath: os.SubPath)
    extends OnDisk with SourceFile with ScalaFile

final case class JavaFile(base: os.Path, subPath: os.SubPath)
    extends OnDisk with SourceFile with AnyJavaFile {
  lazy val path: os.Path = base / subPath
}

final case class CFile(base: os.Path, subPath: os.SubPath)
    extends OnDisk with SourceFile with Compiled {
  lazy val path: os.Path = base / subPath
}

final case class MarkdownFile(base: os.Path, subPath: os.SubPath)
    extends OnDisk with SourceFile with AnyMarkdownFile {
  lazy val path: os.Path = base / subPath
}

final case class Directory(path: os.Path) extends OnDisk with Compiled

final case class ResourceDirectory(path: os.Path) extends OnDisk

final case class VirtualScript(content: Array[Byte], source: String, wrapperPath: os.SubPath)
    extends VirtualSourceFile with AnyScalaFile with AnyScript

object VirtualScript {
  val VirtualScriptNameRegex: Regex = "(^stdin$|^snippet\\d*$)".r
}

final case class VirtualScalaFile(content: Array[Byte], source: String)
    extends VirtualSourceFile with AnyScalaFile {
  def generatedSourceFileName: String = generatedSourceFileName(".scala")
}

final case class VirtualJavaFile(content: Array[Byte], source: String)
    extends VirtualSourceFile with AnyJavaFile {
  def generatedSourceFileName: String = generatedSourceFileName(".java")
}

final case class VirtualMarkdownFile(
  content: Array[Byte],
  override val source: String,
  wrapperPath: os.SubPath
) extends VirtualSourceFile with AnyMarkdownFile

final case class VirtualData(content: Array[Byte], source: String)
    extends Virtual
