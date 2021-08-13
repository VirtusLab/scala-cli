package scala.cli.commands

import caseapp._
import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.google.gson.GsonBuilder

import java.io.File
import java.nio.charset.Charset

import scala.build.{Inputs, Os}
import scala.build.internal.Constants
import scala.collection.JavaConverters._

object SetupIde extends ScalaCommand[SetupIdeOptions] {
  def run(options: SetupIdeOptions, args: RemainingArgs): Unit = {

    val rawArgv = argvOpt.getOrElse {
      System.err.println("setup-ide called in a non-standard way :|")
      sys.exit(1)
    }

    def inputs = options.shared.inputsOrExit(args)

    val argv = {
      val commandIndex = rawArgv.indexOf("setup-ide")
      val withBspCommand =
        if (commandIndex < 0) rawArgv // shouldn't happen
        else rawArgv.take(commandIndex) ++ Array("bsp") ++ rawArgv.drop(commandIndex + 1)

      // Ensure the path to the CLI is absolute
      val progName = rawArgv(0)
      if (progName.contains(File.pathSeparator)) {
        val absoluteProgPath = os.FilePath(progName).resolveFrom(Os.pwd).toString
        absoluteProgPath +: withBspCommand.drop(1)
      }
      else withBspCommand
    }

    val name = options.bspName.map(_.trim).filter(_.nonEmpty).getOrElse("scala-cli")

    val details = new BspConnectionDetails(
      name,
      argv.toList.asJava,
      Constants.version,
      scala.build.blooprifle.internal.Constants.bspVersion,
      List("scala", "java").asJava
    )

    val gson = new GsonBuilder().setPrettyPrinting().create()

    val json = gson.toJson(details)

    val dir = options.bspDirectory
      .filter(_.nonEmpty)
      .map(os.Path(_, Os.pwd))
      .getOrElse(inputs.workspace / ".bsp")

    val dest = dir / s"$name.json"

    val charset = options.charset
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(Charset.forName)
      .getOrElse(Charset.defaultCharset())// Should it be UTF-8?

    os.write.over(dest, json.getBytes(charset), createFolders = true)

    if (options.shared.logging.verbosity >= 0)
      System.err.println(s"Wrote $dest")
  }
}
