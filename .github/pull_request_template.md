[Tutorial for writing good descriptions]: https://cbea.ms/git-commit/

[//]: # (Be mindful that the PR title also needs to follow conventional commit standards)

# _Title of Change_

- Imperative, present tense description of a change that matches the
  [Tutorial for writing good descriptions].

[//]: # (e.g. "- Create 'androidLibrary' Gradle module.")

## JIRA ticket link:

- [DCMAW-XXXXX](https://govukverify.atlassian.net/browse/DCMAW-XXXXX)

## Evidence of the change:

[//]: # (Screenshots / uploaded videos go here)

## Checklist

### Before creating the pull request

- [ ] Commit messages that conform to conventional commit messages.
- [ ] Tests pass locally.
- [ ] Pull request has a clear title with a short description about the feature or update.
- [ ] Created a `draft` pull request if it's not ready for review.

### Before the CODEOWNERS review the pull request

- [ ] Complete all Acceptance Criteria within Jira ticket.
- [ ] Self-review code.
- [ ] Complete automated Testing:
  * [ ] Unit Tests.
  * [ ] Integration Tests.
  * [ ] Instrumentation / Emulator Tests.
- [ ] Handle PR comments.

### Before merging the pull request

- [ ] For **Defect**, **Tech Story** and **Story** tickets: Ensure that QA has reviewed (by checking they have the correct links to the correct JIRA tickets, the correct amount of details in the PR description to match the JIRA ticket, screenshot/video evidence attached when necessary) and commented QA approval.
- [ ] For **Tech Debt**, **Task** and **Spike** tickets (dev-only): Ensure that developer has reviewed (by checking they have the correct links to the correct JIRA tickets, the correct amount of details in the PR description to match the JIRA ticket, screenshot/video evidence attached when necessary) and commented Dev QA approval.
- [ ] [Sonar cloud report] passes inspections for your PR.
- [ ] Resolve all comments.

[Sonar cloud report]: https://sonarcloud.io/project/overview?id=mobile-android-networking
