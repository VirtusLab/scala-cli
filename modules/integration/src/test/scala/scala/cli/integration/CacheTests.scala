package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

class CacheTests extends munit.FunSuite {

  protected lazy val extraOptions: Seq[String] = TestUtil.extraOptions

  protected val ciOpt: Seq[String] =
    Option(System.getenv("CI")).map(v => Seq("-e", s"CI=$v")).getOrElse(Nil)

  def cacheTest(): Unit = {
    val fileName = "Hello.scala"
    val message  = "Hello"
    val inputs = TestInputs(
      Seq(
        os.rel / fileName ->
          s"""object Hello extends App {
             |  println("$message")
             |}
             |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val baseImage     = Constants.dockerTestImage
      val cacheFileName = "cache.tar.gz"
      val cacheZip      = root / cacheFileName
      val tmpDir        = os.temp.dir()

      val cacheExportOutput =
        os.proc(TestUtil.cli, "cache", "export", extraOptions, "--output", cacheZip, fileName).call(
          cwd =
            root,
          env = Map(
            "COURSIER_CACHE"         -> (tmpDir / "cache" / "coursier").toString,
            "COURSIER_ARCHIVE_CACHE" -> (tmpDir / "cache" / "arch").toString,
            "XDG_CACHE_HOME"         -> (tmpDir / "cache" / "bloop").toString
          )
        ).out.text().trim

      println(cacheExportOutput)
      assert(os.exists(cacheZip))

      os.copy(os.Path(TestUtil.cliPath, os.pwd), root / "scala")
      val script =
        s"""#!/usr/bin/env sh
           |./scala cache import -c $cacheFileName
           |./scala ${extraOptions.mkString(" ") /* meh escaping */} $fileName | tee output
           |""".stripMargin
      os.write(root / "script.sh", script)
      os.perms.set(root / "script.sh", "rwxr-xr-x")
      val termOpt = if (System.console() == null) Nil else Seq("-t")

      // format: off
      val cmd = Seq[os.Shellable](
        "docker", "run", "--rm", termOpt,
        "-v", s"$root:/data",
        "-w", "/data",
        "--network", "none",
        ciOpt,
        baseImage,
        "/data/script.sh"
      )
      // format: on
      os.proc(cmd).call(cwd = root, stdout = os.Inherit)
      val rootOutput = os.read(root / "output").trim
      expect(rootOutput == message)
    }
  }

  if (Properties.isLinux && TestUtil.isNativeCli)
    test("cache test") {
      cacheTest()
    }

}
