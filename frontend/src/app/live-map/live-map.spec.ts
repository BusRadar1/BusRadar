import { TestBed } from '@angular/core/testing';
import * as L from 'leaflet';
import { CLEAN_LAYER_FACTORY, type CleanLayerFactory } from './base-layers';
import { LiveMap } from './live-map';
import { BASE_MAPS, MAP_ZOOM, TRICITY_BOUNDS } from './map-config';

describe('LiveMap', () => {
  async function createMap(factory: CleanLayerFactory) {
    TestBed.configureTestingModule({
      imports: [LiveMap],
      providers: [{ provide: CLEAN_LAYER_FACTORY, useValue: factory }],
    });
    const fixture = TestBed.createComponent(LiveMap);
    await fixture.whenStable();
    return {
      fixture,
      element: fixture.nativeElement as HTMLElement,
      map: fixture.componentInstance['map'] as L.Map,
    };
  }

  function tileLayers(map: L.Map): L.TileLayer[] {
    const layers: L.TileLayer[] = [];
    map.eachLayer((layer) => {
      if (layer instanceof L.TileLayer) {
        layers.push(layer);
      }
    });
    return layers;
  }

  describe('with the clean map available', () => {
    const clean = L.layerGroup();
    let element: HTMLElement;
    let map: L.Map;

    beforeEach(async () => {
      ({ element, map } = await createMap(() => Promise.resolve(clean)));
    });

    it('should render a map without markers', () => {
      expect(element.querySelector('.leaflet-container')).toBeTruthy();
      expect(element.querySelector('.leaflet-marker-icon')).toBeNull();
    });

    it('should show the clean map and not the standard OpenStreetMap map', () => {
      expect(map.hasLayer(clean)).toBe(true);
      expect(tileLayers(map)).toHaveLength(0);
    });

    it('should offer both maps in the layer control, with the clean map selected', () => {
      const labels = Array.from(element.querySelectorAll('.leaflet-control-layers-base label')).map(
        (label) => label.textContent?.trim(),
      );
      const checked = element.querySelector<HTMLInputElement>(
        '.leaflet-control-layers-base input:checked',
      );

      expect(labels).toEqual([BASE_MAPS.clean.label, BASE_MAPS.osm.label]);
      expect(checked?.closest('label')?.textContent?.trim()).toBe(BASE_MAPS.clean.label);
    });

    it('should switch to the standard OpenStreetMap map from the layer control', () => {
      const osmInput = Array.from(
        element.querySelectorAll<HTMLInputElement>('.leaflet-control-layers-base input'),
      ).find((input) => input.closest('label')?.textContent?.trim() === BASE_MAPS.osm.label);

      osmInput?.click();

      expect(map.hasLayer(clean)).toBe(false);
      expect(tileLayers(map).map((layer) => layer.getTileUrl({ x: 1, y: 2 } as L.Coords))).toEqual([
        `https://tile.openstreetmap.org/${MAP_ZOOM.initial}/1/2.png`,
      ]);
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

  describe('with the clean map unavailable', () => {
    it('should fall back to the standard OpenStreetMap map', async () => {
      const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined);

      const { element, map } = await createMap(() => Promise.reject(new Error('no WebGL')));

      expect(tileLayers(map)).toHaveLength(1);
      expect(element.querySelector('.leaflet-control-layers')).toBeNull();
      expect(consoleError).toHaveBeenCalled();
      consoleError.mockRestore();
    });
  });

  describe('when destroyed before the clean map has loaded', () => {
    it('should not add any layer to the removed map', async () => {
      let resolveLayer: (layer: L.Layer) => void = () => undefined;
      const pending = new Promise<L.Layer>((resolve) => (resolveLayer = resolve));
      const clean = L.layerGroup();

      TestBed.configureTestingModule({
        imports: [LiveMap],
        providers: [{ provide: CLEAN_LAYER_FACTORY, useValue: () => pending }],
      });
      const fixture = TestBed.createComponent(LiveMap);
      fixture.detectChanges();
      const map = fixture.componentInstance['map'] as L.Map;

      fixture.destroy();
      resolveLayer(clean);
      await pending;
      await Promise.resolve();

      expect(map.hasLayer(clean)).toBe(false);
    });
  });
});
