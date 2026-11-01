import { AfterViewInit, Component, ElementRef, OnDestroy, viewChild } from '@angular/core';
import * as L from 'leaflet';

const GDANSK_CENTER: L.LatLngTuple = [54.352, 18.6466];
const GDANSK_ZOOM = 12;

@Component({
  selector: 'app-live-map',
  templateUrl: './live-map.html',
  styleUrl: './live-map.css',
})
export class LiveMap implements AfterViewInit, OnDestroy {
  private readonly container = viewChild.required<ElementRef<HTMLElement>>('map');
  private map?: L.Map;

  ngAfterViewInit(): void {
    this.map = L.map(this.container().nativeElement).setView(GDANSK_CENTER, GDANSK_ZOOM);

    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    }).addTo(this.map);
  }

  ngOnDestroy(): void {
    this.map?.remove();
  }
}
