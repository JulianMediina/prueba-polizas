import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

@Injectable({ providedIn: 'root' })
export class NotificacionService {
  private readonly snackBar = inject(MatSnackBar);

  exito(mensaje: string): void {
    this.snackBar.open(mensaje, 'Cerrar', { duration: 5000, panelClass: 'snack-ok' });
  }

  error(mensaje: string): void {
    this.snackBar.open(mensaje, 'Cerrar', { duration: 8000, panelClass: 'snack-error', politeness: 'assertive' });
  }
}
