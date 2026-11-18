import { InjectionToken } from '@angular/core';
import * as L from 'leaflet';
import type { StyleSpecification } from 'maplibre-gl';
import {
  BASE_MAPS,
  cleanStyle,
  MAP_ZOOM,
  MAPLIBRE_WORKER_PATH,
  TRICITY_BOUNDS,
} from './map-config';

export type CleanLayerFactory = () => Promise<L.Layer>;

export function createOsmLayer(): L.TileLayer {
  return L.tileLayer(BASE_MAPS.osm.tileUrl, {
    bounds: TRICITY_BOUNDS,
    minZoom: MAP_ZOOM.min,
    maxZoom: MAP_ZOOM.max,
    attribution: BASE_MAPS.osm.attribution,
  });
}

function isWebGlSupported(): boolean {
  const canvas = document.createElement('canvas');
  return !!(canvas.getContext('webgl2') ?? canvas.getContext('webgl'));
}

// MapLibre GL is large (about 1.1 MB), so it is loaded here with import() and stays out of the
// initial bundle. The map is only drawn once it has arrived.
export async function createCleanLayer(): Promise<L.Layer> {
  // Checked first: MapLibre cannot be created without WebGL, and Leaflet cannot cleanly undo a
  // layer that failed while being added. It also saves downloading MapLibre for nothing.
  if (!isWebGlSupported()) {
    throw new Error('WebGL is not supported in this browser');
  }

  const [maplibre, { maplibreGL }] = await Promise.all([
    import('maplibre-gl'),
    import('@maplibre/maplibre-gl-leaflet'),
  ]);
  maplibre.setWorkerUrl(
    new URL(`${MAPLIBRE_WORKER_PATH}?v=${maplibre.getVersion()}`, document.baseURI).toString(),
  );

  const response = await fetch(BASE_MAPS.clean.styleUrl);
  if (!response.ok) {
    throw new Error(`Map style request failed with status ${response.status}`);
  }
  const style = cleanStyle((await response.json()) as StyleSpecification);

  return maplibreGL({
    style,
    // The plugin shows this text in Leaflet's attribution control.
    attributionControl: { customAttribution: BASE_MAPS.clean.attribution },
  });
}

// Tests replace this factory, because jsdom has no WebGL.
export const CLEAN_LAYER_FACTORY = new InjectionToken<CleanLayerFactory>('CLEAN_LAYER_FACTORY', {
  providedIn: 'root',
  factory: () => createCleanLayer,
});
