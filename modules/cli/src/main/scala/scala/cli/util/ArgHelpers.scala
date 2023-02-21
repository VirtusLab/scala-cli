package scala.cli.util

import caseapp.core.Arg
import caseapp.core.help.HelpFormat

import scala.cli.ScalaCli.allowRestrictedFeatures
import scala.cli.commands.{SpecificationLevel, tags}

object ArgHelpers {
  extension (arg: Arg) {
    def isExperimentalOrRestricted: Boolean =
      arg.tags.exists(_.name == tags.restricted) || arg.tags.exists(_.name == tags.experimental)

    def isSupported: Boolean = allowRestrictedFeatures || !arg.isExperimentalOrRestricted
    def isImportant: Boolean = arg.tags.exists(_.name == tags.inShortHelp)

    def isMust: Boolean = arg.tags.exists(_.name == tags.must)

    def level: SpecificationLevel = arg.tags
      .flatMap(t => tags.levelFor(t.name))
      .headOption
      .getOrElse(SpecificationLevel.IMPLEMENTATION)
  }

  extension (helpFormat: HelpFormat) {
    def withPrimaryGroup(primaryGroup: String): HelpFormat =
      helpFormat.withPrimaryGroups(Seq(primaryGroup))
    def withPrimaryGroups(primaryGroups: Seq[String]): HelpFormat = {
      val oldSortedGroups         = helpFormat.sortedGroups.getOrElse(Seq.empty)
      val filteredOldSortedGroups = oldSortedGroups.filterNot(primaryGroups.contains)
      helpFormat.copy(sortedGroups = Some(primaryGroups ++ filteredOldSortedGroups))
    }
  }
}
