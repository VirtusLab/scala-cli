package scala.cli.integration.compose

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j as b
import com.eed3si9n.expecty.Expecty.expect

import java.net.URI
import java.nio.file.Paths

import scala.async.Async.{async, await}
import scala.cli.integration.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters.*

trait ComposeBspTestDefinitions extends ScalaCliSuite { _: BspTestDefinitions =>
  test(
    "composed setup-ide should write the .bsp file in the directory where module config file was found"
  ) {
    val testInputs = TestInputs(
      os.rel / Constants.moduleConfigFileName ->
        """[modules.webpage]
          |dependsOn = ["core"]
          |
          |[modules.core]
          |roots = ["Core.scala", "Utils.scala"]
          |""".stripMargin,
      os.rel / "webpage" / "Website.scala" -> "",
      os.rel / "Core.scala"                -> "",
      os.rel / "Utils.scala"               -> ""
    )

    testInputs.fromRoot { root =>
      os.proc(TestUtil.cli, "--power", "setup-ide", ".", extraOptions).call(
        cwd = root,
        stdout = os.Inherit
      )
      val details                = readBspConfig(root)
      val expectedIdeOptionsFile = root / Constants.workspaceDirName / "ide-options-v2.json"
      val expectedIdeLaunchFile  = root / Constants.workspaceDirName / "ide-launcher-options.json"
      val expectedIdeInputsFile  = root / Constants.workspaceDirName / "ide-inputs.json"
      val expectedIdeEnvsFile    = root / Constants.workspaceDirName / "ide-envs.json"
      val expectedArgv = Seq(
        TestUtil.cliPath,
        "--power",
        "bsp",
        "--json-options",
        expectedIdeOptionsFile.toString,
        "--json-launcher-options",
        expectedIdeLaunchFile.toString,
        "--envs-file",
        expectedIdeEnvsFile.toString,
        (root / Constants.moduleConfigFileName).toString
      )
      expect(details.argv == expectedArgv)
      expect(os.isFile(expectedIdeOptionsFile))
      expect(os.isFile(expectedIdeInputsFile))
    }
  }

  test("composed bsp should have build targets for all modules and compile OK") {
    val testInputs = TestInputs(
      os.rel / Constants.moduleConfigFileName ->
        """[modules.core]
          |dependsOn = ["utils"]
          |
          |[modules.utils]
          |roots = ["Utils.scala", "Utils2.scala"]
          |""".stripMargin,
      os.rel / "core" / "Core.scala" ->
        """object Core extends App {
          |  println(Utils.util)
          |  println(Utils2.util)
          |}
          |""".stripMargin,
      os.rel / "Utils.scala"  -> "object Utils { def util: String = \"util\"}",
      os.rel / "Utils2.scala" -> "object Utils2 { def util: String = \"util2\"}"
    )

    withBsp(testInputs, Seq("--power", ".")) { (root, _, remoteServer) =>
      async {
        val buildTargetsResp = await(remoteServer.workspaceBuildTargets().asScala)
        val target = {
          val targets = buildTargetsResp.getTargets.asScala.map(_.getId).toSeq
          expect(targets.length == 4)
          expect(extractMainTargetsOfModules(targets).size == 2)
          expect(extractTestTargetsOfModules(targets).size == 2)
          extractMainTargets(targets.filter(_.getUri.contains("core")))
        }

        val targetUri = TestUtil.normalizeUri(target.getUri)
        checkTargetUri(root, targetUri)

        val targets = List(target).asJava

        {
          val resp = await {
            remoteServer
              .buildTargetDependencySources(new b.DependencySourcesParams(targets))
              .asScala
          }
          val foundTargets = resp.getItems.asScala.map(_.getTarget.getUri).toSeq
          expect(foundTargets == Seq(targetUri))
          val foundDepSources = resp.getItems.asScala
            .flatMap(_.getSources.asScala)
            .toSeq
            .map { uri =>
              val idx = uri.lastIndexOf('/')
              uri.drop(idx + 1)
            }
          if (actualScalaVersion.startsWith("2.")) {
            expect(foundDepSources.length == 1)
            expect(foundDepSources.forall(_.startsWith("scala-library-")))
          }
          else {
            expect(foundDepSources.length == 2)
            expect(foundDepSources.exists(_.startsWith("scala-library-")))
            expect(foundDepSources.exists(_.startsWith("scala3-library_3-3")))
          }
          expect(foundDepSources.forall(_.endsWith("-sources.jar")))
        }

        {
          val resp = await(remoteServer.buildTargetSources(new b.SourcesParams(targets)).asScala)
          val foundTargets = resp.getItems.asScala.map(_.getTarget.getUri).toSeq
          expect(foundTargets == Seq(targetUri))
          val foundSources = resp.getItems.asScala
            .map(_.getSources.asScala.map(_.getUri).toSeq)
            .toSeq
            .map(_.map(TestUtil.normalizeUri))
          val expectedSources = Seq(
            Seq(
              TestUtil.normalizeUri((root / "core" / "Core.scala").toNIO.toUri.toASCIIString)
            )
          )
          expect(foundSources == expectedSources)
        }

        val scalacOptionsResp = {
          val resp = await {
            remoteServer
              .buildTargetScalacOptions(new b.ScalacOptionsParams(targets))
              .asScala
          }
          val foundTargets = resp
            .getItems
            .asScala
            .map(_.getTarget.getUri)
            .map(TestUtil.normalizeUri)
          expect(foundTargets == Seq(targetUri))
          val foundOptions = resp.getItems.asScala.flatMap(_.getOptions.asScala).toSeq
          if (actualScalaVersion.startsWith("2."))
            expect(foundOptions.exists { opt =>
              opt.startsWith("-Xplugin:") && opt.contains("semanticdb-scalac")
            })
          else
            expect(foundOptions.contains("-Xsemanticdb"))
          resp
        }

        {
          val resp = await {
            remoteServer.buildTargetJavacOptions(new b.JavacOptionsParams(targets)).asScala
          }
          val foundTargets = resp
            .getItems
            .asScala
            .map(_.getTarget.getUri)
            .map(TestUtil.normalizeUri)
          expect(foundTargets == Seq(targetUri))
        }

        val classDir = os.Path(
          Paths.get(new URI(scalacOptionsResp.getItems.asScala.head.getClassDirectory))
        )

        {
          val resp = await(remoteServer.buildTargetCompile(new b.CompileParams(targets)).asScala)
          expect(resp.getStatusCode == b.StatusCode.OK)
        }

        os.walk(classDir).filter(os.isFile(_)).map(_.relativeTo(classDir))

//        if (actualScalaVersion.startsWith("3."))
//          expect(compileProducts.contains(os.rel / "simple$_.class"))
//        else
//          expect(compileProducts.contains(os.rel / "simple$.class"))
//

//        Thread.sleep(60*1000)
//
//        expect(
//          compileProducts.contains(os.rel / "META-INF" / "semanticdb" / "simple.sc.semanticdb")
//        )
      }
    }
  }

  private def extractMainTargetsOfModules(targets: Seq[BuildTargetIdentifier])
    : Seq[BuildTargetIdentifier] =
    targets.collect {
      case t if !t.getUri.contains("-test") => t
    }

  private def extractTestTargetsOfModules(targets: Seq[BuildTargetIdentifier])
    : Seq[BuildTargetIdentifier] =
    targets.collect {
      case t if t.getUri.contains("-test") => t
    }
}
