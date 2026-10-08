package scala.cli.signing.util

import java.nio.file.Paths
import scala.cli.signing.shared.Secret

class BouncycastleSignerTest extends munit.FunSuite {
  test("init") {
    val secretKey: os.Path = {
      val uri = Thread.currentThread().getContextClassLoader
        .getResource("test-keys/key.skr")
        .toURI
      os.Path(Paths.get(uri))
    }
    val secretKeyBytes = os.read.bytes(secretKey)
    BouncycastleSigner(
      secretKey = Secret(secretKeyBytes),
      passwordOpt = Some(Secret("1234"))
    )
  }
}
