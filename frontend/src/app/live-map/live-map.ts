import { AfterViewInit, Component, ElementRef, OnDestroy, viewChild } from '@angular/core';
import * as L from 'leaflet';
import { MAP_ZOOM, TRICITY_BOUNDS, TRICITY_CENTER } from './map-config';

@Component({
  selector: 'app-live-map',
  templateUrl: './live-map.html',
  styleUrl: './live-map.css',
})
export class LiveMap implements AfterViewInit, OnDestroy {
  private readonly container = viewChild.required<ElementRef<HTMLElement>>('map');
  private map?: L.Map;

  ngAfterViewInit(): void {
    this.map = L.map(this.container().nativeElement, {
      maxBounds: TRICITY_BOUNDS,
      maxBoundsViscosity: 1,
      minZoom: MAP_ZOOM.min,
      maxZoom: MAP_ZOOM.max,
    }).setView(TRICITY_CENTER, MAP_ZOOM.initial);

    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      bounds: TRICITY_BOUNDS,
      minZoom: MAP_ZOOM.min,
      maxZoom: MAP_ZOOM.max,
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    }).addTo(this.map);
  }

  ngOnDestroy(): void {
    this.map?.remove();
  }
}
