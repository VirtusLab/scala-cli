# Regexes for preparing Scala CLI release notes

When auto-generating release notes from the GitHub UI, the notes will contain GitHub-idiomatic @mentions of users, 
links to PRs, issues, etc. Those have to then be swapped out for the corresponding Markdown syntax to be rendered 
correctly on our documentation website.
What's worse, the GitHub syntax has to be preserved in the GitHub release description, as using the full Markdown 
syntax required by our website breaks the GitHub mouse hover magic.

This has since been automated in the [process_release_notes.sc](../scripts/process_release_notes.sc) script.

```bash
# Check if the release notes need processing
.github/scripts/process_release_notes.sc check website/docs/release_notes.md
# Error: File ~/scala-cli/website/docs/release_notes.md contains patterns that need transformation
# The following patterns were found that should be transformed:
#   - Pattern: by @(.*?) in(?!.*\]\()
#   - Pattern: (?<!\[)@(.*?) made(?!.*\]\()
# Run: .github/scripts/process_release_notes.sc apply <file>

# Apply the regexes to fix the release notes
.github/scripts/process_release_notes.sc apply website/docs/release_notes.md 
# Applied regexes to: ~/scala-cli/website/docs/release_notes.md

# Verify that the release notes are now properly formatted
# This is the check we run on the CI, as well
.github/scripts/process_release_notes.sc verify website/docs/release_notes.md                  
# File /~/scala-cli/website/docs/release_notes.md is properly formatted
```

If you ever need to manually fix the release notes, you can use the old regexes below.

Do keep in mind that IDEA IntelliJ allows to automatically apply regexes when replacing text, so you can use that to
fix the release notes on the fly.

![image](img/apply-regexes-on-release-notes-in-intellij.png)

## PR link
Find: `in https\:\/\/github\.com\/VirtusLab\/scala\-cli\/pull\/(.*?)$` </br>
Replace: `in [#$1](https://github.com/VirtusLab/scala-cli/pull/$1)`

## Contributor link
Find: `by @(.*?) in` </br>
Replace: `by [@$1](https://github.com/$1) in`

## New contributor link
Find: `@(.*?) made` </br>
Replace: `[@$1](https://github.com/$1) made`

## No GH contributor link
Find: `by \[@(.*?).\(.*\) in` </br>
Replace: `by @$1 in`