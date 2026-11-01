import { Component } from '@angular/core';
import { LiveMap } from './live-map/live-map';

@Component({
  imports: [LiveMap],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {}
