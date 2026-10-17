# 8 Audio

8 Audio is the Android music-player build target based on the open-source ArchiveTune project.

## Build an APK

1. Open the **Actions** tab in this repository.
2. Select **Build 8 Audio APK**.
3. Choose **Run workflow** on the `main` branch.
4. When the job succeeds, open the run and download the `8-audio-arm64-debug` artifact.

The workflow checks out the upstream ArchiveTune source, applies the 8 Audio application label and application ID, and builds an arm64 debug APK. Debug APKs are for testing and are not signed for Play Store release.

## Source and license

- Upstream source: https://github.com/rukamori/ArchiveTune
- License: GNU General Public License v3.0 (GPL-3.0)

The upstream source and its copyright/license notices are retained during the build. This repository currently contains the build configuration; the upstream source tree is fetched by the workflow rather than vendored into this repository. The workflow tracks upstream `main`, so the exact upstream revision may change between builds.

No upstream signing keys are used. A separate release signing configuration and private key must be created for any distributable release.
