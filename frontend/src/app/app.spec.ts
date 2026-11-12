import { TestBed } from '@angular/core/testing';
import * as L from 'leaflet';
import { App } from './app';
import { CLEAN_LAYER_FACTORY } from './live-map/base-layers';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        { provide: CLEAN_LAYER_FACTORY, useValue: () => Promise.resolve(L.layerGroup()) },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render the live map', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-live-map')).toBeTruthy();
  });
});
