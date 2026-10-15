import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { FormControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { RiesgoRequest } from '../../../core/api.models';

const DOCUMENTO = /^[0-9A-Za-z-]{4,20}$/;

export type PersonaForm = FormGroup<{ documento: FormControl<string>; nombre: FormControl<string> }>;

export type RiesgoForm = FormGroup<{
  direccion: FormControl<string>;
  ciudad: FormControl<string>;
  arrendatario: PersonaForm;
  arrendador: PersonaForm;
  canonMensual: FormControl<number | null>;
}>;

/** Formulario tipado de un riesgo, con las mismas reglas que valida el backend. */
export function crearRiesgoForm(fb: NonNullableFormBuilder): RiesgoForm {
  const persona = (): PersonaForm =>
    fb.group({
      documento: fb.control('', [Validators.required, Validators.pattern(DOCUMENTO)]),
      nombre: fb.control('', [Validators.required, Validators.maxLength(150)]),
    });
  return fb.group({
    direccion: fb.control('', [Validators.required, Validators.maxLength(200)]),
    ciudad: fb.control('', [Validators.required, Validators.maxLength(80)]),
    arrendatario: persona(),
    arrendador: persona(),
    canonMensual: new FormControl<number | null>(null, [Validators.required, Validators.min(1)]),
  });
}

export function aRiesgoRequest(form: RiesgoForm): RiesgoRequest {
  const v = form.getRawValue();
  return {
    direccion: v.direccion.trim(),
    ciudad: v.ciudad.trim(),
    arrendatario: { documento: v.arrendatario.documento.trim(), nombre: v.arrendatario.nombre.trim() },
    arrendador: { documento: v.arrendador.documento.trim(), nombre: v.arrendador.nombre.trim() },
    canonMensual: Number(v.canonMensual),
  };
}

/** Campos de un riesgo (inmueble, arrendatario/asegurado, arrendador/beneficiario y canon). */
@Component({
  selector: 'app-riesgo-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule],
  template: `
    <div class="grid" [formGroup]="form()">
      <mat-form-field class="span-2">
        <mat-label>Dirección del inmueble</mat-label>
        <input matInput formControlName="direccion" autocomplete="street-address" />
        <mat-error>La dirección es obligatoria</mat-error>
      </mat-form-field>
      <mat-form-field>
        <mat-label>Ciudad</mat-label>
        <input matInput formControlName="ciudad" />
        <mat-error>La ciudad es obligatoria</mat-error>
      </mat-form-field>
      <mat-form-field>
        <mat-label>Canon mensual (COP)</mat-label>
        <input matInput type="number" min="1" step="1000" formControlName="canonMensual" />
        <mat-error>Ingrese un canon mayor que cero</mat-error>
      </mat-form-field>

      <fieldset formGroupName="arrendatario">
        <legend>Arrendatario (asegurado)</legend>
        <mat-form-field>
          <mat-label>Documento</mat-label>
          <input matInput formControlName="documento" />
          <mat-error>Documento inválido</mat-error>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Nombre</mat-label>
          <input matInput formControlName="nombre" />
          <mat-error>El nombre es obligatorio</mat-error>
        </mat-form-field>
      </fieldset>

      <fieldset formGroupName="arrendador">
        <legend>Arrendador (beneficiario)</legend>
        <mat-form-field>
          <mat-label>Documento</mat-label>
          <input matInput formControlName="documento" />
          <mat-error>Documento inválido</mat-error>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Nombre</mat-label>
          <input matInput formControlName="nombre" />
          <mat-error>El nombre es obligatorio</mat-error>
        </mat-form-field>
      </fieldset>
    </div>
  `,
  styles: `
    .grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      column-gap: 16px;
    }
    .span-2 {
      grid-column: span 2;
    }
    fieldset {
      display: contents;
    }
    legend {
      grid-column: span 2;
      margin: 4px 0 8px;
      font-size: 12px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      color: var(--color-text-muted);
    }
    @media (max-width: 600px) {
      .grid {
        grid-template-columns: 1fr;
      }
      .span-2,
      legend {
        grid-column: auto;
      }
    }
  `,
})
export class RiesgoFormComponent {
  readonly form = input.required<RiesgoForm>();
}
