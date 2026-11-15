package br.com.fiap.citizentech.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fiap.citizentech.model.Totem;

public interface TotemRepository extends JpaRepository<Totem, Long> {

	Optional<Totem> findByIdentificador(String identificador);

	boolean existsByIdentificador(String identificador);

	boolean existsByIdentificadorAndIdNot(String identificador, Long id);

}
