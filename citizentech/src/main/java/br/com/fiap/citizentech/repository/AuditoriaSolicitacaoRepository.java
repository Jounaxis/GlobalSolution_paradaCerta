package br.com.fiap.citizentech.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fiap.citizentech.model.AuditoriaSolicitacao;

public interface AuditoriaSolicitacaoRepository extends JpaRepository<AuditoriaSolicitacao, Long> {

	List<AuditoriaSolicitacao> findBySolicitacaoIdOrderByDataHoraAsc(Long solicitacaoId);

}
