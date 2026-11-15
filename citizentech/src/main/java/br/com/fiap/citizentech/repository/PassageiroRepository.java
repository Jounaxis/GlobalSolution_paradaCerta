package br.com.fiap.citizentech.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fiap.citizentech.model.Passageiro;

public interface PassageiroRepository extends JpaRepository<Passageiro, Long> {

	Page<Passageiro> findByCuidadorId(Long cuidadorId, Pageable pageable);

}
