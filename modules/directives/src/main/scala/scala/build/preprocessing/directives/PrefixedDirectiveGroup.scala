package scala.build.preprocessing.directives

import scala.build.options.BuildOptions
import scala.build.Positioned
import scala.build.errors.BuildException
import scala.util.Try
import caseapp.core.help.Help

abstract class PrefixedDirectiveGroup[T](prefix: String, capitalizePrefx: String, help: Help[_]) {

  protected def mkBuildOptions(parsed: T): BuildOptions

  def group: DirectiveHandlerGroup[BuildOptions]

  abstract class NamedDirectiveHandler[V, U](
    descriptionOverride: Option[String],
    constrains: DirectiveConstrains[V, U],
    baseKey: Option[String]
  ) extends SimpleUsingDirectiveHandler("", "", Nil, constrains){
    private def primaryName = 
      baseKey.getOrElse(this.getClass.getSimpleName.filterNot('$' == _))

    override val name = s"$capitalizePrefx $primaryName"

    val camelCaseOpt = prefix + primaryName.capitalize
    val hypenOpt = camelCaseOpt.map{ c => if c.isUpper then "-" + c.toLower else c.toString }.mkString
    
    override val keys = Seq(camelCaseOpt, hypenOpt)
    
    override val description = descriptionOverride.getOrElse {
      val arg = help.args.find(a => a.name.name == camelCaseOpt || a.extraNames.exists(_.name == camelCaseOpt))
      println(arg)
      arg.fold("TODO no arg")(_.helpMessage.fold("TODO nod help")(_.message))
    }
      
  }

  class StringSetting(
    parse: (String) => T,
    exampleValues: Seq[String],
    usageValue: String = "value",
    baseKey: Option[String] = None,
    description: Option[String] = None,
  ) extends NamedDirectiveHandler(
        descriptionOverride = description,
        constrains = Single(ValueType.String),
        baseKey
      ) {

    def process(opt: Positioned[String])(using Ctx): Either[BuildException, BuildOptions] =
      Try(mkBuildOptions(parse(opt.value)))
        .toEither.left.flatMap(e => opt.error(e.getMessage))

    def examples: Seq[String] = {
      assert(exampleValues.size == keys.size, "Please provide example for each key!")
      keys.zip(exampleValues).map { case (k, v) => s"//> using $k \"$v\"" }
    }

    def usagesCode: Seq[String] = keys.map(k => s"//> using $k \"$usageValue\"")
  }

  class StringListSetting(
    parse: Seq[String] => T,
    exampleValues: Seq[Seq[String]],
    usageValue: String = "value",
    baseKey: Option[String] = None,
    description: Option[String] = None,
  ) extends NamedDirectiveHandler(
        descriptionOverride = description,
        constrains = AtLeastOne(ValueType.String),
        baseKey
      ) {

    def process(opts: ::[Positioned[String]])(using Ctx): Either[BuildException, BuildOptions] =
      Try(mkBuildOptions(parse(opts.map(_.value))))
        .toEither.left.flatMap(e => opts.head.error(e.getMessage))

    def examples: Seq[String] = {
      assert(exampleValues.size == keys.size, "Please provide example for each key!")
      val str = exampleValues.map(_.map(v => s"\"$v\"").mkString(", "))
      keys.zip(exampleValues).map { case (k, v) => s"//> using $k $str" }
    }

    def usagesCode: Seq[String] = {
      val usages =
        exampleValues.map(_.zipWithIndex.map { case (_, i) => usageValue + i }.mkString(", "))
      keys.zip(usages).map { case (k, u) => s"//> using $k \"$u\"" }
    }
  }

  class BooleanDirective(
    parse: Boolean => T,
    description: Option[String] = None,
    baseKey: Option[String] = None
  ) extends NamedDirectiveHandler(
        descriptionOverride = description,
        constrains = Single(ValueType.Boolean),
        baseKey
      ) {

    def process(opt: Positioned[Boolean])(using Ctx): Either[BuildException, BuildOptions] =
      Try(mkBuildOptions(parse(opt.value)))
        .toEither.left.flatMap(e => opt.error(e.getMessage))

    def examples: Seq[String] =
      Seq(
        s"//> using ${keys(0)}",
        s"//> using ${keys(1)} false",
      )

    def usagesCode: Seq[String] = keys.map(k => s"//> using $k [true|false]")
  }
}
