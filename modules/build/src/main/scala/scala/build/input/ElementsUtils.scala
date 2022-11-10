package scala.build.input

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import scala.build.Directories

object ElementsUtils {
  extension (d: Directory) {
    def singleFilesFromDirectory(enableMarkdown: Boolean): Seq[SingleFile] = {
      import Ordering.Implicits.seqOrdering
      os.walk.stream(d.path, skip = _.last.startsWith("."))
        .filter(os.isFile(_))
        .collect {
          case p if p.last.endsWith(".java") =>
            JavaFile(d.path, p.subRelativeTo(d.path))
          case p if p.last == "project.scala" =>
            ProjectScalaFile(d.path, p.subRelativeTo(d.path))
          case p if p.last.endsWith(".scala") =>
            SourceScalaFile(d.path, p.subRelativeTo(d.path))
          case p if p.last.endsWith(".sc") =>
            Script(d.path, p.subRelativeTo(d.path))
          case p if p.last.endsWith(".c") || p.last.endsWith(".h") =>
            CFile(d.path, p.subRelativeTo(d.path))
          case p if p.last.endsWith(".md") && enableMarkdown =>
            MarkdownFile(d.path, p.subRelativeTo(d.path))
        }
        .toVector
        .sortBy(_.subPath.segments)
    }

    def configFile: Seq[ProjectScalaFile] =
      if (os.exists(d.path / "project.scala"))
        Seq(ProjectScalaFile(d.path, os.sub / "project.scala"))
      else Nil
  }

  extension (elements: Seq[Element]) {
    def projectSettingsFiles: Seq[ProjectScalaFile] =
      elements.flatMap {
        case f: ProjectScalaFile => Seq(f)
        case d: Directory        => d.configFile
        case _                   => Nil
      }.distinct

    def inputsHash: String = {
      def bytes(s: String): Array[Byte] = s.getBytes(StandardCharsets.UTF_8)

      val it = elements.iterator.flatMap {
        case elem: OnDisk =>
          val prefix = elem match {
            case _: Directory         => "dir:"
            case _: ResourceDirectory => "resource-dir:"
            case _: JavaFile          => "java:"
            case _: ProjectScalaFile  => "config:"
            case _: SourceScalaFile   => "scala:"
            case _: CFile             => "c:"
            case _: Script            => "sc:"
            case _: MarkdownFile      => "md:"
          }
          Iterator(prefix, elem.path.toString, "\n").map(bytes)
        case v: Virtual =>
          Iterator(bytes("virtual:"), v.content, bytes(v.source), bytes("\n"))
      }
      val md = MessageDigest.getInstance("SHA-1")
      it.foreach(md.update)
      val digest        = md.digest()
      val calculatedSum = new BigInteger(1, digest)
      String.format(s"%040x", calculatedSum).take(10)
    }

    def homeWorkspace(directories: Directories): os.Path = {
      val hash0 = elements.inputsHash
      val dir   = directories.virtualProjectsDir / hash0.take(2) / s"project-${hash0.drop(2)}"
      os.makeDir.all(dir)
      dir
    }
  }
}
