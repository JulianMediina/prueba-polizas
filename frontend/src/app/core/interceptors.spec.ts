import { HttpErrorResponse } from '@angular/common/http';

import { mensajeDeError } from './interceptors';

describe('mensajeDeError', () => {
  it('usa el detail del ProblemDetail', () => {
    const err = new HttpErrorResponse({
      status: 409,
      error: { title: 'Conflicto', detail: 'No se puede renovar una póliza cancelada (IND-1)', codigo: 'poliza-cancelada' },
    });
    expect(mensajeDeError(err)).toBe('No se puede renovar una póliza cancelada (IND-1)');
  });

  it('lista los errores de validación por campo', () => {
    const err = new HttpErrorResponse({
      status: 400,
      error: { detail: 'La solicitud tiene campos inválidos', errores: { canonMensual: 'debe ser mayor que 0' } },
    });
    expect(mensajeDeError(err)).toBe('La solicitud tiene campos inválidos (canonMensual: debe ser mayor que 0)');
  });

  it('informa falta de conexión', () => {
    expect(mensajeDeError(new HttpErrorResponse({ status: 0 }))).toContain('No hay conexión');
  });

  it('usa un mensaje genérico para errores 5xx sin cuerpo', () => {
    expect(mensajeDeError(new HttpErrorResponse({ status: 502, error: 'Bad Gateway' }))).toBe(
      'Ocurrió un error en el servidor.',
    );
  });
});
