package com.aseguradora.polizas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.aseguradora.polizas.domain.DatosRiesgo;
import com.aseguradora.polizas.domain.EstadoRiesgo;
import com.aseguradora.polizas.domain.Poliza;
import com.aseguradora.polizas.domain.RecursoNoEncontradoException;
import com.aseguradora.polizas.domain.TipoPoliza;
import com.aseguradora.polizas.domain.Tomador;
import com.aseguradora.polizas.repository.PolizaRepository;
import com.aseguradora.polizas.repository.RiesgoRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class RiesgoServiceTest {

	@Mock
	RiesgoRepository riesgos;

	@Mock
	PolizaRepository polizas;

	@Mock
	ApplicationEventPublisher eventos;

	final Clock clock = Clock.fixed(Instant.parse("2026-04-01T15:00:00Z"), ZoneOffset.UTC);

	@Test
	void cancelaPasandoPorElAgregado() {
		var datos = new DatosRiesgo("Calle 1", "Cali", "1", "A", "2", "B", new BigDecimal("1000000"));
		var poliza = Poliza.crear("COL-1", TipoPoliza.COLECTIVA, new Tomador("NIT", "900", "Inmo", null, null),
				LocalDate.of(2026, 1, 1), 12, List.of(datos, datos));
		var riesgo = poliza.getRiesgos().getFirst();
		when(riesgos.findById(5L)).thenReturn(Optional.of(riesgo));
		when(polizas.findWithRiesgosById(null)).thenReturn(Optional.of(poliza));
		var service = new RiesgoService(riesgos, polizas, eventos, clock);

		var cancelado = service.cancelar(5L);

		assertThat(cancelado.getEstado()).isEqualTo(EstadoRiesgo.CANCELADO);
		assertThat(cancelado.getFechaCancelacion()).isEqualTo(LocalDate.of(2026, 4, 1));
		assertThat(poliza.getCanonMensual()).isEqualByComparingTo("1000000");
		verify(eventos).publishEvent(org.mockito.ArgumentMatchers.any(Object.class));
	}

	@Test
	void riesgoInexistenteEs404() {
		when(riesgos.findById(5L)).thenReturn(Optional.empty());
		var service = new RiesgoService(riesgos, polizas, eventos, clock);

		assertThatThrownBy(() -> service.cancelar(5L)).isInstanceOf(RecursoNoEncontradoException.class);
		verifyNoInteractions(eventos);
	}
}
