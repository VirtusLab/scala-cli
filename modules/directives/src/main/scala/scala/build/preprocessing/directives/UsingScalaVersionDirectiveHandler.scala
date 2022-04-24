package scala.build.preprocessing.directives
import scala.build.options.{BuildOptions, ScalaOptions}
import scala.build.Positioned

case object UsingScalaVersionDirectiveHandler extends SimpleUsingDirectiveHandler(
  name = "Scala version",
  description = "Set the default Scala version",
  keys = Seq("scala"),
  constrains = AtLeastOne(ValueType.String, ValueType.Number)
){
  
  def process(versions: ::[Positioned[String]])(using Ctx) = {
    val base :: extra = versions.map(_.value)
    // TODO add version validation here!
    Right(
      BuildOptions(
        scalaOptions = ScalaOptions(
          scalaVersion = Some(base),
          extraScalaVersions = extra.toSet
        )
      ) 
    )
  }

  def usagesCode: Seq[String] = Seq(
    "//> using scala <version>",
    "//> using scala <base-version>, <cross-version>",
    "//> using scala <base-version>, <cross-version1>, <cross-version2>"
  )

  override def examples = Seq(
    "//> using scala \"3.0.2\"",
    "//> using scala 3",
    "//> using scala 3.1",
    "//> using scala \"2.13\"",
    "//> using scala \"2\"",
    "//> using scala \"2.13.6\", \"2.12.15\""
  ) 
}
