# Release procedure reference

- [ ] Draft release notes using the `Draft new release` button in the `Releases` section of `scala-cli` GitHub page.
    - [ ] Create a tag for the new release.
    - [ ] Use the `Auto-generate release notes` feature to pre-populate the document with pull requests included in the
      release.
    - [ ] Fill in the remaining sections, as in previous releases (features worth mentioning, notable changes, etc).
    - [ ] To prepare the `Contributors` section, checkout the repository locally and run the command below.
      Make sure the new release tag is added on your local git repository to get the correct results.
  ```bash
  git shortlog -sn --no-merges {old-release-tag}...{new-release-tag}
  ```
    - [ ] Don't publish, save as draft instead
- [ ] Add the release notes on top
  of [the release notes doc](https://github.com/VirtusLab/scala-cli/blob/main/.github/release/release_notes.md) and
  create a PR.
    - [ ] Copy any fixes over to the draft after getting the PR reviewed and merged.
- [ ] ScalaCLI Setup
    - [ ] Merge pull request with updated Scala CLI version
      in [scala-cli-setup](https://github.com/VirtusLab/scala-cli-setup) repository. Pull request should be opened
      automatically after release.
    - [ ] Make a release with the updated Scala CLI version.
    - [ ] Update v0.1 tag to the newest tag.
- [ ] Mark the release draft as `pre-release` and then `Publish Release`
- [ ] Wait for a green release CI build with all the updated versions.
- [ ] Unmark release as `pre-release`.
