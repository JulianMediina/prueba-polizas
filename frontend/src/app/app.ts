import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, MatToolbarModule, MatIconModule],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {}
