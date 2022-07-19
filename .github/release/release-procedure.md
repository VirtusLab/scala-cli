# Release procedure reference

- [ ] Draft release notes using the `Draft new release` button in the `Releases` section of `scala-cli` GitHub page.
    - [ ] Create a tag for the new release.
    - [ ] Use the `Auto-generate release notes` feature to pre-populate the document with pull requests included in the
      release.
    - [ ] Fill in the remaining sections, as in previous releases (features worth mentioning, notable changes, etc).
    - [ ] Don't publish, save as draft instead
- [ ] Add the release notes on top
  of [the release notes doc](https://github.com/VirtusLab/scala-cli/blob/main/.github/release/release_notes.md) and
  create a PR.
    - [ ] Copy any fixes over to the draft after getting the PR reviewed and merged.
- [ ] Mark the release draft as `pre-release` and then `Publish Release`
- [ ] Wait for a green release CI build with all the updated versions.
- [ ] ScalaCLI Setup
    - [ ] Merge pull request with updated Scala CLI version
      in [scala-cli-setup](https://github.com/VirtusLab/scala-cli-setup) repository. Pull request should be opened
      automatically after release.
    - [ ] Make a release with the updated Scala CLI version.
    - [ ] Update v0.1 tag to the newest tag.
- [ ] Unmark release as `pre-release`.
- [ ] Announce new release on twitter and reddit if release contains noteworthy changes.
- [ ] Create a ticket for the next release using the `Plan a release` template and assign it to the person responsible.
