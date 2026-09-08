# Releasing KhataGo

Version **1.0.0**, version code **1**, package `com.shohan.khatago`.

## Before you build

1. `./gradlew clean testDebugUnitTest` — unit tests must pass.
2. `./gradlew lintDebug` — lint is configured with `abortOnError = true`.
3. `./gradlew assembleRelease` — produces the unsigned or signed release APK.

## Signing

The build script deliberately ships **no keystore and no signing credentials**.
To produce a signed release locally, point Gradle at your own config, for example
in `~/.gradle/gradle.properties`:

```
KHATAGO_STORE_FILE=/absolute/path/to/keystore.jks
KHATAGO_STORE_PASSWORD=...
KHATAGO_KEY_ALIAS=khataGo
KHATAGO_KEY_PASSWORD=...
```

and add the matching `signingConfigs.release` block in `app/build.gradle.kts`
reading from those properties. Never commit a keystore or a password.

`app/proguard-rules.pro` already keeps Room, kotlinx-serialization and the
Compose contracts working under R8.

## GitHub release

The release workflow (`.github/workflows/release.yml`) runs on any `v*` tag: it
runs the unit tests and lint, builds the release APK, signs it when the four
`KHATAGO_*` secrets are present (otherwise it stays unsigned) and publishes a
GitHub Release for the tag.

Manual equivalent:

```bash
git tag v1.0.0
git push origin v1.0.0
gh release create v1.0.0 app/build/outputs/apk/release/app-release.apk \
    --title "KhataGo v1.0.0" \
    --notes "First release. See README.md for the full feature list."
```

## Checklist

- [x] Version name and code match (`1.0.0` / `1`).
- [x] Unit tests pass (87 unit tests, CI run `34226566255`).
- [x] Lint passes with no errors (CI run `34226566255`).
- [ ] Release APK installs and opens on a device with Android 8.0 (API 26)+.
- [ ] Fresh install shows onboarding, then setup, then an empty dashboard.
- [ ] No network permission in the merged manifest (verify with
      `aapt dump permissions app/build/outputs/apk/release/app-release.apk`).
