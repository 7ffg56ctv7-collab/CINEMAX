# Contributing

1. Open an issue describing the feature or bug.
2. Keep platform-specific code in androidMain/iosMain and shared logic in commonMain.
3. Add tests for protocol parsing, URL construction and security-sensitive behavior.
4. Never execute downloaded provider code outside a sandbox.
5. Run `gradle :composeApp:assembleDebug` before submitting Android changes.
