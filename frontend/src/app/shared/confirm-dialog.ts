import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

export interface ConfirmData {
  titulo: string;
  mensaje: string;
  /** Detalle del impacto de la acción (p. ej. "Se cancelarán 3 riesgos"). */
  impacto?: string[];
  accion: string;
  peligrosa?: boolean;
}

@Component({
  selector: 'app-confirm-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ data.titulo }}</h2>
    <mat-dialog-content>
      <p>{{ data.mensaje }}</p>
      @if (data.impacto?.length) {
        <ul class="impacto">
          @for (linea of data.impacto; track linea) {
            <li>{{ linea }}</li>
          }
        </ul>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">Volver</button>
      <button mat-flat-button [class.peligrosa]="data.peligrosa" [mat-dialog-close]="true" cdkFocusInitial>
        {{ data.accion }}
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .impacto {
      margin: 0;
      padding-left: 20px;
      color: var(--color-text-muted);
    }
    .peligrosa {
      --mat-button-filled-container-color: var(--color-error);
    }
  `,
})
export class ConfirmDialog {
  protected readonly data = inject<ConfirmData>(MAT_DIALOG_DATA);
}
