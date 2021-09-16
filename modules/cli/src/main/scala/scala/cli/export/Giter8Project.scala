package scala.cli.`export`

import os.Path
import giter8.Giter8

case object Giter8Project extends Project {
  override def writeTo(dir: Path): Unit = {
    Giter8.main(Array("VirtuslabRnD/scala-cli.g8", "-o", dir.toString()))
  }
}
