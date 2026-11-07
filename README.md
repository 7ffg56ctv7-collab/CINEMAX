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
This repository is not yet a release-ready streaming application. A native SwiftUI iOS app shell and XcodeGen project definition now live in `iosApp/`, with a macOS GitHub Actions workflow that builds an unsigned iOS Simulator app artifact. Cross-addon search aggregation, external subtitle selection, richer playback controls, Nuvio repository discovery/JS sandbox, account sync, accessibility/localization polish, Apple signing, and end-to-end physical-device testing remain to be implemented.

## Build
Requirements: JDK 17+, Android SDK, and macOS/Xcode for iOS. See `iosApp/README.md` for the native iOS build commands. The iOS CI workflow builds the shared simulator framework, generates the Xcode project with XcodeGen, and packages an unsigned Simulator `.app` zip. This is not a signed iPhone installation package.

The Android CI workflow runs:
`gradle :composeApp:assembleDebug`

## Compatibility and safety
Stremio addons are remote HTTP services; their responses are parsed as data and never evaluated as code. Compatibility is provider-specific and must be tested. Nuvio plugins are a different executable JavaScript format; do not execute them without a hardened sandbox, explicit consent, and strict limits. Use only content sources you are authorized to access. CINEMAX does not host media.
