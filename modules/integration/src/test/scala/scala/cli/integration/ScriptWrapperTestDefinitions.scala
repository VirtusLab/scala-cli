package scala.cli.integration

import ch.epfl.scala.{bsp4j => b}
import com.eed3si9n.expecty.Expecty.expect

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._

trait ScriptWrapperTestDefinitions extends ScalaCliSuite { _: BspTestDefinitions =>
  private def appWrapperSnippet(wrapperName: String)    = s"object $wrapperName extends App {"
  private def classWrapperSnippet(wrapperName: String)  = s"final class $wrapperName$$_"
  private def objectWrapperSnippet(wrapperName: String) = s"object $wrapperName {"
  def expectScriptWrapper(
    path: os.Path,
    containsCheck: String => Boolean,
    doesNotContainCheck: String => Boolean
  ): Unit = {
    val generatedFileContent = os.read(path)
    assert(
      containsCheck(generatedFileContent),
      clue(s"Generated file content: $generatedFileContent")
    )
    assert(
      doesNotContainCheck(generatedFileContent),
      clue(s"Generated file content: $generatedFileContent")
    )
  }
  def expectAppWrapper(wrapperName: String, path: os.Path): Unit =
    expectScriptWrapper(
      path,
      _.contains(appWrapperSnippet(wrapperName)),
      content =>
        !content.contains(classWrapperSnippet(wrapperName)) &&
        !content.contains(objectWrapperSnippet(wrapperName))
    )

  def expectObjectWrapper(wrapperName: String, path: os.Path): Unit =
    expectScriptWrapper(
      path,
      _.contains(objectWrapperSnippet(wrapperName)),
      content =>
        !content.contains(classWrapperSnippet(wrapperName)) &&
        !content.contains(appWrapperSnippet(wrapperName))
    )

  def expectClassWrapper(wrapperName: String, path: os.Path): Unit =
    expectScriptWrapper(
      path,
      _.contains(classWrapperSnippet(wrapperName)),
      content =>
        !content.contains(appWrapperSnippet(wrapperName)) &&
        !content.contains(objectWrapperSnippet(wrapperName))
    )

  def testScriptWrappers(
    inputs: TestInputs,
    bspOptions: Seq[String] = Nil,
    extraOptionsOverride: Seq[String] = extraOptions
  )(expectWrapperFunction: (String, os.Path) => Unit)(implicit ec: ExecutionContext): Unit = {
    withBsp(
      inputs,
      inputs.fileNames ++ Seq("--power") ++ bspOptions,
      extraOptionsOverride = extraOptionsOverride
    ) {
      (root, _, remoteServer) =>
        async {
          val buildTargetsResp = await(remoteServer.workspaceBuildTargets().asScala)
          val targets          = buildTargetsResp.getTargets.asScala.map(_.getId).asJava
          val compileParams    = new b.CompileParams(targets)
          val buildResp        = await(remoteServer.buildTargetCompile(compileParams).asScala)
          expect(buildResp.getStatusCode == b.StatusCode.OK)
          val projectDir = os.list(root / Constants.workspaceDirName).filter(
            _.baseName.startsWith(root.baseName + "_")
          )
          expect(projectDir.size == 1)
          inputs.fileNames.map(_.stripSuffix(".sc")).foreach {
            scriptName =>
              expectWrapperFunction(
                scriptName,
                projectDir.head / "src_generated" / "main" / s"$scriptName.scala"
              )
          }
        }
    }
  }
}
