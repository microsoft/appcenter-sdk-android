# Contributing to Visual Studio App Center SDK for Android

Welcome, and thank you for your interest in contributing to VS App Center SDK for Android!
The goal of this document is to provide a high-level overview of how you can get involved.

## Sending a pull request

Small pull requests are much easier to review and more likely to get merged. Make sure the PR does only one thing, otherwise please split it.

Please make sure the following is done when submitting a pull request:

### Workflow and validation

1. Fork the repository and create your branch from `develop`.
1. Run `git submodule update --init --recursive` before opening the solution if you don't want errors in the test application.
1. Use Android Studio 3.3 to edit and compile the SDK.
1. To run the test app that uses project references to the SDK sources:
   1. Select build variant `projectDependencyFirebaseDebug` before hitting gradle sync.
   1. Disable `Instant run` in Android Studio settings before running.
1. Make sure all tests have passed and your code is covered: run `gradlew coverageReport` command to generate report.
1. Make sure that there are no lint errors: run `gradlew assemble lint` command.
1. If your change includes a fix or feature related to the changelog of the next release, you have to update the **CHANGELOG.md**.
1. After creating a pull request, sign the CLA, if you haven't already.

### Code formatting

1. Make sure you name all the constants in capital letters.
1. Make sure you name all the classes in upper camel case.
1. Use blank line in-between methods.
1. No newlines within methods except in front of a comment.
1. Make sure you name all the properties/methods in camel case. Start private properties with `m`, static properties with `s`.
1. Use `{}` even if you have single operation in block.

### Comments

1. Use capital letter in the beginning of each comment and dot at the end.
1. For comments use `/* */` (multiline) even if it's only one line.
1. Provide JavaDoc for each `public` and `protected` class, method, property.
1. Use blank line between description and tags like `@return` or `@param`.
    > Tip: You can configure these options in Android Studio via File > Settings > Editor > Code Style > Java

## Thank You!

Your contributions to open source, large or small, constantly make projects better. Thank you for taking the time to contribute.
