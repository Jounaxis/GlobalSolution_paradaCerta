package br.com.fiap.citizentech.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fiap.citizentech.model.TentativaEmbarque;

public interface TentativaEmbarqueRepository extends JpaRepository<TentativaEmbarque, Long> {

	List<TentativaEmbarque> findBySolicitacaoIdOrderByAlertadoEmAsc(Long solicitacaoId);

}
