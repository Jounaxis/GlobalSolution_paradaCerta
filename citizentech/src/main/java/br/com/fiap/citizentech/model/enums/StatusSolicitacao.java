package br.com.fiap.citizentech.model.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida da solicitacao de embarque (coluna solicitacao_embarque.status).
 */
public enum StatusSolicitacao {
	CRIADA,
	MOTORISTA_AVISADO,
	EMBARCADO,
	NAO_ATENDIDA,
	SEM_ONIBUS,
	CANCELADA;

	/**
	 * Estados em que a solicitacao ainda esta em andamento. Sao os mesmos do indice
	 * uq_solic_passageiro_ativa: um passageiro so pode ter uma solicitacao nesses estados.
	 */
	public static final Set<StatusSolicitacao> ATIVOS = EnumSet.of(CRIADA, MOTORISTA_AVISADO, NAO_ATENDIDA);

	/** Estados finais (EMBARCADO, SEM_ONIBUS, CANCELADA) nao mudam mais. */
	public boolean isFinal() {
		return !ATIVOS.contains(this);
	}

}
