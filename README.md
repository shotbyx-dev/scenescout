# SceneScout

An Android app for videographers that finds beautiful, shoot-ready filming
locations — and tells you when the light is perfect and whether you need a permit.

## The idea

Open the app → see a map of your current location → nearby spots are pinned,
each with an AI scenic score (0–100), community star ratings, real shoot
reports ("nobody bothered us", "sun is perfect at golden hour"), golden-hour
timing, and permit guidance.

## v0.1 (this scaffold)

- **4 tabs**: Map, Discover (ranked + filterable by shoot type), AI Scout
  (describe the vibe, get ranked matches), Community (shoot reports).
- **Spot detail page**: score breakdown, golden-hour windows, permit info, reviews.
- **Scoring engine** (`ScenicScorer`): blends AI visual appeal + shootability +
  community stars into one 0–100 score with labels ("Must shoot" → "Skip").
- **Golden-hour calculator** (`SunTimes`): pure-Kotlin sunrise/sunset math.
- **Permit guide** (`PermitGuide`): curated film-office table (LA, NYC, Miami,
  Atlanta, Austin, National Parks) + safe default.
- **Sample data**: 5 Miami spots so the app runs offline on first launch.

## Roadmap (data sources)

| Feature | Source |
|---|---|
| Map + nearby scenic places | Google Maps SDK + Places API (reviews = the "comments database") |
| Street View imagery | Street View Static API → vision model scores each frame |
| AI scenic analysis | Gemini/Vertex vision API (or on-device model later) |
| Community spots, stars, notes | Firebase Firestore (free tier to start) |
| Permits | Curated film-office table → deep links; no central US API exists |

## Imagery strategy (best quality, legally clean)

| Job | Source | Why |
|---|---|---|
| Show the user what a spot looks like | Google Street View stills / Place Photos | Sharpest, most current — display is allowed with attribution |
| AI scenic scoring | Mapillary (CC BY-SA) + user uploads | Google ToS **forbids** feeding their imagery to AI models |

`data/imagery/` holds the split: `StreetView` (display-only URL builders),
`MapillaryClient` (API v4 search + sharpest-thumbnail picker),
`BestImagery` (picks sharpest display image; AI only ever sees Mapillary/uploads),
`ImageryRepository` (one call per spot). Every image carries its attribution,
shown under it in the app.

## Build

```bash
# API keys (both optional — the app degrades gracefully without them)
echo 'MAPS_API_KEY=your_google_key' >> local.properties
echo 'MAPILLARY_TOKEN=your_mapillary_token' >> local.properties  # free at mapillary.com/dashboard

./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug        # APK at app/build/outputs/apk/debug/
```

CI (`.github/workflows/android.yml`) runs tests + builds the APK on every push.
