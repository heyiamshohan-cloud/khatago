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

The release workflow (`.github/workflows/release.yml`, parked at
`workflows/release.yml` until the App has workflow permission) runs the unit
tests, builds the signed release APK when the four `KHATAGO_*` secrets are
present, and publishes a GitHub Release for a tag such as `v1.0.0`.

Manual equivalent:

```bash
git tag v1.0.0
git push origin v1.0.0
gh release create v1.0.0 app/build/outputs/apk/release/app-release.apk \
    --title "KhataGo v1.0.0" \
    --notes "First release. See README.md for the full feature list."
```

## Checklist

- [ ] Version name and code match (`1.0.0` / `1`).
- [ ] Unit tests pass.
- [ ] Lint passes with no errors.
- [ ] Release APK installs and opens on a device with Android 8.0 (API 26)+.
- [ ] Fresh install shows onboarding, then setup, then an empty dashboard.
- [ ] No network permission in the merged manifest (verify with
      `aapt dump permissions app/build/outputs/apk/release/app-release.apk`).
