import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TRACKING_API } from './api-config';
import { Vehicle } from './vehicle';
import { VehiclesApi } from './vehicles-api';

const VEHICLES: Vehicle[] = [
  {
    vehicleId: 145989,
    vehicleCode: '2810',
    line: '123',
    headsign: 'Brama Wyżynna',
    lat: 54.334,
    lon: 18.655,
    direction: 270,
    speed: 20,
    delaySeconds: 35,
    readAt: '2026-11-08T17:16:32Z',
  },
  {
    vehicleId: 146002,
    vehicleCode: '1042',
    line: '8',
    headsign: 'Jelitkowo',
    lat: 54.402,
    lon: 18.571,
    direction: 90,
    speed: 0,
    delaySeconds: -60,
    readAt: '2026-11-08T17:16:30Z',
  },
];

describe('VehiclesApi', () => {
  let api: VehiclesApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(VehiclesApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('should GET the vehicles from the tracking api', () => {
    api.getVehicles().subscribe();

    const request = http.expectOne(TRACKING_API.vehicles);
    expect(request.request.method).toBe('GET');
    expect(request.request.url).toBe('/api/tracking/vehicles');
    request.flush(VEHICLES);
  });

  it('should return the vehicles as received', () => {
    let result: Vehicle[] | undefined;
    api.getVehicles().subscribe((vehicles) => (result = vehicles));

    http.expectOne(TRACKING_API.vehicles).flush(VEHICLES);

    expect(result).toEqual(VEHICLES);
  });

  it('should return an empty list when no vehicles are tracked', () => {
    let result: Vehicle[] | undefined;
    api.getVehicles().subscribe((vehicles) => (result = vehicles));

    http.expectOne(TRACKING_API.vehicles).flush([]);

    expect(result).toEqual([]);
  });

  it('should pass a server error to the subscriber', () => {
    let error: HttpErrorResponse | undefined;
    api.getVehicles().subscribe({ error: (e: HttpErrorResponse) => (error = e) });

    http
      .expectOne(TRACKING_API.vehicles)
      .flush('boom', { status: 500, statusText: 'Internal Server Error' });

    expect(error?.status).toBe(500);
  });

  it('should not send a request until subscribed', () => {
    api.getVehicles();

    http.expectNone(TRACKING_API.vehicles);
  });
});
