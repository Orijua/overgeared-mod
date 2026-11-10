# Releasing

1. **Merge everything** that goes in the release into `main` and wait for the CI of `main` to be green.
2. **Version.** Set `mod_version` in `gradle.properties` (for example `0.1.0`).
3. **Changelog.** In `CHANGELOG.md`, rename `## [Unreleased]` to `## [0.1.0] - YYYY-MM-DD` and add a new empty `## [Unreleased]` above it.
4. **Commit** those two changes through a PR (the title says the result, for example "Version 0.1.0") and merge it.
5. **Tag** the merge commit and push the tag:

   ```bash
   git checkout main && git pull
   git tag v0.1.0
   git push origin v0.1.0
   ```

6. The **Release** workflow checks that the tag matches `mod_version`, takes the notes from the changelog section, runs the build and the game tests and publishes a GitHub release with the jar (a pre-release while the version starts with 0). If it fails, fix the cause, delete the tag (`git push origin --delete v0.1.0` and `git tag -d v0.1.0`) and tag again.
7. **CurseForge.** Download the jar from the GitHub release and upload it as a new file of the project: choose the release type (Alpha/Beta/Release), Minecraft 1.21.1, the NeoForge loader and Java 21, and paste the changelog section as the notes.
