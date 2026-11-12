import {
  AfterViewInit,
  Component,
  ElementRef,
  inject,
  OnDestroy,
  PendingTasks,
  viewChild,
} from '@angular/core';
import * as L from 'leaflet';
import { CLEAN_LAYER_FACTORY, createOsmLayer } from './base-layers';
import { BASE_MAPS, MAP_ZOOM, TRICITY_BOUNDS, TRICITY_CENTER } from './map-config';

@Component({
  selector: 'app-live-map',
  templateUrl: './live-map.html',
  styleUrl: './live-map.css',
})
export class LiveMap implements AfterViewInit, OnDestroy {
  private readonly container = viewChild.required<ElementRef<HTMLElement>>('map');
  private readonly createCleanLayer = inject(CLEAN_LAYER_FACTORY);
  private readonly pendingTasks = inject(PendingTasks);
  private map?: L.Map;

  ngAfterViewInit(): void {
    const map = L.map(this.container().nativeElement, {
      maxBounds: TRICITY_BOUNDS,
      maxBoundsViscosity: 1,
      minZoom: MAP_ZOOM.min,
      maxZoom: MAP_ZOOM.max,
    }).setView(TRICITY_CENTER, MAP_ZOOM.initial);
    this.map = map;

    void this.pendingTasks.run(() => this.addBaseLayers(map));
  }

  ngOnDestroy(): void {
    this.map?.remove();
    this.map = undefined;
  }

  // The clean map is the default. The standard OpenStreetMap map is the second choice in the
  // layer control, and the fallback when the clean map cannot be created (no network, no WebGL).
  private async addBaseLayers(map: L.Map): Promise<void> {
    const osm = createOsmLayer();
    let clean: L.Layer;

    try {
      clean = await this.createCleanLayer();
    } catch (error) {
      console.error('The clean map is not available, falling back to OpenStreetMap.', error);
      if (this.map === map) {
        osm.addTo(map);
      }
      return;
    }

    if (this.map !== map) {
      return; // the component was destroyed while the layer was loading
    }
    clean.addTo(map);
    L.control.layers({ [BASE_MAPS.clean.label]: clean, [BASE_MAPS.osm.label]: osm }).addTo(map);
  }
}
