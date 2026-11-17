package br.com.fiap.citizentech.ai;

import br.com.fiap.citizentech.model.enums.TipoNecessidade;
import br.com.fiap.citizentech.model.enums.TipoSinal;

/**
 * Dados disponiveis para montar a mensagem de voz do totem.
 * <p>
 * A necessidade e usada localmente para escolher a orientacao de acessibilidade.
 * O modelo recebe os trechos de acessibilidade. Os dados da viagem sao usados pela API.
 *
 * @param tipoNecessidade necessidade do passageiro; null quando o pedido veio do botao, sem cartao
 * @param letreiroLinha   letreiro da linha (ex.: 6091-10); null quando a linha nao e conhecida
 * @param destinoLinha    destino da linha (ex.: Terminal Varginha)
 * @param tipoSinal       CARTAO ou BOTAO
 */
public record ContextoMensagemVoz(
		TipoNecessidade tipoNecessidade,
		String letreiroLinha,
		String destinoLinha,
		TipoSinal tipoSinal) {
}
