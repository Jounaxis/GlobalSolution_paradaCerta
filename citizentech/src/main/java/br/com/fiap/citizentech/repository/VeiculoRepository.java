package br.com.fiap.citizentech.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fiap.citizentech.model.Veiculo;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

	boolean existsByPrefixo(String prefixo);

	boolean existsByPrefixoAndIdNot(String prefixo, Long id);

	boolean existsByMotoristaId(Long motoristaId);

	boolean existsByMotoristaIdAndIdNot(Long motoristaId, Long id);

}
