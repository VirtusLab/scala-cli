package cli.tests

import caseapp.Tag
import com.eed3si9n.expecty.Expecty.expect
import com.github.plokhotnyuk.jsoniter_scala.core.{readFromArray, writeToArray}

import scala.cli.commands.{LoggingOptions, SharedOptions, VerbosityOptions}

class SetupIdeTests extends munit.FunSuite {

  test("should encode and decode verbosity options") {
    // encode
    val sharedOptions = SharedOptions(logging =
      LoggingOptions(verbosityOptions = VerbosityOptions(verbose = Tag.of(2)))
    )
    val scalaCliOptions = writeToArray(sharedOptions)(SharedOptions.jsonCodec)

    // decode
    val decodedSharedOptions = readFromArray(scalaCliOptions)(SharedOptions.jsonCodec)
    expect(decodedSharedOptions == sharedOptions)
  }

}
