package br.com.fiap.citizentech.messaging;

import java.time.LocalDateTime;

import br.com.fiap.citizentech.model.enums.TipoSinal;

/**
 * Evento publicado no RabbitMQ quando uma solicitacao de embarque e criada.
 * <p>
 * Leva so ids e dados operacionais (nenhum dado pessoal): o consumidor busca o resto no banco.
 *
 * @param solicitacaoId id da solicitacao criada
 * @param totemId       totem onde o pedido foi feito
 * @param linhaId       linha pedida; null quando o pedido veio do botao, sem cartao
 * @param tipoSinal     CARTAO ou BOTAO
 * @param criadaEm      momento da criacao
 */
public record SolicitacaoCriadaEvent(
		Long solicitacaoId,
		Long totemId,
		Long linhaId,
		TipoSinal tipoSinal,
		LocalDateTime criadaEm) {
}
