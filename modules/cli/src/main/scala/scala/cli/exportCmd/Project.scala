package scala.cli.exportCmd

abstract class Project extends Product with Serializable {
  def writeTo(dir: os.Path): Unit
}
