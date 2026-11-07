# CINEMAX iOS app

This folder contains the native SwiftUI iOS application shell. The shared screens and Stremio integration are provided by the Kotlin Multiplatform `CinemaxKit` framework.

## Build on macOS

Requirements: Xcode 16+, JDK 17+, Gradle 8.14+, and XcodeGen.

From the repository root:

```sh
gradle :composeApp:linkDebugFrameworkIosSimulatorArm64
cd iosApp
xcodegen generate
xcodebuild -project CINEMAX.xcodeproj -scheme CINEMAX -sdk iphonesimulator -configuration Debug CODE_SIGNING_ALLOWED=NO
```

For a physical iPhone, build the `iosArm64` framework and select an iPhone destination in Xcode. Device installation and App Store distribution require signing with an Apple Developer team and provisioning profile.

The project currently targets iOS 16.0 and supports iPhone and iPad. The iOS Simulator build is unsigned and is not an installable App Store IPA.
