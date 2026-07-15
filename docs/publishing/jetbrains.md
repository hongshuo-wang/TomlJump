# JetBrains Plugin Packaging

This document covers public build and packaging behavior for the TomlJump JetBrains plugin.

## Local Verification

Requirements:

- JDK 21 or newer.

Full verification:

```bash
./scripts/verify/jetbrains.sh
```

Build only the ZIP artifact:

```bash
./gradlew :plugins:jetbrains:buildPlugin
```

ZIP files are written under:

```text
plugins/jetbrains/build/distributions/
```

## Versioning

The default local development version lives in:

```text
gradle.properties
```

Update `pluginVersion` there when you want local builds to use a different default version:

```properties
pluginVersion=0.1.0
```

GitHub release builds use the pushed tag as the release version instead of requiring a version commit.
For example, pushing `v0.1.1` builds with `-PpluginVersion=0.1.1`.

Raw Gradle commands require Java 21 or newer, for example:

```bash
JAVA_HOME=/path/to/jdk21 ./gradlew :plugins:jetbrains:buildPlugin
```

## Marketplace Metadata

Project metadata:

- Plugin name: `TomlJump`
- Plugin ID: `com.tomljump`
- License: `MIT`
- Repository: `https://github.com/hongshuo-wang/TomlJump`
- Description: `JetBrains plugin for jumping from TOML config to files and source declarations.`
- Tags: `toml`, `navigation`, `configuration`, `kotlin`

Assets and copy covered by the repository:

- Plugin icon: `plugins/jetbrains/src/main/resources/META-INF/pluginIcon.svg`
- Dark plugin icon: `plugins/jetbrains/src/main/resources/META-INF/pluginIcon_dark.svg`
- Overview copy: `plugins/jetbrains/src/main/resources/META-INF/plugin.xml`
- Changelog: `plugins/jetbrains/src/main/resources/META-INF/plugin.xml`
- License: `LICENSE`

Recommended public assets for a Marketplace listing:

- At least one screenshot showing TOML navigation in a JetBrains IDE.
- A short demo GIF showing command/control click or Go to Declaration.

## GitHub Release Automation

GitHub releases are created from version tags that start with `v`.

Example:

```bash
git tag v0.1.1
git push origin v0.1.1
```

Pushing a tag matching `v*.*.*` starts `.github/workflows/release.yml`.
The workflow:

- derives the Gradle plugin version from the tag by removing the leading `v`;
- builds `:plugins:jetbrains:buildPlugin`;
- uploads the ZIP as a workflow artifact;
- creates a GitHub Release for the tag;
- uploads the plugin ZIP to the release;
- uses GitHub generated release notes for the tag-related commit summary.

If a release already exists for the tag, rerunning the workflow uploads the ZIP again with `--clobber`.
Existing release notes are not rewritten in that case.

## Release Checks

Before a public release, confirm these repository-visible items are ready:

- `pluginVersion` in `gradle.properties` has the release version.
- For GitHub releases, the pushed tag uses the `vX.Y.Z` format.
- `pluginSinceBuild` matches the minimum supported IntelliJ Platform build.
- `pluginUntilBuild` is omitted unless there is a known compatibility ceiling.
- Plugin icon
- Screenshots
- Description copy
- Changelog
- `./scripts/verify/jetbrains.sh` passes.
- ZIP artifact exists under `plugins/jetbrains/build/distributions/`.
