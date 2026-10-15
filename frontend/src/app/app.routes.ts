import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    title: 'Pólizas · Gestión de pólizas',
    loadComponent: () => import('./features/polizas/lista/poliza-lista').then((m) => m.PolizaLista),
  },
  {
    // Ruta en singular para no chocar con la API directa (/polizas), que nginx enruta al backend.
    path: 'poliza/:id',
    title: 'Detalle de póliza · Gestión de pólizas',
    loadComponent: () => import('./features/polizas/detalle/poliza-detalle').then((m) => m.PolizaDetalle),
  },
  { path: '**', redirectTo: '' },
];
