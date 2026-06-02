# Android Tweets

An Android app that lets you pick any location on a map and see Mastodon posts tagged with `#Android` and your chosen city. Originally a GWU course project; modernized from 2020-era patterns to current Android best practices.

---

## Running the app

### Prerequisites

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog (2023.1) or newer |
| JDK | 17 (bundled with Android Studio) |
| Android SDK | API 35 (install via SDK Manager) |
| Emulator or device | API 23+ |

No API keys, cloud accounts, or secrets are required.

### Steps

1. **Clone the repo**
   ```bash
   git clone https://github.com/bushidocodes/Android-Tweets.git
   cd Android-Tweets
   ```

2. **Open in Android Studio**
   File → Open → select the `Android-Tweets` directory.

3. **Sync Gradle**
   Android Studio will prompt you to sync. Let it finish — it downloads dependencies on first run (~200 MB including the OSMDroid tile library).

4. **Run**
   Select a device or emulator (API 23+) and press **Run** (▶).

### Logging in

The app uses local authentication — no Firebase, no account needed.

- **Email:** any address containing `@` (e.g. `demo@demo.com`)
- **Password:** any string ≥ 6 characters (e.g. `demo123`)

### Using the map

- **Long-press** anywhere on the OpenStreetMap map to drop a pin. The app reverse-geocodes the tap to a city name and the confirm button turns green.
- Tap the green **confirm button** to load posts.
- Tap the **location crosshair** (top-left) to jump to your device's current GPS position.

### The tweets screen

Shows Mastodon posts from [mastodon.social](https://mastodon.social) tagged with both `#Android` and `#<city>` (e.g. `#android + #london`). If the city has no co-tagged posts the feed falls back to global `#Android` posts.

---

## Architecture

```
LoginFragment
    └─ AuthViewModel          local email/password validator
MapsFragment
    └─ MapsViewModel          Geocoder (Android OS), LocationSelection state
TweetsFragment
    ├─ MapsViewModel          reads selected address (activity-scoped)
    └─ TweetsViewModel        Mastodon API via Retrofit + kotlinx.serialization
```

All three screens share a single `MainActivity` shell wired through the **Jetpack Navigation Component**. The address selected on the map travels to the tweets screen via an activity-scoped `MapsViewModel` — no Intent extras, no argument bundles.

### Key libraries

| Purpose | Library |
|---------|---------|
| Maps | [OSMDroid](https://github.com/osmdroid/osmdroid) (OpenStreetMap, no key needed) |
| Networking | Retrofit 2 + OkHttp + kotlinx.serialization |
| Social feed | [Mastodon public API](https://docs.joinmastodon.org/methods/timelines/) |
| Images | Coil |
| UI | Material 3 (`Theme.Material3.DayNight`) |
| Navigation | Jetpack Navigation Component |
| Async | Kotlin Coroutines + StateFlow |

---

## Tests

```bash
./gradlew testDebugUnitTest
```

39 unit tests across 5 classes:

| Class | What it covers |
|-------|---------------|
| `AuthViewModelTest` | Local validation (success, failure, state isolation) |
| `MappersTest` | DTO → Tweet HTML stripping, entity decoding |
| `TweetsViewModelTest` | City-filter → fallback → error propagation |
| `MastodonApiServiceTest` | HTTP parsing, unknown fields, URL params |
| `TweetTest` | Data class, no-arg constructor |

---

## CI

GitHub Actions runs on every push to `master` and every pull request:

```
JDK 17 + Gradle cache
  → ./gradlew testDebugUnitTest
  → ./gradlew lintDebug
  → ./gradlew assembleDebug
```

Test reports and the debug APK are uploaded as workflow artifacts.

---

## Project history

This started as a GWU CSCI 4237 course project (Fall 2020). The original used Twitter v1.1 geo-search, Firebase Auth, Google Maps, Anko `doAsync`, and Activity-based navigation with manual `onSaveInstanceState` — all replaced during this modernization pass:

| Before | After |
|--------|-------|
| Twitter API (paywalled) | Mastodon public hashtag timeline |
| Firebase Auth | Local validator (no cloud needed) |
| Google Maps SDK (API key) | OSMDroid / OpenStreetMap (no key) |
| `FusedLocationProviderClient` | Platform `LocationManager` |
| Anko `doAsync` | `viewModelScope` + Coroutines |
| 3 Activities + Intent extras | Navigation Component + 3 Fragments |
| AppCompat theme | Material 3 (`Theme.Material3.DayNight`) |
| Picasso | Coil |
| 2 placeholder tests | 39 unit tests across 5 classes |
