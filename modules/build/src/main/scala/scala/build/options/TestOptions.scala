package scala.build.options

final case class TestOptions(
  frameworkOpt: Option[String] = None
) {
  def orElse(other: TestOptions): TestOptions =
    TestOptions(
      frameworkOpt = frameworkOpt.orElse(other.frameworkOpt)
    )
}

object TestOptions {
  implicit val hasHashData: HasHashData[TestOptions] = HasHashData.derive
}
