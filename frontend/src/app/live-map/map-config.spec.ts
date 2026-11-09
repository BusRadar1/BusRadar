import { type LatLngBounds, latLngBounds } from 'leaflet';
import { MAP_ZOOM, TRICITY_BOUNDS, TRICITY_CENTER } from './map-config';

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
