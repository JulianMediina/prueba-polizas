import { TestBed } from '@angular/core/testing';

import { EstadoChip } from './estado-chip';

describe('EstadoChip', () => {
  function render(estado: string): HTMLElement {
    const fixture = TestBed.createComponent(EstadoChip);
    fixture.componentRef.setInput('estado', estado);
    fixture.detectChanges();
    return fixture.nativeElement.querySelector('span');
  }

  it('muestra la etiqueta y la clase según el estado', () => {
    const activa = render('ACTIVA');
    expect(activa.textContent?.trim()).toBe('Activa');
    expect(activa.className).toContain('chip--activa');

    expect(render('RENOVADA').className).toContain('chip--renovada');
    expect(render('CANCELADO').textContent?.trim()).toBe('Cancelado');
  });
});
