import type { LatLngBoundsLiteral, LatLngTuple } from 'leaflet';

export const TRICITY_BOUNDS: LatLngBoundsLiteral = [
  [54.2, 18.31],
  [54.67, 18.99],
];

export const TRICITY_CENTER: LatLngTuple = [54.352, 18.6466];
export const MAP_ZOOM = { min: 11, initial: 12, max: 18 } as const;
