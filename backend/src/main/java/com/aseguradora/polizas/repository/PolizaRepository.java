package com.aseguradora.polizas.repository;

import java.util.Optional;

import com.aseguradora.polizas.domain.EstadoPoliza;
import com.aseguradora.polizas.domain.Poliza;
import com.aseguradora.polizas.domain.TipoPoliza;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PolizaRepository extends JpaRepository<Poliza, Long> {

	Page<Poliza> findByTipo(TipoPoliza tipo, Pageable pageable);

	Page<Poliza> findByEstado(EstadoPoliza estado, Pageable pageable);

	Page<Poliza> findByTipoAndEstado(TipoPoliza tipo, EstadoPoliza estado, Pageable pageable);

	/** Carga la póliza con sus riesgos en una sola consulta (las operaciones del agregado los necesitan). */
	@EntityGraph(attributePaths = "riesgos")
	Optional<Poliza> findWithRiesgosById(Long id);

	@Query(value = "SELECT nextval('poliza_numero_seq')", nativeQuery = true)
	long siguienteNumero();
}
