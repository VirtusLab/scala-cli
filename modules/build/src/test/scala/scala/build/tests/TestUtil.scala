package scala.build.tests

import bloop.config.{Config => BloopConfig}
import scala.build.{Build, Project}
import scala.build.options.BuildOptions
import scala.build.internal.Constants
import scala.scalanative.{build => sn}

object TestUtil {

  implicit class TestBuildOps(private val build: Build) extends AnyVal {
    private def successfulBuild: Build.Successful =
      build.successfulOpt.getOrElse {
        sys.error("Compilation failed")
      }
    def generated(): Seq[os.RelPath] =
      os.walk(successfulBuild.output)
        .filter(os.isFile(_))
        .map(_.relativeTo(successfulBuild.output))
    def assertGeneratedEquals(expected: String*): Unit = {
      val generated0 = generated()
      assert(
        generated0.map(_.toString).toSet == expected.toSet, {
          pprint.log(generated0)
          pprint.log(expected)
          ""
        }
      )
    }
  }

  implicit class TestBuildOptionsOps(private val options: BuildOptions) extends AnyVal {
    def enableJs = {
      val config = BloopConfig.JsConfig(
        version = Constants.scalaJsVersion,
        mode = BloopConfig.LinkerMode.Debug,
        kind = BloopConfig.ModuleKindJS.CommonJSModule,
        emitSourceMaps = false,
        jsdom = None,
        output = None,
        nodePath = None,
        toolchain = Nil
      )
      options.copy(
        scalaJsOptions = options.scalaJsOptions.copy(
          enable = true
        )
      )
    }
    def enableNative = {
      val config = BloopConfig.NativeConfig(
        version = Constants.scalaNativeVersion,
        mode = BloopConfig.LinkerMode.Debug,
        gc = "default",
        targetTriple = None,
        clang = sn.Discover.clang(),
        clangpp = sn.Discover.clangpp(),
        toolchain = Nil,
        options = BloopConfig.NativeOptions(
          linker = sn.Discover.linkingOptions().toList,
          compiler = sn.Discover.compileOptions().toList
        ),
        linkStubs = false,
        check = false,
        dump = false,
        output = None
      )
      options.copy(
        scalaNativeOptions = options.scalaNativeOptions.copy(
          enable = true
        )
      )
    }
  }

  implicit class TestAnyOps[T](private val x: T) extends AnyVal {
    def is(expected: T): Unit =
      assert(
        x == expected, {
          pprint.log(x)
          pprint.log(expected)
          "Assertion failed"
        }
      )
  }
}
