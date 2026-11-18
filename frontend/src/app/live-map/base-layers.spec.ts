import { getWorkerUrl } from 'maplibre-gl';
import { createCleanLayer, createOsmLayer } from './base-layers';
import { BASE_MAPS, HIDDEN_STYLE_LAYERS, MAPLIBRE_WORKER_PATH } from './map-config';

describe('base layers', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  describe('createOsmLayer', () => {
    it('should use the configured source and credit OpenStreetMap', () => {
      const layer = createOsmLayer();

      expect(layer.options.attribution).toBe(BASE_MAPS.osm.attribution);
      expect(layer.getTileUrl({ x: 1, y: 2, z: 3 } as never)).toContain('tile.openstreetmap.org');
    });
  });

  describe('createCleanLayer', () => {
    const style = {
      version: 8,
      sources: {},
      layers: [
        { id: 'background', type: 'background' },
        { id: 'building', type: 'fill' },
        { id: 'highway-name-major', type: 'symbol' },
      ],
    };

    function stubWebGl(supported: boolean) {
      vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(
        supported ? ({} as RenderingContext) : null,
      );
    }

    it('should fail without downloading the style when WebGL is not supported', async () => {
      stubWebGl(false);
      const fetchMock = vi.fn();
      vi.stubGlobal('fetch', fetchMock);

      await expect(createCleanLayer()).rejects.toThrow(/WebGL/);
      expect(fetchMock).not.toHaveBeenCalled();
    });

    it('should fail when the style cannot be downloaded', async () => {
      stubWebGl(true);
      vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 503 }));

      await expect(createCleanLayer()).rejects.toThrow(/503/);
    });

    it('should build a layer from the cleaned style with the configured attribution', async () => {
      stubWebGl(true);
      const fetchMock = vi.fn().mockResolvedValue({ ok: true, json: () => Promise.resolve(style) });
      vi.stubGlobal('fetch', fetchMock);

      const layer = await createCleanLayer();
      const options = (layer as unknown as { options: Record<string, unknown> }).options;

      expect(fetchMock).toHaveBeenCalledWith(BASE_MAPS.clean.styleUrl);
      expect(options['attributionControl']).toEqual({
        customAttribution: BASE_MAPS.clean.attribution,
      });
      const layerIds = (options['style'] as typeof style).layers.map((l) => l.id);
      expect(layerIds).toEqual(['background', 'highway-name-major']);
      for (const hidden of HIDDEN_STYLE_LAYERS) {
        expect(layerIds).not.toContain(hidden);
      }
    });

    it('should point MapLibre at the worker copied by angular.json', async () => {
      stubWebGl(true);
      vi.stubGlobal(
        'fetch',
        vi.fn().mockResolvedValue({ ok: true, json: () => Promise.resolve(style) }),
      );

      await createCleanLayer();

      expect(getWorkerUrl()).toContain(`${MAPLIBRE_WORKER_PATH}?v=`);
    });
  });
});
