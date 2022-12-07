# Thanks for contributing to Scala CLI!

This doc is meant as a guide on how best to contribute to Scala CLI.

## Creating issues

Whenever you happen upon something that needs improvement, be sure to come back to us and create an issue. Please make
use of the available templates and answer all the included questions, so that the maintenance team can understand your
problem easier.

## Pull requests

### Fork-Pull

We accept external pull requests according to
the [standard GitHub fork-pull flow](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request-from-a-fork).
Create a fork of this repository, commit your changes there and create a pull request. We try to review those as often
as possible.

### Main & stable branches

#### `main`

All code changes should branch from [main](https://github.com/VirtusLab/scala-cli/tree/main) (which is also the default
branch).

#### `stable` and documentation changes

However, documentation changes which don't depend on any not-yet-released code changes should branch
from [stable](https://github.com/VirtusLab/scala-cli/tree/stable). This allows the CI to immediately update the website.
A subsequent PR from `stable` back to `main` is created automatically.

### Rules for a well-formed PR

Whenever reasonable, we try to follow the following set of rules when merging code to the repository. Following those
will save you from getting a load of comments and speed up the code review.

- If the PR is meant to be merged as a single commit (`squash & merge`), please make sure that you modify only one
  thing.
    - This means such a PR shouldn't include code clean-up, a secondary feature or bug fix, just the single thing
      mentioned in the title.
    - If it's not obvious, please mention it in the PR description or a comment.
- Otherwise, make sure you keep all the commits nice and tidy:
    - all side-refactors, nitpick changes, formatting fixes and other side-changes should be extracted to separate
      commits with the `NIT` prefix in the commit message;
      - similarly, code review comments regarding such changes should be marked with the same prefix;
    - ensure everything compiles at every commit (`./mill -i __.compile`);
    - ensure everything is well formatted at every commit (`scala-cli fmt .` or `scalafmt`);
    - ensure imports are well-ordered at every commit (`./mill -i __.fix`);
    - ensure reference docs are up-to date at every commit (`./mill -i generate-reference-doc.run`);
    - ensure all tests pass at every commit (refer to the [dev docs](DEV.md) on how to run tests);
        - nobody expects you to run all the unit and integration tests for all platforms locally, that'd take too long;
        - just make sure the test suites relevant to your changes pass on your local machine.

Other notes:

- give a short explanation on what the PR is meant to achieve in the description, unless covered by the PR title;
- make sure to add tests wherever possible;
    - favor unit tests over integration tests where applicable;
- try to add scaladocs for key classes and functions;
- try to add comments where your code isn't self-explanatory;
- if you're changing the app behaviour or adding a new feature, make sure to add docs on the website (or note in the PR
  that you'll do it separately).