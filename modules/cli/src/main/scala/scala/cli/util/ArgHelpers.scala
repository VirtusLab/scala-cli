package scala.cli.util

import caseapp.core.Arg
import caseapp.core.help.HelpFormat

import scala.cli.ScalaCli.allowRestrictedFeatures
import scala.cli.commands.shared.{HelpCommandGroup, HelpGroup}
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
    def withPrimaryGroup(primaryGroup: HelpGroup): HelpFormat =
      helpFormat.withPrimaryGroups(Seq(primaryGroup))
    def withPrimaryGroups(primaryGroups: Seq[HelpGroup]): HelpFormat = {
      val primaryStringGroups     = primaryGroups.map(_.toString)
      val oldSortedGroups         = helpFormat.sortedGroups.getOrElse(Seq.empty)
      val filteredOldSortedGroups = oldSortedGroups.filterNot(primaryStringGroups.contains)
      helpFormat.copy(sortedGroups = Some(primaryStringGroups ++ filteredOldSortedGroups))
    }
    def withHiddenGroups(hiddenGroups: Seq[HelpGroup]): HelpFormat =
      helpFormat.copy(hiddenGroups = Some(hiddenGroups.map(_.toString)))

    def withHiddenGroup(hiddenGroup: HelpGroup): HelpFormat =
      helpFormat.withHiddenGroups(Seq(hiddenGroup))
    def withHiddenGroupsWhenShowHidden(hiddenGroups: Seq[HelpGroup]): HelpFormat =
      helpFormat.copy(hiddenGroupsWhenShowHidden = Some(hiddenGroups.map(_.toString)))
    def withHiddenGroupWhenShowHidden(hiddenGroup: HelpGroup): HelpFormat =
      helpFormat.withHiddenGroupsWhenShowHidden(Seq(hiddenGroup))
    def withSortedGroups(sortedGroups: Seq[HelpGroup]): HelpFormat =
      helpFormat.copy(sortedGroups = Some(sortedGroups.map(_.toString)))
    def withSortedCommandGroups(sortedGroups: Seq[HelpCommandGroup]): HelpFormat =
      helpFormat.copy(sortedCommandGroups = Some(sortedGroups.map(_.toString)))

  }
}
