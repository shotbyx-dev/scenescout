# SceneScout v0.9.0

## Fixed: actually shows local spots now
- **Discovery rewrite:** the old single Overpass regex query routinely timed out,
  leaving you with zero live spots. Replaced with three small exact-match queries
  (culture/art/viewpoints/museums/piers · historic/beach/marina/churches · parks)
  running in parallel — one slow query no longer kills the whole result.
- **Healthier endpoint first:** overpass-api.de is now primary (fresh data),
  kumi kept as fallback.
- Verified live: 46 real spots around the test location (murals, museums, piers,
  parks) through the actual app parser.

## Fixed: photos you can trust
- Card and map heroes now only show a photo when it actually depicts the place
  (Commons title relevance check). Random nearby photos (specimen scans, stores)
  are gone — you get a branded gradient instead of a misleading picture.
- Galleries sort most-relevant first; every photo carries its author credit.

## New: search that works
- Discover tab finally has a real search field — name, description, and tags,
  combinable with the shoot-type chips.

## New: Shotbyx cinematic redesign
- Near-black base, electric-red glow, chrome surfaces and type — the Shotbyx
  brand, not stock Material.
- Glass cards everywhere, ambient red glow behind every tab, animated tab
  transitions, staggered list entrances, shimmer photo loading.

## Smaller fixes
- Map tells you honestly when GPS is unavailable instead of pretending.
- Scout match bars now show the actual mood-match %.
- Planner "Custom…" location finally opens a text field.
- Timezones follow the device instead of hard-coded New York.
- Empty photo states explain themselves; AI copy no longer over-promises.

**Tested:** 67/67 unit tests green · live Overpass + Commons checks at real
coordinates · APK inspected (MapLibre native libs for all 4 ABIs).

*Created by Shotbyx.*
