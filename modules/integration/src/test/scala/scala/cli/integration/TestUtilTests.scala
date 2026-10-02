package scala.cli.integration

class TestUtilTests extends ScalaCliSuite {
  test("background thread stack traces are dropped") {
    val lines = Vector(
      "Compiling project (Scala 3.3.8, JVM (17))",
      "Compiled project (Scala 3.3.8, JVM (17))",
      """Exception in thread "delete-bloop-bsp-named-socket" java.nio.file.AccessDeniedException: C:\proc-2656""",
      "\tat java.base@17.0.9/sun.nio.fs.WindowsFileSystemProvider.implDelete(WindowsFileSystemProvider.java:275)",
      "",
      "\tat java.base@17.0.9/java.nio.file.Files.deleteIfExists(Files.java:1191)",
      "\t... 3 more"
    )
    val expected = Vector(
      "Compiling project (Scala 3.3.8, JVM (17))",
      "Compiled project (Scala 3.3.8, JVM (17))"
    )
    assertEquals(TestUtil.dropBackgroundThreadStackTraces(lines), expected)
  }

  test("main thread stack traces are kept") {
    val lines = Vector(
      """Exception in thread "main" java.lang.Exception: boom""",
      "\tat Main$.main(Main.scala:2)"
    )
    assertEquals(TestUtil.dropBackgroundThreadStackTraces(lines), lines)
  }

  test("output following a background thread stack trace is kept") {
    val lines = Vector(
      """Exception in thread "delete-bloop-bsp-named-socket" java.io.IOException: nope""",
      "\tat Foo$.bar(Foo.scala:1)",
      "Compilation failed"
    )
    assertEquals(
      TestUtil.dropBackgroundThreadStackTraces(lines),
      Vector("Compilation failed")
    )
  }

  test("JVM unified logging lines are dropped") {
    val lines = Vector(
      """[9.163s][warning][os,thread] Failed to start thread "Unknown thread" - pthread_create failed (EAGAIN) for attributes: stacksize: 1024k, guardsize: 4k, detached.""",
      "Hello, world!",
      "[0.012s][info ][gc     ] Using G1",
      "[0.5s][error][os] something"
    )
    assertEquals(TestUtil.dropJvmLogLines(lines), Vector("Hello, world!"))
  }

  test("regular output resembling JVM unified logging is kept") {
    val lines = Vector(
      "[warning] not a JVM log line",
      "[1, 2, 3]",
      "[info][gc] missing decorations"
    )
    assertEquals(TestUtil.dropJvmLogLines(lines), lines)
  }
}
