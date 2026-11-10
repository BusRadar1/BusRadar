import { type LatLngBounds, latLngBounds } from 'leaflet';
import type { StyleSpecification } from 'maplibre-gl';
import {
  BASE_MAPS,
  cleanStyle,
  HIDDEN_STYLE_LAYERS,
  MAP_ZOOM,
  TRICITY_BOUNDS,
  TRICITY_CENTER,
} from './map-config';

const ZTM_VEHICLE_EXTENT = latLngBounds([54.24097, 18.41426], [54.47558, 18.93416]);

const GDYNIA_EXTENT = latLngBounds([54.42275, 18.35798], [54.62416, 18.90983]);

const MARGIN_DEGREES = 0.04;

function withMargin(extent: LatLngBounds) {
  return latLngBounds(
    [extent.getSouth() - MARGIN_DEGREES, extent.getWest() - MARGIN_DEGREES],
    [extent.getNorth() + MARGIN_DEGREES, extent.getEast() + MARGIN_DEGREES],
  );
}

describe('map config', () => {
  const bounds = latLngBounds(TRICITY_BOUNDS);

  it('should keep the center inside the bounds', () => {
    expect(bounds.contains(TRICITY_CENTER)).toBe(true);
  });

  it('should order zoom levels as min < initial < max', () => {
    expect(MAP_ZOOM.min).toBeLessThan(MAP_ZOOM.initial);
    expect(MAP_ZOOM.initial).toBeLessThan(MAP_ZOOM.max);
  });

  it('should cover every ZTM vehicle position with a margin', () => {
    expect(bounds.contains(withMargin(ZTM_VEHICLE_EXTENT))).toBe(true);
  });

  it('should cover the whole of Gdynia with a margin', () => {
    expect(bounds.contains(withMargin(GDYNIA_EXTENT))).toBe(true);
  });
});

describe('base maps', () => {
  it('should credit OpenStreetMap on both maps', () => {
    expect(BASE_MAPS.clean.attribution).toContain('OpenStreetMap');
    expect(BASE_MAPS.osm.attribution).toContain('OpenStreetMap');
  });
});

describe('cleanStyle', () => {
  const style = {
    version: 8,
    sources: {},
    layers: [
      { id: 'background', type: 'background' },
      { id: 'building', type: 'fill' },
      { id: 'highway_minor', type: 'line' },
      { id: 'highway-name-major', type: 'symbol' },
      { id: 'road_shield_us', type: 'symbol' },
      { id: 'label_city', type: 'symbol' },
    ],
  } as StyleSpecification;

  const idsOf = (result: StyleSpecification) => result.layers.map((layer) => layer.id);

  it('should remove buildings, the airport and road shields', () => {
    const ids = idsOf(cleanStyle(style));

    for (const hidden of HIDDEN_STYLE_LAYERS) {
      expect(ids).not.toContain(hidden);
    }
  });

  it('should keep streets, street names and place labels', () => {
    expect(idsOf(cleanStyle(style))).toEqual([
      'background',
      'highway_minor',
      'highway-name-major',
      'label_city',
    ]);
  });

  it('should not change the style it was given', () => {
    const before = idsOf(style);

    cleanStyle(style);

    expect(idsOf(style)).toEqual(before);
  });
});
