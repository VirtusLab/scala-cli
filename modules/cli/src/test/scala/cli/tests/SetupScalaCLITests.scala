package cli.tests

import com.eed3si9n.expecty.Expecty.expect

import java.util.{Collections, Properties}

import scala.build.internal.Constants
import scala.build.tests.TestInputs
import scala.cli.ScalaCli
import scala.jdk.CollectionConverters.{MapHasAsJava, MapHasAsScala}

class SetupScalaCLITests extends TestUtil.ScalaCliSuite {
  test(s"should read java properties from file") {
    val key    = "scala-cli"
    val value  = "true"
    val inputs = TestInputs(
      os.rel / Constants.jvmPropertiesFileName ->
        s"""-Xignored_1
           |-Xignored_2
           |-Xignored_3
           |-D$key=$value
           |""".stripMargin
    )
    inputs.fromRoot(root =>
      // save current props to restore them after test
      val currentProps = System.getProperties.clone().asInstanceOf[Properties]
      ScalaCli.loadJavaProperties(root)
      expect(sys.props.get(key).contains(value))

      expect(sys.props.get("ignored_1").isEmpty)
      expect(sys.props.get("ignored_2").isEmpty)
      expect(sys.props.get("ignored_3").isEmpty)
      // restore original props
      System.setProperties(currentProps)
    )
  }

  test(s"should read java properties from JAVA_OPTS and JDK_JAVA_OPTIONS") {
    // Adapted from https://stackoverflow.com/a/496849
    def setEnvVars(newEnv: Map[String, String]): Unit = {
      val classes = classOf[Collections].getDeclaredClasses
      val env     = System.getenv()
      for (cl <- classes)
        if (cl.getName.equals("java.util.Collections$UnmodifiableMap")) {
          val field = cl.getDeclaredField("m")
          field.setAccessible(true)
          val obj = field.get(env)
          val map = obj.asInstanceOf[java.util.Map[String, String]]
          map.clear()
          map.putAll(newEnv.asJava)
        }
    }

    val javaOptsValues       = "  -Xignored_1   -Dhttp.proxy=4.4.4.4   -Xignored_2"
    val jdkJavaOptionsValues = " -Xignored_3 -Dscala-cli=true  -Xignored_4"

    TestInputs().fromRoot(root =>
      //
      val currentEnv = System.getenv().asScala.toMap
      // modify environment variable of this process
      setEnvVars(Map("JAVA_OPTS" -> javaOptsValues, "JDK_JAVA_OPTIONS" -> jdkJavaOptionsValues))
      ScalaCli.loadJavaProperties(root)
      expect(sys.props.get("http.proxy").contains("4.4.4.4"))
      expect(sys.props.get("scala-cli").contains("true"))

      expect(sys.props.get("ignored_1").isEmpty)
      expect(sys.props.get("ignored_2").isEmpty)
      expect(sys.props.get("ignored_3").isEmpty)
      expect(sys.props.get("ignored_4").isEmpty)
      // reset the env
      setEnvVars(currentEnv)
    )
  }
}
