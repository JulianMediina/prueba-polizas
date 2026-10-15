import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { Riesgo } from '../../../core/api.models';
import { PolizasApiService } from '../data/polizas-api.service';
import { RiesgoFormComponent, aRiesgoRequest, crearRiesgoForm } from './riesgo-form';

export interface RiesgoDialogData {
  polizaId: number;
  numero: string;
}

/** Diálogo para agregar un riesgo a una póliza colectiva. Devuelve el riesgo creado. */
@Component({
  selector: 'app-riesgo-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatDialogModule, MatButtonModule, MatProgressBarModule, RiesgoFormComponent],
  template: `
    <h2 mat-dialog-title>Agregar riesgo a <span class="mono">{{ data.numero }}</span></h2>
    <mat-dialog-content>
      <app-riesgo-form [form]="form" />
    </mat-dialog-content>
    @if (guardando()) {
      <mat-progress-bar mode="indeterminate" />
    }
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close [disabled]="guardando()">Cancelar</button>
      <button mat-flat-button (click)="guardar()" [disabled]="guardando()">Agregar riesgo</button>
    </mat-dialog-actions>
  `,
})
export class RiesgoDialog {
  protected readonly data = inject<RiesgoDialogData>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<RiesgoDialog, Riesgo>>(MatDialogRef);
  private readonly api = inject(PolizasApiService);
  protected readonly form = crearRiesgoForm(inject(NonNullableFormBuilder));
  protected readonly guardando = signal(false);

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.api.agregarRiesgo(this.data.polizaId, aRiesgoRequest(this.form)).subscribe({
      next: (riesgo) => this.ref.close(riesgo),
      error: () => this.guardando.set(false),
    });
  }
}
