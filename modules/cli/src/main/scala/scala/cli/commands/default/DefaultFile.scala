package scala.cli.commands.default

import caseapp.core.RemainingArgs

import java.io.File

import scala.build.Logger
import scala.cli.commands.ScalaCommand
import scala.cli.commands.util.CommonOps.*
import scala.cli.internal.Constants
import scala.util.Using

object DefaultFile extends ScalaCommand[DefaultFileOptions] {

  override def hidden       = true
  override def isRestricted = true

  private def readDefaultFile(path: String): Array[Byte] = {
    val resourcePath = Constants.defaultFilesResourcePath + "/" + path
    val cl           = Thread.currentThread().getContextClassLoader
    val resUrl       = cl.getResource(resourcePath)
    if (resUrl == null)
      sys.error(s"Should not happen - resource $resourcePath not found")
    Using.resource(resUrl.openStream())(_.readAllBytes())
  }

  final case class DefaultFile(
    path: os.SubPath,
    content: () => Array[Byte]
  ) {
    def printablePath: String = path.segments.mkString(File.separator)
  }

  def defaultWorkflow: Array[Byte] =
    readDefaultFile("workflows/default.yml")
  def defaultGitignore: Array[Byte] =
    readDefaultFile("gitignore")

  val defaultFiles = Map(
    "workflow"  -> DefaultFile(os.sub / ".github" / "workflows" / "ci.yml", () => defaultWorkflow),
    "gitignore" -> DefaultFile(os.sub / ".gitignore", () => defaultGitignore)
  )
  val defaultFilesByRelPath: Map[String, DefaultFile] = defaultFiles.flatMap {
    case (_, d) =>
      // d.path.toString and d.printablePath differ on Windows (one uses '/', the other '\')
      Seq(
        d.path.toString -> d,
        d.printablePath -> d
      )
  }

  private def unrecognizedFile(name: String, logger: Logger): Nothing = {
    logger.error(
      s"Error: unrecognized default file $name (available: ${defaultFiles.keys.toVector.sorted.mkString(", ")})"
    )
    sys.exit(1)
  }

  override def runCommand(
    options: DefaultFileOptions,
    args: RemainingArgs,
    logger: Logger
  ): Unit = {
    lazy val allArgs = {
      val l = args.all
      if (l.isEmpty) {
        logger.error("No default file asked")
        sys.exit(1)
      }
      l
    }

    if (options.list || options.listIds)
      for ((name, d) <- defaultFiles.toVector.sortBy(_._1)) {
        if (options.listIds)
          println(name)
        if (options.list)
          println(d.printablePath)
      }
    else if (options.write)
      for (arg <- allArgs)
        defaultFiles.get(arg).orElse(defaultFilesByRelPath.get(arg)) match {
          case Some(f) =>
            val dest = os.pwd / f.path
            if (!options.force && os.exists(dest)) {
              logger.error(
                s"Error: ${f.path} already exists. Pass --force to force erasing it."
              )
              sys.exit(1)
            }
            if (options.force)
              os.write.over(dest, f.content(), createFolders = true)
            else
              os.write(dest, f.content(), createFolders = true)
            logger.message(s"Wrote ${f.path}")
          case None =>
            unrecognizedFile(arg, logger)
        }
    else {
      if (allArgs.length > 1) {
        logger.error(s"Error: expected only one argument, got ${allArgs.length}")
        sys.exit(1)
      }

      val arg = allArgs.head
      val f = defaultFiles.get(arg).orElse(defaultFilesByRelPath.get(arg)).getOrElse {
        unrecognizedFile(arg, logger)
      }
      System.out.write(f.content())
    }
  }
}
