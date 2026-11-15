package br.com.fiap.citizentech.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fiap.citizentech.model.SinalTotem;

public interface SinalTotemRepository extends JpaRepository<SinalTotem, Long> {

	Optional<SinalTotem> findByChaveIdempotencia(String chaveIdempotencia);

}
