# ADR-005: Base layer of the live map

* **Status:** Proposed
* **Date:** 2026-11-10
* **Author:** TomaszDylik (frontend)
* **Deciders:** team (to be accepted at the Wednesday meeting)

## Context

The live map (#9) is a Leaflet 1.9.4 map with the standard OpenStreetMap raster tiles (`tile.openstreetmap.org`), limited to Tricity in #40 (bounds and zoom 11-18). The standard OSM style is dense: points of interest, shops, building details and many labels. Vehicle markers are not on the map yet, but when they are, they will compete with all of that. Issue #41 asks for a base map that shows mainly streets and the main street names, in muted light colours so the vehicles stand out. The base map source must be set in one place (`map-config.ts`) with attribution kept as the source's terms require, and a layer control to switch back to the standard OSM map is optional.

Constraints that drive the decision:

* There are no vehicle markers yet, so changing the map engine is cheapest now and gets more expensive with every feature built on top of it.
* The production build has an initial bundle budget of 500 kB (warning) and 1 MB (error). The current initial bundle is 361 kB raw (95 kB transferred).
* Unit tests run in Vitest with jsdom, which has no WebGL.
* The frontend image is built with `npm ci` and served by nginx without a Content Security Policy, so an external tile host is not blocked.
* This is a semester project with no budget, and the PR checklist forbids committing secrets and tokens.

What the providers offer (checked 2026-11-09 and 2026-11-10):

* **CARTO Basemaps (Positron raster).** Since 2026-09-23 every tile request needs a free API key (`?key=` in the tile URL). Free up to 1 million requests per month for commercial use and 5 million for non-commercial use. Attribution "© OpenStreetMap contributors, © CARTO" must be visible.
* **OpenFreeMap (vector tiles).** The public instance needs no key and no registration, and its terms of service (updated 2026-10-14) do not state any rate limit for normal use. The service is provided as-is, with no SLA, and may be discontinued without notice. Its quick start shows the attribution "OpenFreeMap © OpenMapTiles Data from OpenStreetMap", and OSM data has to be credited anyway.
* **OpenFreeMap Positron style** (`https://tiles.openfreemap.org/styles/positron`). 55 layers and none of them is a point-of-interest layer. The only building detail is the `building` layer. Street names (`highway-name-*`) and place labels (`label_*`) are present. Colours are already muted grey.
* **MapLibre GL JS 6.17.0** (BSD-3-Clause). The main file is 1.1 MB raw (about 295 kB gzip), plus a 520 kB worker (about 147 kB gzip) that runs in a Web Worker, plus 72 kB of CSS. These are CDN file sizes, not a measured Angular build.
* **`@maplibre/maplibre-gl-leaflet` 0.1.4** (ISC) adds a MapLibre GL layer to a Leaflet map. It is published by the MapLibre organisation and works with Leaflet 1.9.3+.

## Decision

We use Leaflet with a vector base layer: MapLibre GL through `@maplibre/maplibre-gl-leaflet`, with the OpenFreeMap Positron style. A function `cleanStyle()` removes the layers we do not want (`building`, `airport`, `highway-shield-*`, `road_shield_us`), and keeps streets, street names and place labels. All base map settings (style URL, attribution, and the standard OSM raster used as the second layer) live in `map-config.ts`.

MapLibre is loaded with a dynamic `import()`, because its main file alone (1.1 MB raw) would exceed the 1 MB error budget of the initial bundle. Vehicles stay Leaflet markers, and the layer control switches between the clean map and the standard OSM map. If the clean map cannot be created (no WebGL, no network, or the style cannot be downloaded), the map falls back to the standard OSM raster. The base layer is created through a factory injected into `LiveMap`, so tests can replace it with a stub.

This is the only option that meets every criterion of #41 without a key to manage, while keeping the Leaflet API for markers and popups. If the plugin turns out to be unreliable in practice, option C is the fallback.

## Considered Options

### Option A: Leaflet with CARTO Positron raster tiles

* **Pros:** Smallest change (about 1 hour). No new dependency, and Leaflet code and tests stay as they are. Positron is light and muted.
* **Cons:** Needs an API key, which would sit in the frontend bundle (a public credential that can be restricted by referrer, but it still needs a team decision against the "no tokens" checklist item). We depend on the vendor's quota and terms. The content of raster tiles is fixed, so we cannot remove or restyle individual elements.

### Option B: Leaflet with a MapLibre GL layer and OpenFreeMap Positron (chosen)

* **Pros:** No key and no stated limits. The style is trimmed in our code, so we decide what is on the map. Leaflet stays for markers, popups and the layer control. Attribution is in the style and in `map-config.ts`.
* **Cons:** A new engine in the bundle (about 295 kB gzip as a lazy chunk, plus a 147 kB worker). WebGL is required. The plugin is a 0.1.x release. Two engines share one view. jsdom tests need a stub. OpenFreeMap has no SLA.

### Option C: Replace Leaflet with MapLibre GL JS

* **Pros:** One engine. The best fit for many moving markers (GeoJSON layers, rotation by bearing). The same clean vector style as option B.
* **Cons:** Rewrites the map code and tests from #40 (coordinate order changes to `[lng, lat]`). Bigger than the M estimate of #41. Same bundle and testing costs as option B. The team knows Leaflet from #9.

### Option D: Self-hosted vector tiles (PMTiles extract of Tricity with MapLibre)

* **Pros:** No third-party service at runtime, no keys, and it could run offline in docker compose. Full control of the style.
* **Cons:** Needs a pipeline to build and host the tile file, and to update it. More infrastructure than #41 justifies at current traffic. Worth revisiting if OpenFreeMap proves unreliable.

### Option E: Keep the standard OSM raster and mute it with a CSS filter

* **Pros:** No dependency and a few lines of code.
* **Cons:** The filter only changes colours. Points of interest, buildings and labels stay, so the main criterion of #41 is not met. The standard OSM layer stays in the map as the second layer of the control.

## Consequences

* **Positive:** A clean, light base map without managing a key. Leaflet markers stay as they are. Sources and attribution are in one file. The standard OSM map remains one click away.
* **Negative:** A larger download for the map (loaded only when the map is shown). We depend on a volunteer-run service with no SLA. `cleanStyle()` removes layers by exact id, so a layer renamed upstream would silently stay on the map; the list has to be re-checked when the style changes.
* **Follow-up:** #41 (implementation PR). In that PR: measure the real bundle size, and decide whether to filter the style at runtime or commit a trimmed copy of the style JSON to `frontend/public/` (the OpenFreeMap quick start says customised styles are hosted by the user). Revisit option D if OpenFreeMap becomes unreliable.

## References

* Issue [#41](https://github.com/BusRadar1/BusRadar/issues/41): this ADR and the clean map
* PR [#45](https://github.com/BusRadar1/BusRadar/pull/45): the map limited to Tricity, which created `map-config.ts`
* [CARTO Basemaps](https://carto.com/basemaps/): API key, free tiers, attribution
* [OpenFreeMap quick start](https://openfreemap.org/quick_start/) and [terms of service](https://openfreemap.org/tos/)
* [OpenFreeMap Positron style](https://tiles.openfreemap.org/styles/positron)
* [maplibre-gl-leaflet](https://github.com/maplibre/maplibre-gl-leaflet)
