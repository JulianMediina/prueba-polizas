package com.aseguradora.polizas.repository;

import java.util.List;

import com.aseguradora.polizas.domain.Riesgo;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RiesgoRepository extends JpaRepository<Riesgo, Long> {

	List<Riesgo> findByPolizaIdOrderById(Long polizaId);
}
