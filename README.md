# CINEMAX

CINEMAX is an early-stage, Arabic-first, cross-platform media client project built with Kotlin Multiplatform and Compose Multiplatform.

## Implemented in the current repository
- Shared Compose UI shell with Arabic labels.
- Android Activity/manifest and iOS Compose view-controller entry point.
- Stremio manifest, catalog, metadata and stream models.
- HTTPS manifest URL validation, preservation of addon configuration query tokens, and encoded Stremio resource paths.
- HTTP repository methods for manifests, catalogs (including declared search extras), metadata details, and streams.
- Add-on installation, switching/removal, persistent descriptors, catalog switching, search extras, and pagination when declared by the addon.
- Shared stream selection with native Android Media3 and iOS AVPlayer playback for secure direct media URLs.
- Nuvio provider abstraction and explicit security gate. Executable JavaScript providers remain disabled until a real sandbox is integrated.
- Shared protocol tests and an Android GitHub Actions build workflow.

## Not complete yet
This repository is not yet a release-ready streaming application. A native SwiftUI iOS app shell and XcodeGen project definition now live in `iosApp/`, with a macOS GitHub Actions workflow that builds unsigned iOS Simulator and iPhone app artifacts. Cross-addon search aggregation, external subtitle selection, richer playback controls, Nuvio repository discovery/JS sandbox, account sync, accessibility/localization polish, Apple signing, and end-to-end physical-device testing remain to be implemented.

## Build
Requirements: JDK 17+, Android SDK, and macOS/Xcode for iOS. See `iosApp/README.md` for the native iOS build commands. The iOS CI workflow builds device and simulator frameworks, combines them into an XCFramework, generates the Xcode project with XcodeGen, and packages unsigned Simulator and iPhone `.app` outputs. The iPhone output is not signed and is not directly installable or distributable.

The Android CI workflow runs:
`gradle :composeApp:testDebugUnitTest`
`gradle :composeApp:assembleDebug`
and uploads the debug APK as a workflow artifact.

## Optional signed iOS release

A manual workflow named **iOS signed release** is available under GitHub Actions. It archives the iPhone app and exports an App Store Connect IPA when the repository has these GitHub Actions secrets configured:

- `IOS_DISTRIBUTION_CERTIFICATE_BASE64`: base64-encoded Apple Distribution `.p12` certificate.
- `IOS_DISTRIBUTION_CERTIFICATE_PASSWORD`: password for that `.p12` file.
- `IOS_APP_STORE_PROFILE_BASE64`: base64-encoded App Store provisioning profile for `com.cinemax.app`.
- `IOS_TEAM_ID`: Apple Developer Team ID.

The certificate and provisioning profile must belong to the same Apple Developer team and match the app's bundle identifier. Add secrets in the repository's **Settings → Secrets and variables → Actions**, then run **Actions → iOS signed release → Run workflow**. Never commit signing certificates, provisioning profiles, passwords, or private keys to the repository. Without these owner-provided Apple signing assets, the workflow intentionally stops before signing.

## Compatibility and safety
Stremio addons are remote HTTP services; their responses are parsed as data and never evaluated as code. Compatibility is provider-specific and must be tested. Nuvio plugins are a different executable JavaScript format; do not execute them without a hardened sandbox, explicit consent, and strict limits. Use only content sources you are authorized to access. CINEMAX does not host media.
