package scala.cli.integration

class CompileTestsDefault extends CompileTestDefinitions(scalaVersionOpt = None) {
  override protected def compileFilesExtensions: Seq[String] = Seq("$.class", ".class", ".tasty")
}
