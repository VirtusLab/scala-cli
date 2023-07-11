package cli.tests

import com.eed3si9n.expecty.Expecty.expect

import java.util.Properties

import scala.build.internal.Constants
import scala.build.tests.TestInputs
import scala.cli.ScalaCli
import scala.util.Properties

class SetupScalaCLITests extends munit.FunSuite {

  test(s"should read java properties from file") {
    val key   = "scala-cli"
    val value = "true"
    val inputs = TestInputs(
      os.rel / Constants.jvmPropertiesFileName ->
        s"""-Xmx2048m
           |-Xms128m
           |-Xss8m
           |-D$key=$value
           |""".stripMargin
    )
    inputs.fromRoot(root =>
      // save current props to restore them after test
      val currentProps = System.getProperties.clone().asInstanceOf[Properties]
      ScalaCli.loadJavaProperties(root)
      expect(sys.props.get(key).contains(value))

      // restore original props
      System.setProperties(currentProps)
    )
  }
}
