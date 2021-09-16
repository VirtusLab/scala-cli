package scala.cli.commands

import caseapp._

import scala.cli.`export`.Giter8Project

object New extends ScalaCommand[NewOptions] {

  def run(options: NewOptions, args: RemainingArgs): Unit = {

    val output = options.output
    val dest   = output.map(os.Path(_, os.pwd)).getOrElse(os.pwd / "scala")

    if (os.exists(dest)) {
      System.err.println(s"Error: $dest already exists.")
      sys.exit(1)
    }

    Giter8Project.writeTo(dest)
  }

}
