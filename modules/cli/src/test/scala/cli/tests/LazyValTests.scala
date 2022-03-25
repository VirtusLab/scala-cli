package cli.tests
import com.eed3si9n.expecty.Expecty.expect
import scala.cli.commands.SharedDirectoriesOptions
import scala.cli.commands.SharedOptions

case class TestBody(a: Int) {
  var objects = 0
  var lazyVals = 0
  var lazyVals2 = 0

  object Obj {
    objects += 1
  }
  lazy val lazyVal = {
    lazyVals += 1
    lazyVals
  }
  lazy val objVal = {
    lazyVals2 += 1
    List(123)
  }

  def alala = Obj.toString()
}

trait A {
  def a: Seq[String]
}

object AAAA {
  var a = 1
}

object AnApp extends App{
  AAAA.a = 123
  println(AAAA.a)
}

object A {
  println("AAAA")
  final case class AA(name: String) extends A {
    var count = 0
    lazy val a = {
      count += 1
      Seq(name)
    }
  }
}

class LazyValTests extends munit.FunSuite {
  test("a"){
    val b1 = new TestBody(1)
    val b2 = new TestBody(1)
    val a = Seq(b1.Obj, b2.Obj)

    expect(b1.objects == 1)
    expect(b2.objects == 1)
  }

  // test("a"){
  //   val b1 = new TestBody(1)
  //   val b2 = new TestBody(1)

  //   expect(b1.objVal != null)
  //   expect(b2.objVal != null)
  // }
  // test("Lazy vals are initialized once"){
  //   val b = new TestBody(1)
  //   expect(b.lazyVal == b.lazyVal)
  //   expect(b.lazyVal == 1)
  //   expect(b.lazyVals == 1)
  //   expect(b.lazyVals == 1)

  //   expect(b.objVal == List(123))
  //   expect(b.lazyVals2 == 1)
    
  // }
  // test("Objects are initialized once"){
  //   val b = new TestBody(1)
  //   expect(b.Obj == b.Obj)
  //   expect(b.objects == 1)
  //   expect(b.Obj == b.Obj)
  //   expect(b.objects == 1)
  //   expect(b.Obj.toString().contains("Obj"))
  // }

  // test("AAA"){
  //   val b = A.AA("ala")
  //   expect(b.a == Seq("ala"))
  //   expect(b.a == Seq("ala"))
    
  //   val a = b.count
  //   expect(a == 1)
    
  // }

  // test("bbb"){
  //   import scala.cli.commands.util.CommonOps._
  //   import scala.cli.commands.util.SharedOptionsUtil._
  //   expect(SharedOptions().directories.directories.localRepoDir != null)
  //   expect(SharedOptions().directories.directories.localRepoDir != null)
  //   val opt = SharedOptions().buildOptions(true, None)
  //   expect(opt != null)
  // }
}
