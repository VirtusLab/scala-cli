package scala.build.interactive

object InteractiveFileOps {

  def erasingPath(
    interactive: Interactive,
    printableDest: String,
    destPath: os.Path
  )(fallbackAction: () => Unit) = {
    val msg = s"""|$printableDest already exists.
                  |Do you want to erase $printableDest?""".stripMargin
    val response = interactive.confirmOperation(msg)
    response match {
      case Some(true) => os.remove.all(destPath)
      case _          => fallbackAction()
    }
  }

  def appendToFile(
    interactive: Interactive,
    msg: String,
    filePath: os.Path,
    entry: String
  )(fallbackAction: () => Unit) = {
    val response = interactive.confirmOperation(msg)
    response match {
      case Some(true) => os.write.append(filePath, entry)
      case _          => fallbackAction()
    }
  }

}
