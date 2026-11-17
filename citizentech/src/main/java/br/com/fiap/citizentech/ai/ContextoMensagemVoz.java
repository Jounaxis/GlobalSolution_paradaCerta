package br.com.fiap.citizentech.ai;

import br.com.fiap.citizentech.model.enums.TipoNecessidade;
import br.com.fiap.citizentech.model.enums.TipoSinal;

/**
 * Dados disponiveis para montar a mensagem de voz do totem.
 * <p>
 * A necessidade e usada localmente para escolher a orientacao de acessibilidade.
 * O modelo recebe o texto das orientacoes e os dados da viagem descritos no prompt.
 *
 * @param tipoNecessidade necessidade do passageiro; null quando o pedido veio do botao, sem cartao
 * @param letreiroLinha   letreiro da linha (ex.: 6091-10); null quando a linha nao e conhecida
 * @param destinoLinha    destino da linha (ex.: Terminal Varginha)
 * @param nomeParada      nome da parada onde esta o totem
 * @param tipoSinal       CARTAO ou BOTAO
 */
public record ContextoMensagemVoz(
		TipoNecessidade tipoNecessidade,
		String letreiroLinha,
		String destinoLinha,
		String nomeParada,
		TipoSinal tipoSinal) {
}
