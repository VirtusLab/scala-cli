//> using lib "com.github.sbt:junit-interface:0.13.2"
import org.junit.Test

class MyTests {

  @Test
  def foo(): Unit = {
    assert(2 + 2 == 4)
  }
}
