package br.com.fiap.citizentech.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fiap.citizentech.model.Parada;

public interface ParadaRepository extends JpaRepository<Parada, Long> {

	boolean existsByCodigoSptrans(Long codigoSptrans);

	boolean existsByCodigoSptransAndIdNot(Long codigoSptrans, Long id);

}
