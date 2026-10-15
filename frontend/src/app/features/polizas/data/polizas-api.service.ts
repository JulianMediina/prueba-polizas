import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  CrearPolizaRequest,
  EstadoPoliza,
  Pagina,
  Poliza,
  Renovacion,
  Riesgo,
  RiesgoRequest,
  TipoPoliza,
} from '../../../core/api.models';

/**
 * Cliente de la API. Todas las rutas van por /api: el nginx del frontend (patrón BFF) agrega la API key en el
 * servidor, así que la key nunca llega al navegador.
 */
export const API_BASE = '/api';

export interface FiltrosPoliza {
  tipo: TipoPoliza | null;
  estado: EstadoPoliza | null;
  page: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class PolizasApiService {
  private readonly http = inject(HttpClient);

  listar(filtros: FiltrosPoliza): Observable<Pagina<Poliza>> {
    const params: Record<string, string | number> = { page: filtros.page, size: filtros.size };
    if (filtros.tipo) params['tipo'] = filtros.tipo;
    if (filtros.estado) params['estado'] = filtros.estado;
    return this.http.get<Pagina<Poliza>>(`${API_BASE}/polizas`, { params });
  }

  obtener(id: number): Observable<Poliza> {
    return this.http.get<Poliza>(`${API_BASE}/polizas/${id}`);
  }

  riesgos(id: number): Observable<Riesgo[]> {
    return this.http.get<Riesgo[]>(`${API_BASE}/polizas/${id}/riesgos`);
  }

  crear(request: CrearPolizaRequest): Observable<Poliza> {
    return this.http.post<Poliza>(`${API_BASE}/polizas`, request);
  }

  renovar(id: number): Observable<Renovacion> {
    return this.http.post<Renovacion>(`${API_BASE}/polizas/${id}/renovar`, null);
  }

  cancelar(id: number): Observable<Poliza> {
    return this.http.post<Poliza>(`${API_BASE}/polizas/${id}/cancelar`, null);
  }

  agregarRiesgo(id: number, riesgo: RiesgoRequest): Observable<Riesgo> {
    return this.http.post<Riesgo>(`${API_BASE}/polizas/${id}/riesgos`, riesgo);
  }

  cancelarRiesgo(riesgoId: number): Observable<Riesgo> {
    return this.http.post<Riesgo>(`${API_BASE}/riesgos/${riesgoId}/cancelar`, null);
  }
}
