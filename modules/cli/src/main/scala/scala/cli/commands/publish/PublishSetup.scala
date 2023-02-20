package scala.cli.commands.publish

import caseapp.core.RemainingArgs
import caseapp.core.help.HelpFormat
import coursier.cache.ArchiveCache

import java.nio.charset.StandardCharsets

import scala.build.Ops.*
import scala.build.errors.CompositeBuildException
import scala.build.internal.CustomCodeWrapper
import scala.build.options.{BuildOptions, InternalOptions, Scope}
import scala.build.{CrossSources, Directories, Logger, Sources}
import scala.cli.ScalaCli
import scala.cli.commands.github.{LibSodiumJni, SecretCreate, SecretList}
import scala.cli.commands.publish.ConfigUtil.*
import scala.cli.commands.shared.SharedOptions
import scala.cli.commands.util.ScalaCliSttpBackend
import scala.cli.commands.{CommandUtils, ScalaCommand}
import scala.cli.config.{ConfigDb, Keys}
import scala.cli.internal.Constants
import scala.cli.util.ArgHelpers.*

object PublishSetup extends ScalaCommand[PublishSetupOptions] {

  override def group                   = "Main"
  override def scalaSpecificationLevel = SpecificationLevel.RESTRICTED

  override def helpFormat: HelpFormat =
    super.helpFormat.withPrimaryGroups(Publish.primaryHelpGroups)

  override def names = List(
    List("publish", "setup")
  )

  override def runCommand(
    options: PublishSetupOptions,
    args: RemainingArgs,
    logger: Logger
  ): Unit = {
    Publish.maybePrintLicensesAndExit(options.publishParams)

    val coursierCache = options.coursier.coursierCache(logger.coursierLogger(""))
    val directories   = Directories.directories

    lazy val configDb = ConfigDb.open(directories.dbPath.toNIO)
      .wrapConfigException
      .orExit(logger)

    val inputArgs = args.all

    val inputs = {
      val maybeInputs = SharedOptions.inputs(
        inputArgs,
        () => None,
        Nil,
        directories,
        logger,
        coursierCache,
        None,
        options.input.defaultForbiddenDirectories,
        options.input.forbid,
        Nil,
        Nil,
        Nil,
        Nil
      )
      maybeInputs match {
        case Left(error) =>
          System.err.println(error)
          sys.exit(1)
        case Right(inputs0) => inputs0
      }
    }

    val (pureJava, publishOptions) = {
      val cliBuildOptions = BuildOptions(
        internal = InternalOptions(
          cache = Some(coursierCache)
        )
      )

      val (crossSources, _) = CrossSources.forInputs(
        inputs,
        Sources.defaultPreprocessors(
          cliBuildOptions.scriptOptions.codeWrapper.getOrElse(CustomCodeWrapper),
          cliBuildOptions.archiveCache,
          cliBuildOptions.internal.javaClassNameVersionOpt,
          () => cliBuildOptions.javaHome().value.javaCommand
        ),
        logger,
        suppressDirectivesInMultipleFilesWarning = None
      ).orExit(logger)

      val crossSourcesSharedOptions = crossSources.sharedOptions(cliBuildOptions)
      val scopedSources = crossSources.scopedSources(crossSourcesSharedOptions).orExit(logger)
      val sources       = scopedSources.sources(Scope.Main, crossSourcesSharedOptions)

      val pureJava = sources.hasJava && !sources.hasScala

      (pureJava, sources.buildOptions.notForBloopOptions.publishOptions)
    }

    val backend = ScalaCliSttpBackend.httpURLConnection(logger)

    val checksInputOpt = options.checks.map(_.trim).filter(_.nonEmpty).filter(_ != "all")
    val checkKinds = checksInputOpt match {
      case None => OptionCheck.Kind.all.toSet
      case Some(checksInput) =>
        OptionCheck.Kind.parseList(checksInput)
          .left.map { unrecognized =>
            System.err.println(s"Unrecognized check(s): ${unrecognized.mkString(", ")}")
            sys.exit(1)
          }
          .merge
          .toSet
    }

    val missingFields =
      OptionChecks.checks(options, configDb, inputs.workspace, coursierCache, logger, backend)
        .filter(check => checkKinds(check.kind))
        .flatMap {
          check =>
            if (check.check(publishOptions)) {
              logger.debug(s"Found field ${check.fieldName}")
              Nil
            }
            else {
              logger.debug(s"Missing field ${check.fieldName}")
              Seq(check)
            }
        }

    if (missingFields.nonEmpty) {
      val count = missingFields.length
      logger.message(s"$count ${if (count > 1) "options need" else "option needs"} to be set")
      for (check <- missingFields)
        logger.message(s"  ${check.fieldName}")
      logger.message("") // printing an empty line, for readability
    }

    lazy val (ghRepoOrg, ghRepoName) = GitRepo.ghRepoOrgName(inputs.workspace, logger)
      .orExit(logger)

    lazy val token = options.token
      .map(_.toConfig)
      .orElse {
        configDb.get(Keys.ghToken)
          .wrapConfigException
          .orExit(logger)
      }
      .map(_.get())
      .getOrElse {
        System.err.println(
          s"No GitHub token passed, please specify one via --token env:ENV_VAR_NAME or --token file:/path/to/token, " +
            s"or by setting ${Keys.ghToken.fullName} in the config."
        )
        sys.exit(1)
      }

    if (options.check)
      if (missingFields.isEmpty)
        logger.message("Setup fine for publishing")
      else {
        logger.message("Found missing config for publishing")
        sys.exit(1)
      }
    else {

      val missingFieldsWithDefaults = missingFields
        .map { check =>
          check.defaultValue(publishOptions).map((check, _))
        }
        .sequence
        .left.map(CompositeBuildException(_))
        .orExit(logger)

      lazy val secretNames = {
        val secretList = SecretList.list(
          ghRepoOrg,
          ghRepoName,
          token,
          backend,
          logger
        ).orExit(logger)

        secretList.secrets.map(_.name).toSet
      }

      val missingSetSecrets = missingFieldsWithDefaults
        .flatMap {
          case (_, default) => default.ghSecrets
        }
        .filter(s => s.force || !secretNames.contains(s.name))

      if (missingSetSecrets.nonEmpty) {

        logger.message("") // printing an empty line, for readability
        logger.message {
          val name = if (missingSetSecrets.length <= 1) "secret" else "secrets"
          if (options.dummy)
            s"Checking ${missingSetSecrets.length} GitHub repository $name"
          else
            s"Uploading ${missingSetSecrets.length} GitHub repository $name"
        }

        LibSodiumJni.init(coursierCache, ArchiveCache().withCache(coursierCache), logger)

        lazy val pubKey = SecretCreate.publicKey(
          ghRepoOrg,
          ghRepoName,
          token,
          backend,
          logger
        ).orExit(logger)

        missingSetSecrets
          .map { s =>
            if (options.dummy) {
              logger.message(s"Would have set GitHub secret ${s.name}")
              Right(true)
            }
            else
              SecretCreate.createOrUpdate(
                ghRepoOrg,
                ghRepoName,
                token,
                s.name,
                s.value,
                pubKey,
                dummy = false,
                printRequest = false,
                backend,
                logger
              )
          }
          .sequence
          .left.map(CompositeBuildException(_))
          .orExit(logger)
      }

      var written = Seq.empty[os.Path]

      if (missingFieldsWithDefaults.nonEmpty) {

        val missingFieldsWithDefaultsAndValues = missingFieldsWithDefaults
          .map {
            case (check, default) =>
              default.getValue().map(v => (check, default, v))
          }
          .sequence
          .left.map(CompositeBuildException(_))
          .orExit(logger)

        val dest = {
          val ext = if (pureJava) ".java" else ".scala"
          inputs.workspace / s"publish-conf$ext"
        }
        val nl = System.lineSeparator() // FIXME Get from dest if it exists?
        val extraLines = missingFieldsWithDefaultsAndValues.map {
          case (_, _, None) => ""
          case (check, default, Some(value)) =>
            s"""//> using ${check.directivePath} "$value"""" + nl +
              default.extraDirectives
                .map {
                  case (k, v) =>
                    s"""//> using $k "$v"""" + nl
                }
                .mkString
        }

        val currentContent =
          if (os.isFile(dest)) os.read.bytes(dest)
          else if (os.exists(dest)) sys.error(s"Error: $dest already exists and is not a file")
          else Array.emptyByteArray
        val updatedContent = currentContent ++
          extraLines.toArray.flatMap(_.getBytes(StandardCharsets.UTF_8))
        os.write.over(dest, updatedContent)
        logger.message("") // printing an empty line, for readability
        logger.message(s"Wrote ${CommandUtils.printablePath(dest)}")
        written = written :+ dest
      }

      if (options.checkWorkflow.getOrElse(options.publishParams.isCi)) {
        val workflowDir = inputs.workspace / ".github" / "workflows"
        val hasWorkflows = os.isDir(workflowDir) &&
          os.list(workflowDir)
            .filter(_.last.endsWith(".yml")) // FIXME Accept more extensions?
            .exists(os.isFile)
        if (hasWorkflows)
          logger.message(
            s"Found some workflow files under ${CommandUtils.printablePath(workflowDir)}, not writing Scala CLI workflow"
          )
        else {
          val dest = workflowDir / "ci.yml"
          val content = {
            val resourcePath = Constants.defaultFilesResourcePath + "/workflows/default.yml"
            val cl           = Thread.currentThread().getContextClassLoader
            val resUrl       = cl.getResource(resourcePath)
            if (resUrl == null)
              sys.error(s"Should not happen - resource $resourcePath not found")
            val is = resUrl.openStream()
            try is.readAllBytes()
            finally is.close()
          }
          os.write(dest, content, createFolders = true)
          logger.message(s"Wrote workflow in ${CommandUtils.printablePath(dest)}")
          written = written :+ dest
        }
      }

      if (written.nonEmpty)
        logger.message("") // printing an empty line, for readability

      if (options.publishParams.isCi && written.nonEmpty)
        logger.message(
          s"Commit and push ${written.map(CommandUtils.printablePath).mkString(", ")}, to enable publishing from CI"
        )
      else
        logger.message("Project is ready for publishing!")

      if (!options.publishParams.isCi) {
        logger.message("To publish your project, run")
        logger.message {
          val inputs = inputArgs
            .map(a => if (a.exists(_.isSpaceChar)) "\"" + a + "\"" else a)
            .mkString(" ")
          s"  ${ScalaCli.progName} publish $inputs"
        }
      }
    }
  }
}
