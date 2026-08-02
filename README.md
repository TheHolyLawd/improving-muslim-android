# Improving Muslim — Android

Native Android client for [Improving Muslim](https://improvingmuslim.com), a library of
curated Islamic lectures. It is a companion to the web app and the iOS app, and reads the
**same public catalog feed** they publish, so all three clients stay in sync from one
source of content.

The app is deliberately native (Kotlin + Jetpack Compose), mirroring the iOS app's choice
of a real native client rather than a web wrapper. Where sensible, its structure and visual
identity follow the iOS app so the two feel like the same product.

> **Status:** early development. The Home screen is functional; several surfaces (search,
> auth, the other bottom-nav tabs, playback) are intentional placeholders. See
> [Roadmap](#roadmap).

---

## Table of contents

- [Tech stack](#tech-stack)
- [Getting started](#getting-started)
- [Architecture](#architecture)
- [Project layout](#project-layout)
- [The catalog data contract](#the-catalog-data-contract)
- [Features](#features)
  - [Catalog loading](#catalog-loading)
  - [Home feed](#home-feed)
  - [Topic filtering](#topic-filtering)
  - [Sorting & result count](#sorting--result-count)
  - [Top header](#top-header)
  - [Bottom navigation](#bottom-navigation)
  - [Watch screen & video playback](#watch-screen--video-playback)
  - [Theming (light/dark)](#theming-lightdark)
  - [Image loading](#image-loading)
- [Conventions](#conventions)
- [Known issues](#known-issues)
- [Roadmap](#roadmap)

---

## Tech stack

| Concern | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Min / target SDK | 24 / 37 |
| Networking | OkHttp |
| JSON | kotlinx.serialization |
| Images | Coil (`coil-compose`) |
| Video | Media3 / ExoPlayer (`media3-exoplayer`, `media3-ui`) |
| State | `ViewModel` + `StateFlow` |
| Build | Gradle (Kotlin DSL), version catalog in `gradle/libs.versions.toml` |

Application ID / namespace: `com.improvingmuslim.android`.

## Getting started

Requirements: Android Studio (recent stable), and an emulator or a physical device.

1. Open this folder in Android Studio and let Gradle sync finish.
2. Select a device/emulator and press **Run ▶**.

Command line (from the repo root):

```bash
./gradlew assembleDebug     # build the debug APK
./gradlew installDebug      # build + install on a running device/emulator
```

On Windows, Gradle needs a JDK. Android Studio ships one; point `JAVA_HOME` at it, e.g.
`D:\Android Studio\jbr`.

The app requests only the `INTERNET` permission (to fetch the catalog and thumbnails).

## Architecture

Unidirectional data flow, one `ViewModel` per screen:

```
Compose UI  ──user intent──▶  ViewModel  ──▶  Repository  ──▶  Network (OkHttp)
    ▲                            │
    └──────  StateFlow<UiState> ─┘
```

- **UI** is stateless Compose functions that render a `UiState` and forward user intent as
  lambdas. No UI reads the network or does business logic.
- **ViewModel** owns screen state, exposes it as an immutable `StateFlow`, and holds the
  filtering/sorting logic. It never references Compose or Android views.
- **Repository** is the single boundary to data. It fetches and decodes the catalog and
  hands back domain models. Swappable for a fake in tests via constructor injection.

Layering rule: UI → ViewModel → Repository → platform. Dependencies only point downward.

**Navigation** is intentionally lightweight for now: [`RootScreen`](app/src/main/java/com/improvingmuslim/android/ui/RootScreen.kt)
holds the selected bottom-nav tab and an optional "currently-watching" video in local
state. When a video is set, `RootScreen` shows [`WatchScreen`](app/src/main/java/com/improvingmuslim/android/ui/watch/WatchScreen.kt)
full-screen (over the tabs and nav bar) and a `BackHandler` clears it. If the screen graph
grows, this is the natural point to adopt Navigation Compose.

## Project layout

```text
app/src/main/java/com/improvingmuslim/android/
├── MainActivity.kt              Activity entry point; sets the Compose content + theme
├── data/
│   └── CatalogRepository.kt     Fetches + decodes the catalog feed (OkHttp)
├── model/
│   ├── Catalog.kt               Feed data models, HomeFeedItem, and feed helpers
│   ├── PlayableVideo.kt         Flattens a card into a playable video (+ date formatting)
│   └── LenientNullableStringSerializer.kt   Fault-tolerant string decoder (see Known issues)
└── ui/
    ├── RootScreen.kt            App scaffold: bottom nav, tab switching, Watch overlay
    ├── theme/
    │   ├── Brand.kt             Semantic brand palette + BrandColors CompositionLocal
    │   ├── Theme.kt             Maps brand palette into a Material 3 theme (light/dark)
    │   └── Type.kt              Typography
    ├── components/              Reusable, screen-agnostic UI
    │   ├── TopHeader.kt         Sticky header (logo, search, streak, menu)
    │   ├── TopicPill.kt         A single topic filter pill
    │   ├── FilterControls.kt    Sort dropdown
    │   ├── FeedCard.kt          Series/lecture card
    │   └── RemoteArtwork.kt     16:9 remote image with loading/empty states
    ├── home/
    │   ├── HomeScreen.kt        Home UI (hero, topic strip, filter row, card list)
    │   └── HomeViewModel.kt     Home state, filtering, sorting
    └── watch/
        └── WatchScreen.kt       Full-screen video player + lecture details
```

`res/drawable/ic_logo.xml` is the app logo as a vector (converted from the website's
`icon.svg`), so it scales crisply and needs no network.

## The catalog data contract

All content comes from one endpoint, shared with the web and iOS clients:

```
https://improvingmuslim.com/api/v1/catalog.json
```

Do **not** hand-maintain a second content list in this repo. The feed is the source of
truth. Models live in [`model/Catalog.kt`](app/src/main/java/com/improvingmuslim/android/model/Catalog.kt)
and are decoded with `kotlinx.serialization` (`ignoreUnknownKeys = true`, so additive feed
fields won't break older builds).

Top-level shape (fields the app relies on):

| Field | Meaning |
|---|---|
| `schemaVersion` | Contract version. A breaking change bumps this. |
| `catalogVersion` | Content hash; usable for cache invalidation. |
| `topics[]` | Filterable categories: `id`, `name`, `description`, `aliases`. |
| `speakers[]` | `id`, `name`, `imageURL`, `bio`. |
| `series[]` | A multi-episode series: `id`, `title`, `speaker`, `categories`, `thumbnailURL`, `episodeCount`, `episodes[]`. |
| `standaloneLectures[]` | One-off videos: `id`, `title`, `speaker`, `categories`, `thumbnailURL`, `duration`, `videoURL`. |

Stable identity (keep aligned with web/iOS so accounts can merge later):

- Series IDs are series slugs; episode IDs are source video IDs; standalone IDs are slugs.
- `categories` values are topic IDs and are matched against `topics[].id`.

## Features

Each subsection notes **what it does** and **where it lives**.

### Catalog loading

- **What:** On launch, fetches `catalog.json`. Shows a loading spinner, then the feed, or an
  error state with a **Try again** button if the request fails.
- **Where:** [`CatalogRepository`](app/src/main/java/com/improvingmuslim/android/data/CatalogRepository.kt)
  performs the request/decode on `Dispatchers.IO`;
  [`HomeViewModel`](app/src/main/java/com/improvingmuslim/android/ui/home/HomeViewModel.kt)
  exposes `HomeUiState.{Loading, Ready, Error}`.

### Home feed

- **What:** A single scrolling list mixing two card types, matching the website:
  - **Series** → one card with an episode-count badge (e.g. "9 Episodes").
  - **Standalone lecture** → one card with a duration badge (e.g. "1:01:43").
  Each card shows a rose category label (e.g. "SERIES · SEERAH, PROPHETS"), title, speaker,
  and 16:9 artwork.
- **Where:** `HomeFeedItem` (sealed type) + `Catalog.homeFeed()` build the feed in
  [`model/Catalog.kt`](app/src/main/java/com/improvingmuslim/android/model/Catalog.kt);
  [`FeedCard`](app/src/main/java/com/improvingmuslim/android/ui/components/FeedCard.kt)
  renders a card. `homeFeed()` also builds each card's category label by mapping category
  IDs through `topics`.

### Topic filtering

- **What:** A horizontal strip of pills (All + every topic). Tapping one filters the feed to
  items whose `categories` include that topic; tapping the active pill clears it.
- **Where:** `TopicStrip` in
  [`HomeScreen.kt`](app/src/main/java/com/improvingmuslim/android/ui/home/HomeScreen.kt) +
  [`TopicPill`](app/src/main/java/com/improvingmuslim/android/ui/components/TopicPill.kt);
  filtering is in `HomeViewModel.emitReady()`.

### Sorting & result count

- **What:** A "Sort by" dropdown with **Default** (shuffled once per load), **Featured
  order** (the feed's natural catalog order), **Most viewed**, and **A–Z**. A live count
  ("N series · M videos") reflects the current topic filter.
- **Where:** `SortOption` enum + sort logic in `HomeViewModel`;
  [`SortDropdown`](app/src/main/java/com/improvingmuslim/android/ui/components/FilterControls.kt)
  and `FilterSortBar` in `HomeScreen.kt`.

### Top header

- **What:** Sticky bar with the logo + wordmark on the left and, on the right, **Search**, a
  **Streak** flame with its score, and the **menu**. Order and contents match the current
  mobile website. Search, streak, and menu are **visual placeholders** until their features
  exist (`onSearch/onStreak/onMenu` are wired but no-op).
- **Where:** [`TopHeader`](app/src/main/java/com/improvingmuslim/android/ui/components/TopHeader.kt).

### Bottom navigation

- **What:** Five tabs — Home, Explore, Pathways, Speakers, Profile — with the active tab in
  the brand green. Home is the real screen; the other four render a "Coming soon"
  placeholder.
- **Where:** [`RootScreen`](app/src/main/java/com/improvingmuslim/android/ui/RootScreen.kt)
  owns the selected-tab state and the Material 3 `NavigationBar`.

### Watch screen & video playback

- **What:** Tapping any card opens a full-screen Watch screen that plays the video and shows
  its details — title, meta (speaker · topic · date), description, Key Takeaways, and recap.
  Standalone lectures play their own video; a series plays its first available episode (a
  dedicated episode-list screen is a follow-up). The player is real Media3/ExoPlayer with
  native controls, streaming the catalog's direct MP4 `videoURL` and loading the `.vtt`
  `captionsURL` as a subtitle track. The player is released when the screen is dismissed.
- **Where:** [`WatchScreen`](app/src/main/java/com/improvingmuslim/android/ui/watch/WatchScreen.kt)
  (player + content); [`PlayableVideo`](app/src/main/java/com/improvingmuslim/android/model/PlayableVideo.kt)
  + `HomeFeedItem.toPlayableVideo()` flatten a card into what the player needs;
  `RootScreen` hosts it (see [Navigation](#architecture)).
- **Note:** the website serves video via a blob URL, but the catalog exposes a direct MP4
  that native clients (iOS and this app) play directly. Takeaway/recap text uses lightweight
  `**bold**` markers in the feed; they're stripped to plain text for now.

### Theming (light/dark)

- **What:** A shared semantic palette (calm parchment + green in light, deep green in dark),
  taken from the iOS app so the clients look alike. Follows the system light/dark setting;
  Material's dynamic color is intentionally disabled to keep the brand identity.
- **Where:** [`Brand.kt`](app/src/main/java/com/improvingmuslim/android/ui/theme/Brand.kt)
  defines `BrandColors` + the `LocalBrandColors` CompositionLocal (access via `Brand.colors`
  in any composable); [`Theme.kt`](app/src/main/java/com/improvingmuslim/android/ui/theme/Theme.kt)
  provides it and maps it onto the Material scheme.

### Image loading

- **What:** Remote 16:9 thumbnails with a calm placeholder while loading and on failure.
- **Where:** [`RemoteArtwork`](app/src/main/java/com/improvingmuslim/android/ui/components/RemoteArtwork.kt)
  uses Coil's `SubcomposeAsyncImage`. Note: `SubcomposeAsyncImage` is used deliberately —
  `rememberAsyncImagePainter` defers loading until the image is drawn, which deadlocks when
  a spinner is shown *instead* of the image.

## Conventions

- **Colors:** never hard-code hex in UI. Use `Brand.colors.*`. Add new roles to
  `BrandColors` (both light and dark) rather than introducing literals.
- **State:** screens expose an immutable `StateFlow<UiState>`; composables receive state +
  intent lambdas and stay stateless where practical.
- **Data:** don't duplicate catalog content locally; extend the models in `model/` and rely
  on the feed. Keep IDs aligned with web/iOS.
- **New reusable UI** goes in `ui/components/`; screen-specific UI stays with its screen.
- **Placeholders:** unbuilt features are visible but inert (e.g. header buttons, non-Home
  tabs). Keep them clearly non-functional rather than faking data.
- **Generated/machine files** (`.idea/`, build output, `local.properties`) are git-ignored;
  don't commit them.

## Known issues

- **Upstream feed data bug:** two "Recap" episodes in the *Forty Hadith Nawawi* series have
  `"recap": true` (a boolean) where the feed schema expects text. Strict decoding would drop
  the entire catalog. The app tolerates this via
  [`LenientNullableStringSerializer`](app/src/main/java/com/improvingmuslim/android/model/LenientNullableStringSerializer.kt),
  which coerces non-string primitives to null. The real fix belongs in the website repo
  (`data/forty-hadith-data.js`); this likely affects the iOS app too.

## Roadmap

Rough order, following the iOS app's slices:

1. ~~Open a card → video playback.~~ Done (Watch screen).
2. Series episode-list screen (tap a series → choose an episode), plus "Up next" and
   "More like this" on the Watch screen.
3. Search.
4. Explore, Pathways, Speakers, Profile screens.
5. Account sign-in (in Profile) + cloud sync.
6. Saved items, watch history, notes, streaks (unlocks "Hide watched" and a real streak
   score). Resume playback position belongs here too.
7. Offline downloads, then release hardening.
