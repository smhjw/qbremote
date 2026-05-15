# Repository Notes

## Android Release Packaging

- For Google Play releases, always use the bundled local toolchain under `tools/android-build/`.
- Preferred release command: `.\scripts\build-release-aab.ps1`
- Do not replace or rotate the existing release signing config unless the user explicitly asks. This app must keep using the current fixed signing identity so Google Play updates continue to work.
- If release packaging is needed, verify the signing configuration from `keystore.properties` and preserve the existing upload key flow.
