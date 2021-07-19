package scala.build.options

final case class TestOptions(
  frameworkOpt: Option[String] = None
) {
  def orElse(other: TestOptions): TestOptions =
    TestOptions(
      frameworkOpt = frameworkOpt.orElse(other.frameworkOpt)
    )

  def addHashData(update: String => Unit): Unit = {
    for (fw <- frameworkOpt)
      update("framework+=" + fw + "\n")
  }
}
