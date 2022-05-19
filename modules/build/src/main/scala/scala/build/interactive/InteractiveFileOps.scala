package scala.build.interactive

import scala.build.options.BuildOptions

object InteractiveFileOps {

  def erasingPath(
    options: BuildOptions,
    printableDest: String,
    destPath: os.Path,
    fallbackAction: () => Unit
  ) = {
    val msg = s"""|Destination path: $printableDest already exists.
                  |Do you want to erasing $printableDest?""".stripMargin
    val response = Interactive(options).confirmOperation(msg, fallbackAction)
    if (response.isRight)
      os.remove.all(destPath)
  }

  def addEntryToFile(
    options: BuildOptions,
    msg: String,
    filePath: os.Path,
    entry: String,
    fallbackAction: () => Unit
  ) = {
    val response = Interactive(options).confirmOperation(msg, fallbackAction)
    if (response.isRight)
      os.write.append(filePath, entry)
  }

}
