import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { ProblemDetail } from './api.models';
import { NotificacionService } from './notificacion.service';

/** Agrega un X-Correlation-Id por petición para poder rastrearla en los logs del backend. */
export const correlationIdInterceptor: HttpInterceptorFn = (req, next) =>
  next(req.clone({ setHeaders: { 'X-Correlation-Id': crypto.randomUUID() } }));

/** Traduce errores HTTP (ProblemDetail) a un mensaje legible y lo muestra una sola vez. */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notificaciones = inject(NotificacionService);
  return next(req).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse) {
        notificaciones.error(mensajeDeError(err));
      }
      return throwError(() => err);
    }),
  );
};

export function mensajeDeError(err: HttpErrorResponse): string {
  if (err.status === 0) {
    return 'No hay conexión con el servidor. Intente de nuevo.';
  }
  const problema = (typeof err.error === 'object' ? err.error : null) as ProblemDetail | null;
  if (problema?.errores && Object.keys(problema.errores).length > 0) {
    const campos = Object.entries(problema.errores)
      .map(([campo, msg]) => `${campo}: ${msg}`)
      .join(' · ');
    return `${problema.detail ?? 'Datos inválidos'} (${campos})`;
  }
  if (problema?.detail) {
    return problema.detail;
  }
  return err.status >= 500 ? 'Ocurrió un error en el servidor.' : `Error ${err.status}`;
}
