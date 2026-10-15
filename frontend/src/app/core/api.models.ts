/** Contratos de la API de pólizas (espejo de los DTO del backend). */

export type TipoPoliza = 'INDIVIDUAL' | 'COLECTIVA';
export type EstadoPoliza = 'ACTIVA' | 'RENOVADA' | 'CANCELADA';
export type EstadoRiesgo = 'ACTIVO' | 'CANCELADO';
export type TipoDocumento = 'CC' | 'CE' | 'NIT' | 'PA' | 'TI';

export const TIPOS_POLIZA: readonly TipoPoliza[] = ['INDIVIDUAL', 'COLECTIVA'];
export const ESTADOS_POLIZA: readonly EstadoPoliza[] = ['ACTIVA', 'RENOVADA', 'CANCELADA'];
export const TIPOS_DOCUMENTO: readonly TipoDocumento[] = ['CC', 'CE', 'NIT', 'PA', 'TI'];

export interface Tomador {
  tipoDocumento: TipoDocumento;
  documento: string;
  nombre: string;
  email: string | null;
  telefono: string | null;
}

export interface Persona {
  documento: string;
  nombre: string;
}

export interface Poliza {
  id: number;
  numero: string;
  tipo: TipoPoliza;
  estado: EstadoPoliza;
  tomador: Tomador;
  fechaInicio: string;
  fechaFin: string;
  mesesVigencia: number;
  canonMensual: number;
  prima: number;
  fechaCancelacion: string | null;
}

export interface Riesgo {
  id: number;
  polizaId: number;
  direccion: string;
  ciudad: string;
  arrendatario: Persona;
  arrendador: Persona;
  canonMensual: number;
  estado: EstadoRiesgo;
  fechaInclusion: string;
  fechaCancelacion: string | null;
}

export interface Renovacion {
  poliza: Poliza;
  anioIpc: number;
  ipcAplicado: number;
  canonAnterior: number;
  canonNuevo: number;
  primaAnterior: number;
  primaNueva: number;
  fechaInicioAnterior: string;
  fechaFinAnterior: string;
}

export interface Pagina<T> {
  contenido: T[];
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
}

export interface RiesgoRequest {
  direccion: string;
  ciudad: string;
  arrendatario: Persona;
  arrendador: Persona;
  canonMensual: number;
}

export interface CrearPolizaRequest {
  tipo: TipoPoliza;
  tomador: Omit<Tomador, 'email' | 'telefono'> & { email?: string | null; telefono?: string | null };
  fechaInicio: string;
  mesesVigencia: number;
  riesgos: RiesgoRequest[];
}

/** RFC 9457 + extensiones del backend. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  codigo?: string;
  errores?: Record<string, string>;
}
