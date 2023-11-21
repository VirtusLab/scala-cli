# Release procedure reference

- [ ] Draft release notes using the `Draft new release` button in the `Releases` section of `scala-cli` GitHub page.
    - [ ] Create a tag for the new release.
    - [ ] Use the `Auto-generate release notes` feature to pre-populate the document with pull requests included in the
      release.
    - [ ] Fill in the remaining sections, as in previous releases (features worth mentioning, notable changes, etc).
    - [ ] Don't publish, save as draft instead
- [ ] Add the release notes on top
  of [the release notes doc](https://github.com/VirtusLab/scala-cli/blob/main/website/docs/release_notes.md) and
  create a PR.
    - [ ] Make sure the notes render correctly on [the website](https://scala-cli.virtuslab.org/docs/release_notes) - that
      includes swapping out GitHub-idiomatic @mentions of users, links to PRs, issues, etc.
      When using IntelliJ you can do that using the regexes in [release-notes-regexes.md](release-notes-regexes.md).
    - [ ] Copy any fixes over to the draft after getting the PR reviewed and merged.
- [ ] Mark the release draft as `pre-release` and then `Publish Release`
- [ ] Wait for a green release CI build with all the updated versions.
- [ ] ScalaCLI Setup
    - [ ] Merge pull request with updated Scala CLI version
      in [scala-cli-setup](https://github.com/VirtusLab/scala-cli-setup) repository. Pull request should be opened
      automatically after release.
    - [ ] Wait for the `Update dist` PR to be automatically created after the previous one has been merged, and then
      proceed to merge it.
    - [ ] Make a release with the updated Scala CLI version.
    - [ ] Update the `v0.1` tag to the newest tag.
      ```bash
      git fetch --all
      git checkout origin v1.0.x
      git tag -d v1.0
      git tag v1.0
      git push origin v1.0 -f 
      git tag -d v1
      git tag v1
      git push origin v1 -f
      ```
- [ ] Submit Scala CLI MSI installer `scala-cli-x86_64-pc-win32.msi` for malware analysis. The Msi file must be uploaded
  using this [service](https://www.microsoft.com/en-us/wdsi/filesubmission). For more information on this process, refer
  [here](windows-antimalware-analysis.md).
- [ ] Unmark release as `pre-release`.
- [ ] Announce the new release
    - [ ] announce on Twitter
    - [ ] announce on Discord
    - [ ] announce on Reddit if the release contains any noteworthy changes
- [ ] Create a ticket for the next release using the `Plan a release` template and assign it to the person responsible.
