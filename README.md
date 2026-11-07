# CINEMAX

CINEMAX is an early-stage, Arabic-first, cross-platform media client project built with Kotlin Multiplatform and Compose Multiplatform.

## Implemented in the current repository
- Shared Compose UI shell with Arabic labels.
- Android Activity/manifest and iOS Compose view-controller entry point.
- Stremio manifest, catalog, metadata and stream models.
- HTTPS manifest URL validation and HTTP repository methods for manifests, catalogs and streams.
- Add-on installation flow in the UI (manifest validation and first-catalog fetch).
- Nuvio provider abstraction and explicit security gate. Executable JavaScript providers remain disabled until a real sandbox is integrated.
- Shared protocol tests and an Android GitHub Actions build workflow.

## Not complete yet
This repository is not yet a release-ready streaming application. Persistent addon storage, robust catalog pagination/extra parameters, search aggregation, detailed metadata screens, subtitles, playback UI and native player integration, Nuvio repository discovery/JS sandbox, account sync, accessibility/localization polish, iOS project packaging, signing, and end-to-end device testing remain to be implemented.

## Build
Requirements: JDK 17+, Android SDK, and macOS/Xcode for iOS.
The Android CI workflow runs:
`gradle :composeApp:assembleDebug`

## Compatibility and safety
Stremio addons are remote HTTP services; their responses are parsed as data and never evaluated as code. Compatibility is provider-specific and must be tested. Nuvio plugins are a different executable JavaScript format; do not execute them without a hardened sandbox, explicit consent, and strict limits. Use only content sources you are authorized to access. CINEMAX does not host media.
