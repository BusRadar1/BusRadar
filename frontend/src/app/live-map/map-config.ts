import type { LatLngBoundsLiteral, LatLngTuple } from 'leaflet';
import type { StyleSpecification } from 'maplibre-gl';

export const TRICITY_BOUNDS: LatLngBoundsLiteral = [
  [54.2, 18.31],
  [54.67, 18.99],
];

export const TRICITY_CENTER: LatLngTuple = [54.352, 18.6466];
export const MAP_ZOOM = { min: 11, initial: 12, max: 18 } as const;

// Both sources and their attribution are set here and nowhere else.
export const BASE_MAPS = {
  // Vector tiles from OpenFreeMap
  clean: {
    label: 'Clean map',
    styleUrl: 'https://tiles.openfreemap.org/styles/positron',
    attribution:
      '<a href="https://openfreemap.org" target="_blank" rel="noopener">OpenFreeMap</a> ' +
      '<a href="https://www.openmaptiles.org/" target="_blank" rel="noopener">&copy; OpenMapTiles</a> ' +
      'Data from <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap</a>',
  },
  // The standard OpenStreetMap raster
  osm: {
    label: 'OpenStreetMap',
    tileUrl: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
    attribution:
      '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
  },
} as const;

export const MAPLIBRE_WORKER_PATH = 'maplibre/maplibre-gl-worker.mjs';

// Layers of the OpenFreeMap Positron style that we do not want on a map of streets:
// building details, the airport label and road shields. Streets, street names and place labels stay.
export const HIDDEN_STYLE_LAYERS: readonly string[] = [
  'building',
  'airport',
  'highway-shield-non-us',
  'highway-shield-us-interstate',
  'road_shield_us',
];

export function cleanStyle(style: StyleSpecification): StyleSpecification {
  return {
    ...style,
    layers: style.layers.filter((layer) => !HIDDEN_STYLE_LAYERS.includes(layer.id)),
  };
}
