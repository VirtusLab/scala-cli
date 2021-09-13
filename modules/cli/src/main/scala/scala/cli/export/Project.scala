package scala.cli.export

abstract class Project extends Product with Serializable {
  def writeTo(dir: os.Path): Unit
}
