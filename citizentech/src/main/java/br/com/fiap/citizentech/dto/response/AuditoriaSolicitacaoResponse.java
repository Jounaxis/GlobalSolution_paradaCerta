package br.com.fiap.citizentech.dto.response;

import java.time.LocalDateTime;

import br.com.fiap.citizentech.model.AuditoriaSolicitacao;
import br.com.fiap.citizentech.model.enums.OperacaoAuditoria;
import br.com.fiap.citizentech.model.enums.StatusSolicitacao;

public record AuditoriaSolicitacaoResponse(
		Long id,
		Long solicitacaoId,
		OperacaoAuditoria operacao,
		StatusSolicitacao statusAntigo,
		StatusSolicitacao statusNovo,
		String usuarioBanco,
		LocalDateTime dataHora) {

	public static AuditoriaSolicitacaoResponse from(AuditoriaSolicitacao auditoria) {
		return new AuditoriaSolicitacaoResponse(
				auditoria.getId(),
				auditoria.getSolicitacaoId(),
				auditoria.getOperacao(),
				auditoria.getStatusAntigo(),
				auditoria.getStatusNovo(),
				auditoria.getUsuarioBanco(),
				auditoria.getDataHora());
	}

}
