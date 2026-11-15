package br.com.fiap.citizentech.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fiap.citizentech.model.Linha;

public interface LinhaRepository extends JpaRepository<Linha, Long> {

	boolean existsByCodigoSptrans(Long codigoSptrans);

	boolean existsByCodigoSptransAndIdNot(Long codigoSptrans, Long id);

}
