package scala.cli.runner

import munit.FunSuite

class RunnerTests extends FunSuite {

  private def run(className: String, args: Array[String] = Array.empty): String = {
    val out = new java.io.ByteArrayOutputStream()
    Console.withOut(out) {
      Runner.invokeMain(Class.forName(className), args)
    }
    out.toString("UTF-8").trim
  }

  test("static main(String[])") {
    assertEquals(run("scala.cli.runner.StaticWithArgs", Array("x")), "static-with-args:x")
  }

  test("static main()") {
    assertEquals(run("scala.cli.runner.StaticNoArgs"), "static-no-args")
  }

  test("instance main(String[])") {
    assertEquals(run("scala.cli.runner.InstanceWithArgs", Array("y")), "instance-with-args:y")
  }

  test("instance main()") {
    assertEquals(run("scala.cli.runner.InstanceNoArgs"), "instance-no-args")
  }

  test("prefer main(String[]) over main()") {
    assertEquals(run("scala.cli.runner.Both", Array("z")), "with-args:z")
  }

  test("reject instance main with private constructor") {
    intercept[NoSuchMethodException] {
      run("scala.cli.runner.PrivateCtor")
    }
  }
}

class StaticWithArgs
object StaticWithArgs {
  def main(args: Array[String]): Unit =
    println(s"static-with-args:${args.mkString}")
}

class StaticNoArgs
object StaticNoArgs {
  def main(): Unit = println("static-no-args")
}

class InstanceWithArgs {
  def main(args: Array[String]): Unit =
    println(s"instance-with-args:${args.mkString}")
}

class InstanceNoArgs {
  def main(): Unit = println("instance-no-args")
}

class Both {
  def main(args: Array[String]): Unit = println(s"with-args:${args.mkString}")
  def main(): Unit                    = println("no-args")
}

class PrivateCtor private () {
  def main(): Unit = println("unreachable")
}
