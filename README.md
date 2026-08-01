# Improving Muslim — Android

Native Android app for [Improving Muslim](https://improvingmuslim.com), built to match the
approach already used by the project's iPhone app: a real native app (Kotlin + Jetpack
Compose), not a website wrapped in an app. It reads the same public lecture feed the
website and iPhone app use, so there's one shared source of content.

## Update log

Newest first. Each entry says what changed and why, so you can review progress here
without reading code.

### 2026-08-01 — Match the website: top header and card style
- Added the sticky **top header bar** to match the mobile website: the "Improving
  Muslim" logo, a streak flame icon, a "Sign in" button, a settings gear, and the
  hamburger menu. These buttons are **visual placeholders for now** — sign-in, settings,
  streak, and the menu don't do anything yet; they'll be wired up when those features are
  built.
- Reworked the home feed to match the website's card style:
  - **Series** now show as a single card with an episode count (e.g. "9 Episodes"),
    instead of every episode appearing separately.
  - **Standalone lectures** show as their own cards with a duration (e.g. "1:01:43").
  - Category labels (e.g. "SERIES · FIQH, RIGHTEOUS PREDECESSORS") are now shown in
    rose/pink, matching the website.
- Not added yet (noted for a later day): the "Hide watched / Sort by" filter row, and the
  bottom navigation bar (Home / Explore / Pathways / Speakers / Profile). The hero header
  ("Learn Islam. Live it better.") is kept from the iPhone app even though the website
  doesn't show it on mobile — easy to remove later if you'd prefer to match the website
  exactly.

### 2026-08-01 — Home page design and polish
- Gave the home page its real look, matching the iPhone app's identity:
  - A hero header ("Learn Islam. Live it better.") with the serif editorial heading.
  - A row of scrollable topic filter pills (All, Purification, Worry & Distress, etc.);
    tapping one filters the list and updates the section heading and lecture count.
  - Proper lecture cards with 16:9 thumbnail images, a duration badge, the series/episode
    label in green, the title, and the speaker.
- Added the shared brand colour palette (parchment/green in light mode, deep green in dark
  mode), taken from the iPhone app so both apps look like the same product.
- Added light and dark mode support.
- Added image loading for thumbnails and fixed an issue where they were stuck spinning
  forever (the image loader needed to be told to measure the image before downloading it).

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
- A sticky top header (logo, streak, sign-in, settings, menu) — currently visual only.
- A designed home page: hero header, topic filter pills, and cards for both series (with
  episode counts) and standalone lectures (with durations), matching the website's style.
- Topic filtering (tap a pill to narrow the feed).
- Light and dark mode, matching the iPhone app's colours.
- Handles the loading and error cases.

Not built yet: tapping a card to open it, the "Hide watched / Sort by" filter row, the
bottom navigation bar, search, speaker browsing, saved items, watch history, notes,
account sign-in, offline downloads. The header buttons are placeholders until their
features exist.

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
└── ui/
    ├── theme/               Brand colours, light/dark theme
    ├── components/          Reusable pieces (lecture card, topic pill, artwork)
    └── home/                Home screen and its state
```
