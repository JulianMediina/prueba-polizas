import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { EstadoPoliza, EstadoRiesgo } from '../core/api.models';

const ETIQUETAS: Record<EstadoPoliza | EstadoRiesgo, string> = {
  ACTIVA: 'Activa',
  RENOVADA: 'Renovada',
  CANCELADA: 'Cancelada',
  ACTIVO: 'Activo',
  CANCELADO: 'Cancelado',
};

/** Chip de estado con color semántico; el texto siempre acompaña al color (accesible sin depender del color). */
@Component({
  selector: 'app-estado-chip',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span [class]="'chip chip--' + clase()"><span class="dot" aria-hidden="true"></span>{{ etiqueta() }}</span>`,
  styles: `
    .chip {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 2px 10px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 600;
      line-height: 20px;
      white-space: nowrap;
    }
    .dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: currentColor;
    }
    .chip--activa {
      background: #e3f2f1;
      color: #145c59;
    }
    .chip--renovada {
      background: #fdf0dc;
      color: #8a5300;
    }
    .chip--cancelada {
      background: #f1f3f5;
      color: #52606d;
    }
  `,
})
export class EstadoChip {
  readonly estado = input.required<EstadoPoliza | EstadoRiesgo>();

  protected readonly etiqueta = computed(() => ETIQUETAS[this.estado()]);
  protected readonly clase = computed(() => {
    const e = this.estado();
    if (e === 'ACTIVA' || e === 'ACTIVO') return 'activa';
    if (e === 'RENOVADA') return 'renovada';
    return 'cancelada';
  });
}
