import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { Router } from '@angular/router';

import { ESTADOS_POLIZA, EstadoPoliza, Poliza, TIPOS_POLIZA, TipoPoliza } from '../../../core/api.models';
import { NotificacionService } from '../../../core/notificacion.service';
import { EstadoChip } from '../../../shared/estado-chip';
import { PolizasApiService } from '../data/polizas-api.service';
import { NuevaPolizaDialog } from '../nueva/nueva-poliza-dialog';

@Component({
  selector: 'app-poliza-lista',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CurrencyPipe,
    DatePipe,
    MatTableModule,
    MatPaginatorModule,
    MatSelectModule,
    MatFormFieldModule,
    MatButtonModule,
    MatIconModule,
    EstadoChip,
  ],
  templateUrl: './poliza-lista.html',
  styleUrl: './poliza-lista.scss',
})
export class PolizaLista {
  private readonly api = inject(PolizasApiService);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly notificaciones = inject(NotificacionService);

  protected readonly tipos = TIPOS_POLIZA;
  protected readonly estados = ESTADOS_POLIZA;
  protected readonly columnas = ['numero', 'tipo', 'tomador', 'vigencia', 'canon', 'prima', 'estado'];

  protected readonly tipo = signal<TipoPoliza | null>(null);
  protected readonly estado = signal<EstadoPoliza | null>(null);
  protected readonly pagina = signal(0);
  protected readonly tamano = signal(10);

  protected readonly polizas = rxResource({
    params: () => ({ tipo: this.tipo(), estado: this.estado(), page: this.pagina(), size: this.tamano() }),
    stream: ({ params }) => this.api.listar(params),
  });

  protected readonly filas = computed(() => (this.polizas.hasValue() ? this.polizas.value().contenido : []));
  protected readonly total = computed(() => (this.polizas.hasValue() ? this.polizas.value().totalElementos : 0));
  protected readonly hayFiltros = computed(() => this.tipo() !== null || this.estado() !== null);
  protected readonly esqueleto = Array.from({ length: 5 }, (_, i) => i);

  filtrarTipo(tipo: TipoPoliza | null): void {
    this.tipo.set(tipo);
    this.pagina.set(0);
  }

  filtrarEstado(estado: EstadoPoliza | null): void {
    this.estado.set(estado);
    this.pagina.set(0);
  }

  limpiarFiltros(): void {
    this.tipo.set(null);
    this.estado.set(null);
    this.pagina.set(0);
  }

  cambiarPagina(evento: PageEvent): void {
    this.pagina.set(evento.pageIndex);
    this.tamano.set(evento.pageSize);
  }

  abrir(poliza: Poliza): void {
    this.router.navigate(['/poliza', poliza.id]);
  }

  nueva(): void {
    this.dialog
      .open(NuevaPolizaDialog, { width: '760px', maxWidth: '96vw', autoFocus: 'first-tabbable' })
      .afterClosed()
      .subscribe((poliza) => {
        if (poliza) {
          this.notificaciones.exito(`Póliza ${poliza.numero} creada`);
          this.router.navigate(['/poliza', poliza.id]);
        }
      });
  }
}
