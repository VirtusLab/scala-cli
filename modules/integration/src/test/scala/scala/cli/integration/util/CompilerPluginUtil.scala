package scala.cli.integration.util

import scala.cli.integration.TestInputs

object CompilerPluginUtil {
  def compilerPluginForScala2(pluginName: String, pluginErrorMsg: String): TestInputs = TestInputs(
    os.rel / "resources" / "scalac-plugin.xml" ->
      s"""<plugin>
         |    <name>$pluginName</name>
         |    <classname>localhost.DivByZero</classname>
         |</plugin>
         |""".stripMargin,
    os.rel / s"$pluginName.scala" ->
      s"""//> using resourceDir ./resources/
         |
         |package localhost
         |
         |import scala.tools.nsc
         |import nsc.Global
         |import nsc.Phase
         |import nsc.plugins.Plugin
         |import nsc.plugins.PluginComponent
         |
         |class DivByZero(val global: Global) extends Plugin {
         |  import global._
         |
         |  val name = "$pluginName"
         |  val description = "checks for division by zero"
         |  val components = List[PluginComponent](Component)
         |
         |  private object Component extends PluginComponent {
         |    val global: DivByZero.this.global.type = DivByZero.this.global
         |    val runsAfter = List[String]("refchecks")
         |    val phaseName = DivByZero.this.name
         |    def newPhase(_prev: Phase) = new DivByZeroPhase(_prev)
         |    class DivByZeroPhase(prev: Phase) extends StdPhase(prev) {
         |      override def name = DivByZero.this.name
         |      def apply(unit: CompilationUnit): Unit = {
         |        for ( tree @ Apply(Select(rcvr, nme.DIV), List(Literal(Constant(0)))) <- unit.body
         |              if rcvr.tpe <:< definitions.IntClass.tpe)
         |        {
         |          global.reporter.error(tree.pos, "$pluginErrorMsg")
         |        }
         |      }
         |    }
         |  }
         |}
         |""".stripMargin
  )

  def compilerPluginForScala3(pluginName: String, pluginErrorMsg: String): TestInputs = TestInputs(
    os.rel / "resources" / "plugin.properties" ->
      s"""pluginClass=localhost.DivideZero
         |""".stripMargin,
    os.rel / s"$pluginName.scala" ->
      s"""//> using resourceDir ./resources/
         |
         |package localhost
         |
         |import dotty.tools.dotc.ast.Trees.*
         |import dotty.tools.dotc.ast.tpd
         |import dotty.tools.dotc.core.Constants.Constant
         |import dotty.tools.dotc.core.Contexts.Context
         |import dotty.tools.dotc.core.Decorators.*
         |import dotty.tools.dotc.core.StdNames.*
         |import dotty.tools.dotc.core.Symbols.*
         |import dotty.tools.dotc.plugins.{PluginPhase, StandardPlugin}
         |import dotty.tools.dotc.transform.{Pickler, Staging}
         |import dotty.tools.dotc.report
         |
         |class DivideZero extends StandardPlugin:
         |  val name: String = "$pluginName"
         |  override val description: String = "checks for division by zero"
         |
         |  def init(options: List[String]): List[PluginPhase] =
         |    (new DivideZeroPhase) :: Nil
         |
         |class DivideZeroPhase extends PluginPhase:
         |  import tpd.*
         |
         |  val phaseName = "$pluginName"
         |
         |  override val runsAfter = Set(Pickler.name)
         |  override val runsBefore = Set(Staging.name)
         |
         |  override def transformApply(tree: Apply)(implicit ctx: Context): Tree =
         |    tree match
         |      case Apply(Select(rcvr, nme.DIV), List(Literal(Constant(0))))
         |      if rcvr.tpe <:< defn.IntType =>
         |        report.error("$pluginErrorMsg", tree.sourcePos)
         |      case _ =>
         |        ()
         |    tree
         |end DivideZeroPhase
         |""".stripMargin
  )
}
