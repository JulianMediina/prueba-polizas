package com.aseguradora.polizas.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

/** Registro histórico e inmutable de una renovación: qué IPC se aplicó y cómo cambiaron canon, prima y vigencia. */
@Entity
@Table(name = "renovacion")
public class Renovacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "poliza_id", nullable = false, updatable = false)
	private Poliza poliza;

	@Column(name = "anio_ipc", nullable = false)
	private int anioIpc;

	@Column(name = "ipc_aplicado", nullable = false, precision = 6, scale = 3)
	private BigDecimal ipcAplicado;

	@Column(name = "canon_anterior", nullable = false, precision = 15, scale = 2)
	private BigDecimal canonAnterior;

	@Column(name = "canon_nuevo", nullable = false, precision = 15, scale = 2)
	private BigDecimal canonNuevo;

	@Column(name = "prima_anterior", nullable = false, precision = 17, scale = 2)
	private BigDecimal primaAnterior;

	@Column(name = "prima_nueva", nullable = false, precision = 17, scale = 2)
	private BigDecimal primaNueva;

	@Column(name = "fecha_inicio_anterior", nullable = false)
	private LocalDate fechaInicioAnterior;

	@Column(name = "fecha_fin_anterior", nullable = false)
	private LocalDate fechaFinAnterior;

	@Column(name = "fecha_inicio_nueva", nullable = false)
	private LocalDate fechaInicioNueva;

	@Column(name = "fecha_fin_nueva", nullable = false)
	private LocalDate fechaFinNueva;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Renovacion() {
		// Requerido por JPA
	}

	Renovacion(Poliza poliza, Ipc ipc, BigDecimal canonAnterior, BigDecimal primaAnterior,
			LocalDate fechaInicioAnterior, LocalDate fechaFinAnterior) {
		this.poliza = poliza;
		this.anioIpc = ipc.getAnio();
		this.ipcAplicado = ipc.getPorcentaje();
		this.canonAnterior = canonAnterior;
		this.primaAnterior = primaAnterior;
		this.fechaInicioAnterior = fechaInicioAnterior;
		this.fechaFinAnterior = fechaFinAnterior;
		this.canonNuevo = poliza.getCanonMensual();
		this.primaNueva = poliza.getPrima();
		this.fechaInicioNueva = poliza.getFechaInicio();
		this.fechaFinNueva = poliza.getFechaFin();
	}

	public Long getId() {
		return id;
	}

	public Poliza getPoliza() {
		return poliza;
	}

	public int getAnioIpc() {
		return anioIpc;
	}

	public BigDecimal getIpcAplicado() {
		return ipcAplicado;
	}

	public BigDecimal getCanonAnterior() {
		return canonAnterior;
	}

	public BigDecimal getCanonNuevo() {
		return canonNuevo;
	}

	public BigDecimal getPrimaAnterior() {
		return primaAnterior;
	}

	public BigDecimal getPrimaNueva() {
		return primaNueva;
	}

	public LocalDate getFechaInicioAnterior() {
		return fechaInicioAnterior;
	}

	public LocalDate getFechaFinAnterior() {
		return fechaFinAnterior;
	}

	public LocalDate getFechaInicioNueva() {
		return fechaInicioNueva;
	}

	public LocalDate getFechaFinNueva() {
		return fechaFinNueva;
	}
}
