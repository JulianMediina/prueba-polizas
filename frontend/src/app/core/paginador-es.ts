import { Injectable } from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';

@Injectable()
export class PaginadorEs extends MatPaginatorIntl {
  override itemsPerPageLabel = 'Por página';
  override nextPageLabel = 'Página siguiente';
  override previousPageLabel = 'Página anterior';
  override firstPageLabel = 'Primera página';
  override lastPageLabel = 'Última página';

  override getRangeLabel = (page: number, pageSize: number, length: number): string => {
    if (length === 0) return `0 de 0`;
    const inicio = page * pageSize + 1;
    const fin = Math.min(length, (page + 1) * pageSize);
    return `${inicio} – ${fin} de ${length}`;
  };
}
