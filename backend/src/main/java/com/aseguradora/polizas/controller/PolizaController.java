package com.aseguradora.polizas.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import com.aseguradora.polizas.controller.dto.PolizaDtos.CrearPolizaRequest;
import com.aseguradora.polizas.controller.dto.PolizaDtos.PaginaResponse;
import com.aseguradora.polizas.controller.dto.PolizaDtos.PolizaResponse;
import com.aseguradora.polizas.controller.dto.PolizaDtos.RenovacionResponse;
import com.aseguradora.polizas.controller.dto.PolizaDtos.RiesgoRequest;
import com.aseguradora.polizas.controller.dto.PolizaDtos.RiesgoResponse;
import com.aseguradora.polizas.controller.dto.PolizaMapper;
import com.aseguradora.polizas.domain.EstadoPoliza;
import com.aseguradora.polizas.domain.TipoPoliza;
import com.aseguradora.polizas.service.PolizaService;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/polizas")
public class PolizaController {

	private final PolizaService service;

	public PolizaController(PolizaService service) {
		this.service = service;
	}

	/** 1. Lista pólizas filtrando (opcionalmente) por tipo y estado, paginado. */
	@GetMapping
	public PaginaResponse<PolizaResponse> listar(@RequestParam(required = false) TipoPoliza tipo,
			@RequestParam(required = false) EstadoPoliza estado,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
		return PolizaMapper.aPagina(service.listar(tipo, estado, pageable), PolizaMapper::aResponse);
	}

	@GetMapping("/{id}")
	public PolizaResponse obtener(@PathVariable Long id) {
		return PolizaMapper.aResponse(service.obtener(id));
	}

	/** Alta de pólizas: individual con su riesgo único, o colectiva con uno o más riesgos. */
	@PostMapping
	public ResponseEntity<PolizaResponse> crear(@Valid @RequestBody CrearPolizaRequest request) {
		var poliza = service.crear(PolizaMapper.aComando(request));
		var location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(poliza.getId())
			.toUri();
		return ResponseEntity.created(location).body(PolizaMapper.aResponse(poliza));
	}

	/** 2. Riesgos de la póliza. */
	@GetMapping("/{id}/riesgos")
	public List<RiesgoResponse> riesgos(@PathVariable Long id) {
		return service.listarRiesgos(id).stream().map(PolizaMapper::aResponse).toList();
	}

	/** 3. Renueva: canon y prima + IPC, nueva vigencia, estado RENOVADA. */
	@PostMapping("/{id}/renovar")
	public RenovacionResponse renovar(@PathVariable Long id) {
		return PolizaMapper.aResponse(service.renovar(id));
	}

	/** 4. Cancela la póliza y todos sus riesgos. */
	@PostMapping("/{id}/cancelar")
	public PolizaResponse cancelar(@PathVariable Long id) {
		return PolizaMapper.aResponse(service.cancelar(id));
	}

	/** 5. Agrega un riesgo; solo pólizas colectivas. */
	@PostMapping("/{id}/riesgos")
	@ResponseStatus(HttpStatus.CREATED)
	public RiesgoResponse agregarRiesgo(@PathVariable Long id, @Valid @RequestBody RiesgoRequest request) {
		return PolizaMapper.aResponse(service.agregarRiesgo(id, PolizaMapper.aDatos(request)));
	}
}
