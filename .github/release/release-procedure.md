# Release procedure reference
- [ ] Draft release notes using the `Draft new release` button in the `Releases` section of `scala-cli` GitHub page.
  - [ ] Create a tag for the new release.
  - [ ] Use the `Auto-generate release notes` feature to pre-populate the document with pull requests included in the release.
  - [ ] Fill in the remaining sections, as in previous releases (features worth mentioning, notable changes, etc).
  - [ ] To prepare the `Contributors` section, checkout the repository locally and run the command below. 
    Make sure the new release tag is added on your local git repository to get the correct results.
  ```bash
  git shortlog -sn --no-merges {old-release-tag}...{new-release-tag}
  ```
- [ ] Create a pull request with supported scala-versions for the new release to this repository: [Virtuslab/scala-cli-scala-versions ](https://github.com/Virtuslab/scala-cli-scala-versions)
- [ ] Update the supported scala versions for the new release in the docs:
  - [ ] Scala: [scala-cli-scala-versions.md](https://github.com/VirtusLab/scala-cli/blob/main/website/docs/reference/scala-versions.md)
  - [ ] Scala.js: [scala-js.md](https://github.com/VirtusLab/scala-cli/blob/main/website/docs/guides/scala-js.md#supported-scalajs-versions)
- [ ] ScalaCLI Setup
   - [ ] Merge pull request with updated ScalaCLI version in [scala-cli-setup](https://github.com/VirtusLab/scala-cli-setup) repository. Pull request should be opened automatically after release.
   - [ ] Make a release with the updated ScalaCLI version.
   - [ ] Update v0.1 tag to the newest tag.
- [ ] Mark the release draft as `pre-release` and then `Publish Release`
- [ ] Wait for a green release CI build with all the updated versions.
- [ ] Unmark release as `pre-release`.
