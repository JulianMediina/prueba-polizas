package com.aseguradora.polizas.domain;

import java.math.BigDecimal;
import java.math.MathContext;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Variación anual del IPC (en porcentaje) usada para ajustar el canon en las renovaciones. */
@Entity
@Table(name = "ipc")
public class Ipc {

	private static final BigDecimal CIEN = BigDecimal.valueOf(100);

	@Id
	private Integer anio;

	@Column(nullable = false, precision = 6, scale = 3)
	private BigDecimal porcentaje;

	@Column(nullable = false)
	private String fuente;

	protected Ipc() {
		// Requerido por JPA
	}

	public Ipc(int anio, BigDecimal porcentaje) {
		this.anio = anio;
		this.porcentaje = porcentaje;
		this.fuente = "DANE";
	}

	/** Factor multiplicador: 1 + IPC/100. */
	public BigDecimal factor() {
		return BigDecimal.ONE.add(porcentaje.divide(CIEN, MathContext.DECIMAL64));
	}

	public Integer getAnio() {
		return anio;
	}

	public BigDecimal getPorcentaje() {
		return porcentaje;
	}

	public String getFuente() {
		return fuente;
	}
}
