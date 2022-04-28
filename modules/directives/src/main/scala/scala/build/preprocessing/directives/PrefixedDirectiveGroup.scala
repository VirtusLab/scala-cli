package scala.build.preprocessing.directives

import scala.build.options.BuildOptions
import scala.build.Positioned
import scala.build.errors.BuildException
import scala.util.Try
import caseapp.core.help.Help

abstract class PrefixedDirectiveGroup[T](prefix: String, capitalizePrefx: String, help: Help[_]*) {

  protected def mkBuildOptions(parsed: T): BuildOptions

  def group: DirectiveHandlerGroup[BuildOptions]

  trait NamedDirectiveHandler[V] extends BuildOptionsUsingDirectiveHandler[V] {

    def usageValue: String

    private def primaryName = this.getClass.getSimpleName.filterNot('$' == _)

    override def name = s"$capitalizePrefx $primaryName"

    def camelCaseOpt = prefix + primaryName.capitalize
    def hypenOpt = camelCaseOpt.map{ c => if c.isUpper then "-" + c.toLower else c.toString }.mkString

    def cmdOptionName: String = camelCaseOpt
    
    override def keys = Seq(camelCaseOpt, hypenOpt)
    
    override val description = fromCommand(cmdOptionName, help*)

    def defaultPosition(v: V): Positioned[_] 

    def process(opts: V)(using Ctx): Either[BuildException, BuildOptions] =
      Try(processOption(opts).map(mkBuildOptions)).toEither.left.flatMap(e => defaultPosition(opts).error(e.getMessage)).flatten

    def processOption(opts: V)(using Ctx): Either[BuildException, T]
  }

  trait BaseStringSetting extends NamedDirectiveHandler[Positioned[String]]{
    def constrains = Single(ValueType.String)

    def exampleValues: Seq[String]

    def examples: Seq[String] = {
      def render(currentKeys: Seq[String], values: Seq[String]): Seq[String] =
        println(keys -> values)
        if currentKeys.size < exampleValues.size then render(currentKeys ++ keys, values)
        else if values.size < keys.size then render(keys, values ++ exampleValues)
        else currentKeys.zip(values).map { case (k, v) => s"//> using $k \"$v\"" }

      assert(keys.nonEmpty, "No keys for" + name)
      assert(exampleValues.nonEmpty, "No examples for" + name)
      render(keys, exampleValues)
    }

    def usagesCode: Seq[String] = keys.map(k => s"//> using $k \"$usageValue\"")

    def defaultPosition(v: Positioned[String]): Positioned[_]  = v
  }

  class StringSetting(
    parse: (String) => T,
    val exampleValues: Seq[String],
    val usageValue: String = "value",
  ) extends BaseStringSetting {
    def processOption(opt: Positioned[String])(using Ctx): Either[BuildException, T] = Right(parse(opt.value))
  }

  trait BaseStringListSetting extends NamedDirectiveHandler[::[Positioned[String]]]{
    def constrains = AtLeastOne(ValueType.String)
    def exampleValues: Seq[Seq[String]]

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

    def defaultPosition(v: ::[Positioned[String]]): Positioned[_]  = v.head

  }

  class StringListSetting(
    parse: Seq[String] => T,
    val exampleValues: Seq[Seq[String]],
    val usageValue: String = "value",
  ) extends BaseStringListSetting{

    def processOption(opt: ::[Positioned[String]])(using Ctx): Either[BuildException, T] = Right(parse(opt.map(_.value)))
  }

  class BooleanDirective(
    parse: Boolean => T,
    val usageValue: String = "value"  
  ) extends NamedDirectiveHandler[Positioned[Boolean]]{

    def constrains = Single(ValueType.Boolean)

    def processOption(opt: Positioned[Boolean])(using Ctx): Either[BuildException, T] = Right(parse(opt.value))

    def examples: Seq[String] =
      Seq(
        s"//> using ${keys(0)}",
        s"//> using ${keys(1)} false",
      )

    def usagesCode: Seq[String] = keys.map(k => s"//> using $k [true|false]")

    def defaultPosition(v: Positioned[Boolean]): Positioned[_]  = v
  }
}
