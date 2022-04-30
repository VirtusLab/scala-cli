package scala.build.preprocessing.directives

import scala.build.preprocessing.Scoped
import scala.build.options.BuildOptions
import caseapp.core.help.Help
import caseapp.core.app.Command
import shapeless.ops.hlist.Comapped
import os.makeDir.all
import caseapp.core.Arg


abstract class BuildOptionsHandler[V](val constrains: DirectiveConstrains[V])
    extends BaseBuildOptionsHandler[V]
    
case class DirectiveHandlerGroup(name: String, handlers: Seq[BaseBuildOptionsHandler[_]])
  
trait BaseBuildOptionsHandler[V] extends ConstraindDirectiveHandler[BuildOptions, V] {
  override def processScopedValues(scoped: Seq[Scoped[V]])(using Ctx) =
    scoped
      .flatMap(v => constrains.positions(v.value))
      .map(_.error(s"Scope is not supported in $name"))
      .sequenceToComposite


  protected def fromCommand(optName: String, help: Help[_]*): String =
    val arg = help.flatMap(_.args).find(a =>
      a.name.name == optName || a.extraNames.exists(_.name == optName)
    )
    arg.map(_.valueDescription.get.message)
    arg.fold("TODO no arg")(_.helpMessage.fold("TODO nod help")(_.message))


  def usageV: String = "" // TODO
  def valueFormat: String = ""


  def referenceMd(allCommands: Seq[Command[_]], link: Command[_] => String = _ => "TODO"): String = 
    val primaryKey = keys.head
    def matchKey(arg: Arg) = arg.name.name == primaryKey || arg.extraNames.exists(_.name == primaryKey)
    val matchingCommands = for
      command <- allCommands
      arg <- command.messages.args.find(matchKey)
    yield (command, arg)
    val arg = matchingCommands.map(_._2).distinct match 
      case Seq(arg) => Some(arg)
      case Nil => None
      case other => 
        throw new RuntimeException("Too many args: " + other)

    val uValue = 
      if usageV.nonEmpty then usageV 
      else arg.flatMap(_.valueDescription).fold("value")(_.description) 
    val desc = arg.flatMap(_.helpMessage).fold("No argument!")(_.message) // if description.isEmpty then arg.getOrElse("TODO") else description

    s"""### $name
       |
       |**Directives**: ${keys.map("`" + _ + "`").mkString(", ")}
       |
       |$desc
       |
       |**Usage**:
       |
       |${constrains.usage(primaryKey, uValue, valueFormat)}
       |
       |**Examples**:
       |
       |```
       |${examples.mkString("\n")}
       |```
       |""".stripMargin
}
