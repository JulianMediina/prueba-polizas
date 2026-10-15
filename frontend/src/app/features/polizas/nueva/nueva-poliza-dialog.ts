import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormArray, FormControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { startWith } from 'rxjs';

import { Poliza, TIPOS_DOCUMENTO, TipoDocumento, TipoPoliza } from '../../../core/api.models';
import { PolizasApiService } from '../data/polizas-api.service';
import { RiesgoForm, RiesgoFormComponent, aRiesgoRequest, crearRiesgoForm } from '../riesgo-form/riesgo-form';

/** Alta de póliza. Individual: exactamente un riesgo. Colectiva: uno o más. */
@Component({
  selector: 'app-nueva-poliza-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    CurrencyPipe,
    MatDialogModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    MatProgressBarModule,
    RiesgoFormComponent,
  ],
  templateUrl: './nueva-poliza-dialog.html',
  styleUrl: './nueva-poliza-dialog.scss',
})
export class NuevaPolizaDialog {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly api = inject(PolizasApiService);
  private readonly ref = inject<MatDialogRef<NuevaPolizaDialog, Poliza>>(MatDialogRef);

  protected readonly tiposDocumento = TIPOS_DOCUMENTO;
  protected readonly guardando = signal(false);

  protected readonly form = this.fb.group({
    tipo: this.fb.control<TipoPoliza>('INDIVIDUAL'),
    tomador: this.fb.group({
      tipoDocumento: this.fb.control<TipoDocumento>('CC', Validators.required),
      documento: this.fb.control('', [Validators.required, Validators.pattern(/^[0-9A-Za-z-]{4,20}$/)]),
      nombre: this.fb.control('', [Validators.required, Validators.maxLength(150)]),
      email: this.fb.control('', [Validators.email]),
      telefono: this.fb.control('', [Validators.pattern(/^[0-9+ ]{7,20}$/)]),
    }),
    fechaInicio: this.fb.control(new Date().toISOString().slice(0, 10), Validators.required),
    mesesVigencia: new FormControl<number>(12, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1), Validators.max(60)],
    }),
    riesgos: new FormArray<RiesgoForm>([crearRiesgoForm(this.fb)]),
  });

  private readonly valores = toSignal(this.form.valueChanges.pipe(startWith(this.form.getRawValue())), {
    requireSync: true,
  });

  protected readonly esColectiva = computed(() => this.valores().tipo === 'COLECTIVA');

  /** Vista previa de los valores que calculará el backend. */
  protected readonly canonTotal = computed(() =>
    (this.valores().riesgos ?? []).reduce((suma, r) => suma + (Number(r?.canonMensual) || 0), 0),
  );
  protected readonly primaEstimada = computed(() => this.canonTotal() * (Number(this.valores().mesesVigencia) || 0));

  get riesgos(): FormArray<RiesgoForm> {
    return this.form.controls.riesgos;
  }

  cambiarTipo(tipo: TipoPoliza): void {
    this.form.controls.tipo.setValue(tipo);
    if (tipo === 'INDIVIDUAL') {
      while (this.riesgos.length > 1) this.riesgos.removeAt(this.riesgos.length - 1);
      this.form.controls.tomador.controls.tipoDocumento.setValue('CC');
    } else {
      this.form.controls.tomador.controls.tipoDocumento.setValue('NIT');
    }
  }

  agregarRiesgo(): void {
    this.riesgos.push(crearRiesgoForm(this.fb));
  }

  quitarRiesgo(indice: number): void {
    if (this.riesgos.length > 1) this.riesgos.removeAt(indice);
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.guardando.set(true);
    this.api
      .crear({
        tipo: v.tipo,
        tomador: {
          tipoDocumento: v.tomador.tipoDocumento,
          documento: v.tomador.documento.trim(),
          nombre: v.tomador.nombre.trim(),
          email: v.tomador.email.trim() || null,
          telefono: v.tomador.telefono.trim() || null,
        },
        fechaInicio: v.fechaInicio,
        mesesVigencia: v.mesesVigencia,
        riesgos: this.riesgos.controls.map(aRiesgoRequest),
      })
      .subscribe({
        next: (poliza) => this.ref.close(poliza),
        error: () => this.guardando.set(false),
      });
  }
}
