import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PolizasApiService } from './polizas-api.service';

describe('PolizasApiService', () => {
  let api: PolizasApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(PolizasApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista enviando solo los filtros presentes', () => {
    api.listar({ tipo: 'COLECTIVA', estado: null, page: 2, size: 10 }).subscribe();

    const req = http.expectOne((r) => r.url === '/api/polizas');
    expect(req.request.params.get('tipo')).toBe('COLECTIVA');
    expect(req.request.params.has('estado')).toBe(false);
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('10');
    req.flush({ contenido: [], pagina: 2, tamano: 10, totalElementos: 0, totalPaginas: 0 });
  });

  it('usa las rutas del contrato para las acciones', () => {
    api.renovar(7).subscribe();
    api.cancelar(7).subscribe();
    api.cancelarRiesgo(9).subscribe();
    api.agregarRiesgo(7, {
      direccion: 'Calle 1',
      ciudad: 'Bogotá',
      arrendatario: { documento: '123456', nombre: 'A' },
      arrendador: { documento: '654321', nombre: 'B' },
      canonMensual: 1000000,
    }).subscribe();

    expect(http.expectOne('/api/polizas/7/renovar').request.method).toBe('POST');
    expect(http.expectOne('/api/polizas/7/cancelar').request.method).toBe('POST');
    expect(http.expectOne('/api/riesgos/9/cancelar').request.method).toBe('POST');
    const agregar = http.expectOne('/api/polizas/7/riesgos');
    expect(agregar.request.method).toBe('POST');
    expect(agregar.request.body.canonMensual).toBe(1000000);
  });

  it('nunca envía la API key desde el navegador', () => {
    api.obtener(1).subscribe();
    const req = http.expectOne('/api/polizas/1');
    expect(req.request.headers.has('x-api-key')).toBe(false);
    req.flush({});
  });
});
