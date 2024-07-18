# Contributing to Camunda

We welcome new contributions. We take pride in maintaining and encouraging a friendly, welcoming, and collaborative community.

Anyone is welcome to contribute to Camunda! The best way to get started is to choose an existing [issue](#starting-on-an-issue).

For community-maintained Camunda projects, please visit the [Camunda Community Hub](https://github.com/camunda-community-hub). For connectors and process blueprints, please visit [Camunda Marketplace](https://marketplace.camunda.com/en-US/home) instead.

- [Prerequisites](#prerequisites)
  - [Contributor License Agreement](#contributor-license-agreement)
  - [Code of Conduct](#code-of-conduct)
- [GitHub issue guidelines](#github-issue-guidelines)
  - [Starting on an issue](#starting-on-an-issue)
- [Build Camunda from source](#build-camunda-from-source)
  - [Build Zeebe](#build-zeebe)
  - [Test execution](#test-execution)
    - [Test troubleshooting](#test-troubleshooting)
  - [Build profiling](#build-profiling)
- [Creating a pull request](#creating-a-pull-request)
- [Reviewing a pull request](#reviewing-a-pull-request)
  - [Review emoji code](#review-emoji-code)
  - [Stale pull requests](#stale-pull-requests)
- [Backporting changes](#backporting-changes)
- [Commit message guidelines](#commit-message-guidelines)
  - [Commit message header](#commit-message-header)
  - [Commit message body](#commit-message-body)

## Prerequisites
### Contributor License Agreement

You will be asked to sign our [Contributor License Agreement](https://cla-assistant.io/camunda-community-hub/community) when you open a Pull Request. We are not asking you to assign copyright to us but to give us the right to distribute your code without restriction. We ask this of all contributors to assure our users of the origin and continuing existence of the code. 

> [!NOTE]
> In most cases, you will only need to sign the CLA once.

### Code of Conduct

This project adheres to the [Camunda Code of Conduct](https://camunda.com/events/code-conduct/). By participating, you are expected to uphold this code. Please [report](https://camunda.com/events/code-conduct/reporting-violations/) unacceptable behavior as soon as possible.

## GitHub issue guidelines
If you want to report a bug or request a new feature, feel free to open a new issue on [GitHub][issues].

If you report a bug, please help speed up problem diagnosis by providing as much information as possible. Ideally, that would include a small [sample project][sample] that reproduces the problem.

> [!NOTE]  
>  If you have a general usage question, please ask on the [forum](forum).

Every issue should have a meaningful name and a description that either describes:
- A new feature with details about the use case the feature would solve or
improve
- A problem, how we can reproduce it, and what the expected behavior would be
- A change and the intention of how this would improve the system

### Starting on an issue

The `main` branch contains the current in-development state of the project. To work on an issue, follow these steps:

1. Check that a [GitHub issue][issues] exists for the task you want to work on.
   If one does not, create one. Refer to the [issue guidelines](#github-issue-guidelines).
2. Check that no one is already working on the issue, and make sure the team would accept a pull request for this topic. Some topics are complex and may touch multiple of [Camunda's Components](https://docs.camunda.io/docs/components/), requiring internal coordination.
3. Checkout the `main` branch and pull the latest changes.

   ```
   git checkout main
   git pull
   ```
4. Create a new branch with the naming scheme `issueId-description`.

   ```
   git checkout -b 123-adding-bpel-support
   ```
5. Follow the [Google Java Format](https://github.com/google/google-java-format#intellij-android-studio-and-other-jetbrains-ides)
   and [Zeebe Code Style](https://github.com/camunda/camunda/wiki/Code-Style) while coding.
6. Implement the required changes on your branch and regularly push your
   changes to the origin so that the CI can run. Code formatting, style, and
   license header are fixed automatically by running Maven. Checkstyle
   violations have to be fixed manually.

   ```
   git commit -am 'feat: add BPEL execution support'
   git push -u origin 123-adding-bpel-support
   ```
7. If you think you finished the issue, please prepare the branch for review. Please consider our [pull requests and code reviews](https://github.com/camunda/camunda/wiki/Pull-Requests-and-Code-Reviews) guide, before requesting a review. In general, the commits should be squashed into meaningful commits with a helpful message. This means cleanup/fix etc. commits should be squashed into the related commit. If you made refactorings it would be best if they are split up into another commit. Think about how a reviewer can best understand your changes. Please follow the [commit message guidelines](#commit-message-guidelines).
1. After finishing up the squashing, force push your changes to your branch.

   ```
   git push --force-with-lease
   ```

## Build Camunda from source
We are currently working on [architecture streamlining](https://camunda.com/blog/2024/04/simplified-deployment-options-accelerated-getting-started-experience/) to simplify the deployment and build process. While this is in progress, each component has its own build instructions. 

> [!NOTE]
> Zeebe is necessary for all components except Identity.

Build instructions by component:
* [Zeebe](#build-zeebe) (below)
* [Operate](operate/README.md)
* [Tasklist](tasklist/README.md)
* Identity - coming soon
* Optimize - coming soon

### Build Zeebe
Zeebe is a multi-module Maven project. To **quickly** build all components, run the command: `mvn clean install -Dquickly` in the root folder.

> [!NOTE]
> All Camunda core modules are built and tested with JDK 21. Most modules use language level 21, exceptions are: zeebe-bpmn-model, zeebe-client-java, zeebe-gateway-protocol zeebe-gateway-protocol-impl, zeebe-protocol and zeebe-protocol-jackson which use language level 8

For contributions to Camunda, building quickly is typically sufficient. However, users are recommended to build the full distribution.

To fully build the Zeebe distribution, run the command: `mvn clean install -DskipTests` in the root folder. This is slightly slower than building quickly but ensures the distribution is assembled completely. The resulting Zeebe distribution can be found in the folder `dist/target`, i.e.

```
dist/target/camunda-zeebe-X.Y.Z-SNAPSHOT.tar.gz
dist/target/camunda-zeebe-X.Y.Z-SNAPSHOT.zip
```

This distribution can be containerized with Docker (i.e. build a Docker image) by running:

```
docker build \
  --tag camunda/zeebe:local \
  --build-arg DISTBALL='dist/target/camunda-zeebe*.tar.gz' \
  --target app \
  .
```

This is a small overview of the contents of the different modules:
- `.ci` 
- `.github`
- `.idea`
- `.mvn`
- `authentication`
- `bom` - bill of materials (BOM) for importing Zeebe dependencies
- `build-tools` - Zeebe build tools
- `clients` - client libraries
- `dist` 
- `identity` - component within self-managed Camunda 8 responsible for authentication and authorization
- `licenses`
- `monitor` - Monitoring for self-managed Camunda 8
- `operate` - Monitoring tool for monitoring and troubleshooting processes running in Zeebe
- `parent` - Parent POM for all Zeebe projects
- `qa` 
- `search`
- `service`
- `spring-boot-starter-sdk` - official SDK for Spring Boot
- `tasklist` - graphical and API application to manage user tasks in Zeebe
- `testing` - testing libraries for processes and process applications
- `webapps-common` 
- `zeebe` - the process automation engine powering Camunda 8

### Test execution

Tests can be executed via Maven (`mvn verify`) or in your preferred IDE. The Zeebe Team uses mostly [Intellij IDEA](https://www.jetbrains.com/idea/), where we also [provide settings for](https://github.com/camunda/camunda/tree/main/.idea).

> [!TIP]
> To execute the tests quickly, run `mvn verify -Dquickly -DskipTests=false`.
> The tests will be skipped when using `-Dquickly` without `-DskipTests=false`.

#### Test troubleshooting

- If you encounter issues (like `java.lang.UnsatisfiedLinkError: failed to load the required native library`) while running the test StandaloneGatewaySecurityTest.shouldStartWithTlsEnabled take a look at https://github.com/camunda/camunda/issues/10488 to resolve it.

### Build profiling

The development team continues to push for a performant build.
To investigate where the time is spent, you can run your Maven command with the `-Dprofile` option.
This will generate a profiler report in the `target` folder.

## Creating a pull request

Before opening your first pull request, please have a look at this [guide](https://github.com/camunda/camunda/wiki/Pull-Requests-and-Code-Reviews#pull-requests).

1. To start the review process create a new pull request on GitHub from your branch to the `main` branch. Give it a meaningful name and describe your changes in the body of the pull request. Lastly add a link to the issue this pull request closes, i.e. by writing in the description `closes #123`. Without referencing the issue, our [changelog generation] will not recognize your PR as a new feature or fix and instead only include it in the list of merged PRs.
1. Assign the pull request to one developer to review, if you are not sure who should review the issue skip this step. Someone will assign a reviewer for you.
1. The reviewer will look at the pull request in the following days and give you either feedback or accept the changes. Your reviewer might use [emoji code](#review-emoji-code) during the reviewing process.
   1. If there are changes requested, address them in a new commit. Notify the reviewer in a comment if the pull request is ready for review again. If the changes are accepted squash them again in the related commit and force push. Then initiate a merge by adding your PR to the merge queue via the `Merge when ready` button.
   2. If no changes are requested, the reviewer will initiate a merge themselves.
1. When a merge is initiated, a bot will merge your branch with the latest
   `main` and run the CI on it.
   1. If everything goes well, the branch is merged and deleted and the issue and pull request are closed.
   2. If there are merge conflicts, the author of the pull request has to manually rebase `main` into the issue branch and retrigger a merge attempt.
   3. If there are CI errors, the author of the pull request has to check if they are caused by its changes and address them. If they are flaky tests, please have a look at this [guide](docs/ci.md#determine-flakiness) on how to handle them. Once the CI errors are resolved, a merge can be retried by simply enqueueing the PR again.

## Reviewing a pull request

Before doing your first review, please have a look at this [guide](https://github.com/camunda/camunda/wiki/Pull-Requests-and-Code-Reviews#code-reviews).

As a reviewer, you are encouraged to use the following [emoji code](#review-emoji-code) in your comments.

The review should result in:
- **Approving** the changes if there are only optional suggestions/minor issues 🔧, throughts 💭, or likes 👍
- **Requesting changes** if there are major issues ❌
- **Commenting** if there are open questions ❓

### Review emoji code

The following emojis can be used in a review to express the intention of a comment. For example, to distinguish a required change from an optional suggestion.

- 👍 or `:+1:`: This is great! It always feels good when somebody likes your work. Show them!
- ❓ or `:question:`: I have a question. Please clarify.
- ❌ or `:x:`: This has to change. It’s possibly an error or strongly violates existing conventions.
- 🔧 or `:wrench:`: This is a well-meant suggestion or minor issue. Take it or leave it. Nothing major that blocks merging.
- 💭 or `:thought_balloon:`: I’m just thinking out loud here. Something doesn’t necessarily have to change, but I want to make sure to share my thoughts.

_Inspired by [Microsoft's emoji code](https://devblogs.microsoft.com/appcenter/how-the-visual-studio-mobile-center-team-does-code-review/#introducing-the-emoji-code)._

### Stale pull requests

If there has not been any activity in your PR after a month, it is automatically marked as stale. If it remains inactive, we may decide to close the PR.
When this happens and you're still interested in contributing, please feel free to reopen it.

## Backporting changes

Some changes need to be copied to older versions. We use the [backport](https://github.com/zeebe-io/backport-action) Github Action to automate this process. Please follow these steps to backport your changes:

1. **Label the pull request** with a backport label (e.g. the label `backport stable/1.0` indicates that we want to backport this pull request to the `stable/1.0` branch).
   - if the pull request is _not yet_ merged, it will be automatically backported when it gets merged.
   - if the pull request is _already_ merged, create a comment on the pull request that contains
     `/backport` to trigger the automatic backporting.
2. The GitHub actions bot comments on the pull request once it finishes:
   - When _successful_, a new backport pull request was automatically created. Simply approve the PR
     and enqueue it to the merge queue by clicking the `Merge when ready` button.
   - If it _failed_, please follow these **manual steps**:
     1. Locally checkout the target branch (e.g. `stable/1.0`).
     2. Make sure it's up to date with the origin (i.e. `git pull`).
     3. Checkout a new branch for your backported changes (e.g. `git checkout -b
        backport-123-to-stable/1.0`).
     4. Cherry-pick your changes `git cherry-pick -x <sha-1>...<sha-n>`. You may need to resolve
        conflicts.
     5. Push your cherry-picked changes `git push`.
     6. Create a pull request for your backport branch:
        - Make sure it is clear that this backports in the title (e.g. `[Backport stable/1.0] Title of the original PR`).
        - Make sure to change the target of the pull request to the correct branch (e.g. `stable/1.0`).
        - Refer to the pull request in the description to link it (e.g. `backports #123`)
        - Refer to any issues that were referenced in the original pull request (e.g. `relates to #99`).

## Commit message guidelines

Commit messages use [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/#summary) format.

```
<header>
<BLANK LINE> (optional - mandatory with body)
<body> (optional)
<BLANK LINE> (optional - mandatory with footer)
<footer> (optional)
```

Camunda uses a GitHub Actions workflow to check your commit messages when a pull request is submitted. Please make sure to address any hints from the bot.

### Commit message header

Examples:

* `docs: add start event to bpmn symbol support matrix`
* `perf: reduce latency in backpressure`
* `feat: allow more than 9000 jobs in a single call`

The commit header should match the following pattern:

```
%{type}: %{description}
```

The commit header should be kept short, preferably under 72 chars but we allow a max of 120 chars.

- `type` should be one of:
  - `build`: Changes that affect the build system (e.g. Maven, Docker, etc)
  - `ci`: Changes to our CI configuration files and scripts (e.g. GitHub Actions, etc)
  - `deps`: A change to the external dependencies (was already used by Dependabot)
  - `docs`:  A change to the documentation
  - `feat`: A new feature (both internal or user-facing)
  - `fix`: A bug fix (both internal or user-facing)
  - `perf`: A code change that improves performance
  - `refactor`: A code change that does not change the behavior
  - `style`: A change to align the code with our style guide
  - `test`: Adding missing tests or correcting existing tests
- `description`: short description of the change in present tense

### Commit message body

Should describe the motivation for the change. This is optional but encouraged. Good commit messages explain what changed AND why you changed it. See [I've written a clear changelist description](https://github.com/camunda/camunda/wiki/Pull-Requests-and-Code-Reviews#ive-written-a-clear-changelist-description).



[issues]: https://github.com/camunda/camunda/issues
[forum]: https://forum.camunda.io/
[sample]: https://github.com/zeebe-io/zeebe-test-template-java
[clients/java]: https://github.com/camunda/camunda/labels/scope%2Fclients-java
[clients/go]: https://github.com/camunda/camunda/labels/scope%2Fclients-go
[changelog generation]: https://github.com/zeebe-io/zeebe-changelog


