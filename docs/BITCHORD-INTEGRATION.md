# MILLES MUSIC playback integration

## Current migration stage

This branch introduces the AndroidX Media3 playback host. It does not replace the
MILLES MUSIC Compose UI or sample catalogue.

## BitChord reference

- Upstream: https://github.com/kushagrasinghx/BitChord
- Upstream license: GNU General Public License v3.0 (GPL-3.0)
- Upstream playback reference: `app/src/main/java/com/music/bitchord/playback/PlaybackService.kt`
- Upstream service architecture uses Media3 `MediaLibraryService`, a persistent
  ExoPlayer instance and a MediaSession. MILLES MUSIC starts with a smaller
  MediaSessionService host and will port only the required source/playback pieces.

## MILLES MUSIC decisions

- Keep the existing `com.example` Compose UI and visual design.
- Keep playback state out of Compose screen lifecycles.
- Connect UI through MediaController.
- Resolve authorised audio streams through a provider adapter before creating
  MediaItems. A catalogue result or YouTube page URL is not a playable stream.
- Add queue, seek, repeat, shuffle, playback state and media notification wiring
  in later integration steps.
- Do not import BitChord's YouTube extraction or signature-circumvention code.
  Provider terms and music rights must be checked independently.

## Current files

- `MillesPlaybackService.kt`: app-wide Media3 player and media session.
- `MillesMediaItemFactory.kt`: validates and converts resolved stream data.
- `gradle/libs.versions.toml`: pinned Media3 dependencies.

## Verification status

No Android build has been run by this GitHub-only change. Build and device tests
are required before merging this branch.
