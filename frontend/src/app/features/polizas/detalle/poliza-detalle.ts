import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, numberAttribute, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { Observable, filter, switchMap } from 'rxjs';

import { Renovacion, Riesgo } from '../../../core/api.models';
import { NotificacionService } from '../../../core/notificacion.service';
import { ConfirmData, ConfirmDialog } from '../../../shared/confirm-dialog';
import { EstadoChip } from '../../../shared/estado-chip';
import { PolizasApiService } from '../data/polizas-api.service';
import { RiesgoDialog, RiesgoDialogData } from '../riesgo-form/riesgo-dialog';

@Component({
  selector: 'app-poliza-detalle',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    MatProgressBarModule,
    EstadoChip,
  ],
  templateUrl: './poliza-detalle.html',
  styleUrl: './poliza-detalle.scss',
})
export class PolizaDetalle {
  private readonly api = inject(PolizasApiService);
  private readonly dialog = inject(MatDialog);
  private readonly notificaciones = inject(NotificacionService);

  /** Viene del parámetro de ruta :id (withComponentInputBinding). */
  readonly id = input.required({ transform: numberAttribute });

  protected readonly procesando = signal(false);
  protected readonly ultimaRenovacion = signal<Renovacion | null>(null);
  protected readonly columnas = ['inmueble', 'arrendatario', 'arrendador', 'canon', 'inclusion', 'estado', 'acciones'];

  protected readonly poliza = rxResource({
    params: () => ({ id: this.id() }),
    stream: ({ params }) => this.api.obtener(params.id),
  });

  protected readonly riesgos = rxResource({
    params: () => ({ id: this.id() }),
    stream: ({ params }) => this.api.riesgos(params.id),
  });

  protected readonly p = computed(() => (this.poliza.hasValue() ? this.poliza.value() : null));
  protected readonly listaRiesgos = computed(() => (this.riesgos.hasValue() ? this.riesgos.value() : []));
  protected readonly activos = computed(() => this.listaRiesgos().filter((r) => r.estado === 'ACTIVO'));
  protected readonly cancelada = computed(() => this.p()?.estado === 'CANCELADA');
  protected readonly esColectiva = computed(() => this.p()?.tipo === 'COLECTIVA');
  /** Año del IPC que aplicará el backend: el anterior al inicio de la nueva vigencia. */
  protected readonly anioIpc = computed(() => {
    const fin = this.p()?.fechaFin;
    return fin ? Number(fin.slice(0, 4)) - 1 : null;
  });

  puedeCancelarRiesgo(riesgo: Riesgo): boolean {
    return this.esColectiva() && !this.cancelada() && riesgo.estado === 'ACTIVO' && this.activos().length > 1;
  }

  motivoNoCancelable(riesgo: Riesgo): string {
    if (riesgo.estado !== 'ACTIVO') return 'El riesgo ya está cancelado';
    if (!this.esColectiva()) return 'El riesgo de una póliza individual se cancela cancelando la póliza';
    if (this.activos().length <= 1) return 'Una colectiva debe conservar al menos un riesgo activo';
    return '';
  }

  renovar(): void {
    const p = this.p();
    if (!p) return;
    this.confirmarYEjecutar(
      {
        titulo: `Renovar póliza ${p.numero}`,
        mensaje: `Se renovará por ${p.mesesVigencia} meses a partir del ${this.fecha(p.fechaFin)}.`,
        impacto: [
          `El canon de cada riesgo activo se incrementa con el IPC de ${this.anioIpc()}.`,
          'La prima se recalcula como canon × meses de vigencia.',
          'El estado pasa a RENOVADA.',
        ],
        accion: 'Renovar',
      },
      () => this.api.renovar(p.id),
      (r: Renovacion) => {
        this.ultimaRenovacion.set(r);
        this.notificaciones.exito(`Póliza renovada con IPC ${r.anioIpc} (${r.ipcAplicado} %)`);
      },
    );
  }

  cancelar(): void {
    const p = this.p();
    if (!p) return;
    const n = this.activos().length;
    this.confirmarYEjecutar(
      {
        titulo: `Cancelar póliza ${p.numero}`,
        mensaje: 'Esta acción no se puede deshacer.',
        impacto: [`Se cancelarán ${n} ${n === 1 ? 'riesgo activo' : 'riesgos activos'}.`, 'La póliza no podrá renovarse.'],
        accion: 'Cancelar póliza',
        peligrosa: true,
      },
      () => this.api.cancelar(p.id),
      () => this.notificaciones.exito(`Póliza ${p.numero} cancelada`),
    );
  }

  agregarRiesgo(): void {
    const p = this.p();
    if (!p) return;
    this.dialog
      .open<RiesgoDialog, RiesgoDialogData, Riesgo>(RiesgoDialog, {
        data: { polizaId: p.id, numero: p.numero },
        width: '640px',
        maxWidth: '96vw',
      })
      .afterClosed()
      .pipe(filter((r): r is Riesgo => !!r))
      .subscribe(() => {
        this.notificaciones.exito('Riesgo agregado; canon y prima recalculados');
        this.recargar();
      });
  }

  cancelarRiesgo(riesgo: Riesgo): void {
    this.confirmarYEjecutar(
      {
        titulo: 'Cancelar riesgo',
        mensaje: `${riesgo.direccion}, ${riesgo.ciudad}`,
        impacto: ['El canon y la prima de la póliza se recalculan sin este riesgo.'],
        accion: 'Cancelar riesgo',
        peligrosa: true,
      },
      () => this.api.cancelarRiesgo(riesgo.id),
      () => this.notificaciones.exito('Riesgo cancelado'),
    );
  }

  private confirmarYEjecutar<T>(datos: ConfirmData, accion: () => Observable<T>, alTerminar: (r: T) => void): void {
    this.dialog
      .open<ConfirmDialog, ConfirmData, boolean>(ConfirmDialog, { data: datos, width: '480px', maxWidth: '96vw' })
      .afterClosed()
      .pipe(
        filter((ok) => ok === true),
        switchMap(() => {
          this.procesando.set(true);
          return accion();
        }),
      )
      .subscribe({
        next: (r) => {
          this.procesando.set(false);
          alTerminar(r);
          this.recargar();
        },
        error: () => {
          this.procesando.set(false);
          this.recargar();
        },
      });
  }

  private recargar(): void {
    this.poliza.reload();
    this.riesgos.reload();
  }

  private fecha(iso: string): string {
    const [a, m, d] = iso.split('-');
    return `${d}/${m}/${a}`;
  }
}
