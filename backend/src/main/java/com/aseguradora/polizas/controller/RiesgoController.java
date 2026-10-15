package com.aseguradora.polizas.controller;

import com.aseguradora.polizas.controller.dto.PolizaDtos.RiesgoResponse;
import com.aseguradora.polizas.controller.dto.PolizaMapper;
import com.aseguradora.polizas.service.RiesgoService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/riesgos")
public class RiesgoController {

	private final RiesgoService service;

	public RiesgoController(RiesgoService service) {
		this.service = service;
	}

	/** 6. Cancela un riesgo. */
	@PostMapping("/{id}/cancelar")
	public RiesgoResponse cancelar(@PathVariable Long id) {
		return PolizaMapper.aResponse(service.cancelar(id));
	}
}
