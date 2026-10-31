import { TestBed } from '@angular/core/testing';
import { LiveMap } from './live-map';

describe('LiveMap', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LiveMap],
    }).compileComponents();
  });

  it('should render a map without markers', async () => {
    const fixture = TestBed.createComponent(LiveMap);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('.leaflet-container')).toBeTruthy();
    expect(element.querySelector('.leaflet-marker-icon')).toBeNull();
  });
});
