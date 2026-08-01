# Improving Muslim — Android

Native Android app for [Improving Muslim](https://improvingmuslim.com), built to match the
approach already used by the project's iPhone app: a real native app (Kotlin + Jetpack
Compose), not a website wrapped in an app. It reads the same public lecture feed the
website and iPhone app use, so there's one shared source of content.

## Update log

Newest first. Each entry says what changed and why, so you can review progress here
without reading code.

### 2026-08-01 — Home screen shows real lecture content
- Added data models matching the website's public feed
  (`https://improvingmuslim.com/api/v1/catalog.json`).
- Added networking to fetch that feed on launch.
- Replaced the placeholder "Hello Android" screen with a scrolling list of real lecture
  titles, speakers, and series info.
- Added loading and error states (with a "Try again" button) for when the feed can't be
  reached.
- **Found and worked around a data bug**: two episodes in the "Forty Hadith Nawawi" series
  have `"recap": true` in the website's data instead of actual text, which broke strict
  parsing. The app now tolerates this kind of bad data instead of crashing. The real fix
  still belongs on the website side — see [Known issues](#known-issues).

### 2026-08-01 — Project created
- Created the Android Studio project (Kotlin, Jetpack Compose, package
  `com.improvingmuslim.android`).
- Connected it to this GitHub repo.

## Known issues

- **Website data bug**: `data/forty-hadith-data.js` lines 105 and 197 (in the main website
  repo) have `"recap": true` where the field should be text or omitted. This likely affects
  the iPhone app too, since it expects text there as well. Needs a fix on the website side,
  not here.

## Current state

What works right now:
- App launches and fetches the live lecture catalog from the website.
- Shows a scrollable list of lecture titles, speakers, and series/episode context.
- Handles the loading and error cases.

Not built yet: video playback, search, topic/speaker browsing, saved items, watch
history, notes, account sign-in, offline downloads. These will be added in upcoming
slices, following the same order the iPhone app used.

## Run locally

Requirements: Android Studio, an emulator or Android device.

1. Open this folder in Android Studio.
2. Wait for Gradle sync to finish.
3. Click **Run ▶️** with an emulator or device selected.

## Project structure

```text
app/src/main/java/com/improvingmuslim/android/
├── MainActivity.kt          App entry point
├── model/                   Data models matching the website's JSON feed
├── data/                    Networking (fetches the live catalog)
└── ui/home/                 Home screen (list, loading, error states)
```
