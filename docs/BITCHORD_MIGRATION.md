# MILLES MUSIC migration plan: BitChord foundation

## Goal

Use BitChord as the upstream technical foundation for playback and music-source functionality while retaining the MILLES MUSIC product identity, visual direction, and selected app experiences.

## Reviewed upstream

- Repository: https://github.com/kushagrasinghx/BitChord
- Revision reviewed: 21356206de5594ca0bdaf4680ca2db68a2287729
- License: GPL-3.0
- Android application package in upstream: `com.music.bitchord`
- Upstream modules include `:app`, `:shared`, and `:sharedUi`, with additional platform targets.

## Current MILLES MUSIC baseline

- Android application built with Kotlin and Jetpack Compose.
- Existing app entry point: `app/src/main/java/com/example/MainActivity.kt`.
- Main Compose shell: `app/src/main/java/com/example/ui/MillesMusicApp.kt`.
- Current catalogue and playback state are still prototype-oriented and use sample data.
- The existing MILLES MUSIC interface must be reviewed and mapped before replacing any screens.

## Upstream capabilities relevant to the migration

- Media3-based playback service and media session.
- Background playback and system media controls.
- Search, queue, track metadata, artwork, and lyrics infrastructure.
- Local music library support.
- Configurable music-source architecture.
- Shared Kotlin modules for data and UI.
- Existing playback features such as crossfade and audio-quality controls.

## Migration sequence

1. Preserve the current `main` branch. All work stays on `integration/bitchord-foundation` until tested.
2. Pin the exact BitChord upstream revision and preserve its GPL-3.0 license and notices.
3. Map the upstream Gradle modules, dependencies, Android manifest, package names, resources, and runtime services.
4. Bring in the upstream playback/data foundation without overwriting the existing MILLES MUSIC UI.
5. Adapt the application identity, package, icons, labels, and branding to MILLES MUSIC.
6. Connect the existing MILLES MUSIC screens to the upstream playback controller and real track models.
7. Replace sample-only search and playback paths with supported music-source integrations.
8. Keep each provider behind a source adapter. Do not treat unofficial endpoints as licensed distribution rights.
9. Run Gradle builds and tests. Fix compile, manifest, dependency, and runtime issues before proposing a pull request.
10. Only merge after the user has reviewed the working build and the migration.

## Important compatibility and licensing points

- BitChord is a substantial multi-module project. Its Gradle plugin, Kotlin, Compose, Media3, native libraries, and Android SDK requirements differ from the current MILLES MUSIC project.
- Do not copy only `PlaybackService.kt`. It depends on many upstream data, playback, platform, and UI classes.
- The GPL-3.0 license applies to distributed derivative work containing GPL-3.0-covered code. Keep source available and preserve notices.
- GPL-3.0 covers software, not music catalogues or sound-recording rights.
- YouTube Music and other unofficial source implementations must be assessed separately against their service terms. Their presence in upstream code is not permission to redistribute music.
- Keep credentials, signing keys, and private configuration out of Git.

## Build compatibility findings

The two projects cannot be merged by copying a single playback file or replacing one Gradle file:

- MILLES MUSIC currently uses AGP 9.1.1, Kotlin 2.4.21, compile SDK 36.1, and min SDK 24.
- The reviewed BitChord revision uses AGP 8.10.1, Kotlin 2.3.20, compile SDK 37, and min SDK 26 for its Android app.
- BitChord's playback service imports many upstream playback, data, audio, and platform classes. It is not a standalone file.
- BitChord is a large multi-module project with shared Kotlin and shared UI modules, native components, and a substantial dependency set.

The migration must resolve these differences deliberately. Do not downgrade the existing MILLES MUSIC toolchain or replace the app entry point until the imported project builds in isolation.

## Current status

- [x] Created an isolated integration branch.
- [x] Added the upstream GPL-3.0 license text and third-party notice.
- [x] Reviewed BitChord's GPL-3.0 license.
- [x] Reviewed upstream Gradle structure and Android playback service.
- [x] Identified the current MILLES MUSIC Compose entry points.
- [x] Added a minimal Media3 playback service host on the integration branch.
- [x] Added a MediaController connector for the UI layer.
- [x] Added a validated MediaItem factory for provider-resolved streams.
- [ ] Import and pin the complete upstream source tree or selectively port required source modules.
- [x] Connect the existing ViewModel and UI controls to MediaController state.
- [x] Replace simulated playback progress with real Media3 player state.
- [x] Add optional stream URL support to the domain track model.
- [ ] Add a compliant catalogue/source adapter that returns authorised playable streams.
- [ ] Build and test on Android.
