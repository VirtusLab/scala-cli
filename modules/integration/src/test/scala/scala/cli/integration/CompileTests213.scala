package scala.cli.integration

// format: off
class CompileTests213 extends CompileTestDefinitions(
  scalaVersionOpt = Some(Constants.scala213)
) {
// format: on
  override protected def compileFilesExtensions: Seq[String] = Seq("$.class", ".class")
}
