package br.com.fiap.citizentech.model.enums;

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
}
