import { TestBed } from '@angular/core/testing';
import * as L from 'leaflet';
import { LiveMap } from './live-map';
import { MAP_ZOOM, TRICITY_BOUNDS } from './map-config';

describe('LiveMap', () => {
  let element: HTMLElement;
  let map: L.Map;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LiveMap],
    }).compileComponents();

    const fixture = TestBed.createComponent(LiveMap);
    await fixture.whenStable();
    element = fixture.nativeElement;
    map = fixture.componentInstance['map'] as L.Map;
  });

  it('should render a map without markers', () => {
    expect(element.querySelector('.leaflet-container')).toBeTruthy();
    expect(element.querySelector('.leaflet-marker-icon')).toBeNull();
  });

  it('should not pan outside the bounds', () => {
    map.setView([52.2297, 21.0122], MAP_ZOOM.initial, { animate: false });

    expect(L.latLngBounds(TRICITY_BOUNDS).pad(0.001).contains(map.getCenter())).toBe(true);
  });

  it('should limit the zoom range', () => {
    map.setZoom(3, { animate: false });
    expect(map.getZoom()).toBe(MAP_ZOOM.min);

    map.setZoom(19, { animate: false });
    expect(map.getZoom()).toBe(MAP_ZOOM.max);
  });
});
