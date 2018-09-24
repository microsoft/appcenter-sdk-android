# Sasquatch Android App

An app to easily test crash reporting.

## How to build it

You need to install the [Google Breakpad libraries](https://chromium.googlesource.com/breakpad/breakpad). Follow their readme on how to [install them](https://commondatastorage.googleapis.com/chrome-infra-docs/flat/depot_tools/docs/html/depot_tools_tutorial.html#_setting_up) on your machine. Make sure the directory including Google's setup tools is available in your `$PATH`.  

After cloning, change into the `<repo_root>/apps/sasquatch/src/main/cpp/` directory. Run `fetch breakpad` to download the required libraries. This will put everything into a `src` directory. Rename that directory to `google-breakpad`. I couldn't figure out how to tell `depot_tools` to use a different name.

Open the project in Android Studio. Gradle should now be able to resolve all dependencies and build.
