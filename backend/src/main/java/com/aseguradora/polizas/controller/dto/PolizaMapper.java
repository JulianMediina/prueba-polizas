package com.aseguradora.polizas.controller.dto;

import java.util.function.Function;

import com.aseguradora.polizas.controller.dto.PolizaDtos.CrearPolizaRequest;
import com.aseguradora.polizas.controller.dto.PolizaDtos.PaginaResponse;
import com.aseguradora.polizas.controller.dto.PolizaDtos.PersonaResponse;
import com.aseguradora.polizas.controller.dto.PolizaDtos.PolizaResponse;
import com.aseguradora.polizas.controller.dto.PolizaDtos.RenovacionResponse;
import com.aseguradora.polizas.controller.dto.PolizaDtos.RiesgoRequest;
import com.aseguradora.polizas.controller.dto.PolizaDtos.RiesgoResponse;
import com.aseguradora.polizas.controller.dto.PolizaDtos.TomadorResponse;
import com.aseguradora.polizas.domain.DatosRiesgo;
import com.aseguradora.polizas.domain.Poliza;
import com.aseguradora.polizas.domain.Renovacion;
import com.aseguradora.polizas.domain.Riesgo;
import com.aseguradora.polizas.domain.Tomador;
import com.aseguradora.polizas.service.CrearPolizaCommand;

import org.springframework.data.domain.Page;

public final class PolizaMapper {

	private PolizaMapper() {
	}

	public static CrearPolizaCommand aComando(CrearPolizaRequest req) {
		var t = req.tomador();
		var tomador = new Tomador(t.tipoDocumento(), t.documento().trim(), t.nombre().trim(), t.email(), t.telefono());
		return new CrearPolizaCommand(req.tipo(), tomador, req.fechaInicio(), req.mesesVigencia(),
				req.riesgos().stream().map(PolizaMapper::aDatos).toList());
	}

	public static DatosRiesgo aDatos(RiesgoRequest r) {
		return new DatosRiesgo(r.direccion().trim(), r.ciudad().trim(), r.arrendatario().documento().trim(),
				r.arrendatario().nombre().trim(), r.arrendador().documento().trim(), r.arrendador().nombre().trim(),
				r.canonMensual());
	}

	public static PolizaResponse aResponse(Poliza p) {
		var t = p.getTomador();
		return new PolizaResponse(p.getId(), p.getNumero(), p.getTipo(), p.getEstado(),
				new TomadorResponse(t.tipoDocumento(), t.documento(), t.nombre(), t.email(), t.telefono()),
				p.getFechaInicio(), p.getFechaFin(), p.getMesesVigencia(), p.getCanonMensual(), p.getPrima(),
				p.getFechaCancelacion());
	}

	public static RiesgoResponse aResponse(Riesgo r) {
		return new RiesgoResponse(r.getId(), r.getPoliza().getId(), r.getDireccion(), r.getCiudad(),
				new PersonaResponse(r.getArrendatarioDocumento(), r.getArrendatarioNombre()),
				new PersonaResponse(r.getArrendadorDocumento(), r.getArrendadorNombre()), r.getCanonMensual(),
				r.getEstado(), r.getFechaInclusion(), r.getFechaCancelacion());
	}

	public static RenovacionResponse aResponse(Renovacion r) {
		return new RenovacionResponse(aResponse(r.getPoliza()), r.getAnioIpc(), r.getIpcAplicado(),
				r.getCanonAnterior(), r.getCanonNuevo(), r.getPrimaAnterior(), r.getPrimaNueva(),
				r.getFechaInicioAnterior(), r.getFechaFinAnterior());
	}

	public static <E, D> PaginaResponse<D> aPagina(Page<E> page, Function<E, D> mapper) {
		return new PaginaResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
				page.getTotalElements(), page.getTotalPages());
	}
}
