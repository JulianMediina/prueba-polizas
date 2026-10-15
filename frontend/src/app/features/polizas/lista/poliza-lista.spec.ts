import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localeEsCo from '@angular/common/locales/es-CO';
import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Poliza } from '../../../core/api.models';
import { PolizaLista } from './poliza-lista';

registerLocaleData(localeEsCo);

const POLIZA: Poliza = {
  id: 5,
  numero: 'COL-0000005',
  tipo: 'COLECTIVA',
  estado: 'ACTIVA',
  tomador: { tipoDocumento: 'NIT', documento: '900123456', nombre: 'Inmobiliaria Andina S.A.S.', email: null, telefono: null },
  fechaInicio: '2025-12-01',
  fechaFin: '2026-12-01',
  mesesVigencia: 12,
  canonMensual: 6250000,
  prima: 75000000,
  fechaCancelacion: null,
};

describe('PolizaLista', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: LOCALE_ID, useValue: 'es-CO' },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  async function cargar(contenido: Poliza[]) {
    const fixture = TestBed.createComponent(PolizaLista);
    fixture.detectChanges();
    TestBed.tick(); // ejecuta el efecto del resource que dispara la petición
    http
      .expectOne((r) => r.url === '/api/polizas')
      .flush({ contenido, pagina: 0, tamano: 10, totalElementos: contenido.length, totalPaginas: 1 });
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('muestra las pólizas con número, tomador, montos en COP y estado', async () => {
    const el = await cargar([POLIZA]);

    const fila = el.querySelector('tr.mat-mdc-row');
    expect(fila?.textContent).toContain('COL-0000005');
    expect(fila?.textContent).toContain('Inmobiliaria Andina S.A.S.');
    expect(fila?.textContent).toContain('6.250.000');
    expect(fila?.textContent).toContain('Activa');
    expect(el.textContent).toContain('1 póliza');
  });

  it('muestra un estado vacío cuando no hay resultados', async () => {
    const el = await cargar([]);

    expect(el.textContent).toContain('Aún no hay pólizas registradas.');
    expect(el.querySelector('table')).toBeNull();
  });
});
