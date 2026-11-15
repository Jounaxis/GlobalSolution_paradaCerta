package br.com.fiap.citizentech.ai;

import br.com.fiap.citizentech.model.enums.TipoNecessidade;
import br.com.fiap.citizentech.model.enums.TipoSinal;

/**
 * O que a IA recebe para montar a mensagem de voz do totem.
 * <p>
 * LGPD: nenhum dado pessoal vai para o modelo (nem nome, nem contato).
 * So o tipo de necessidade, que e o que muda a orientacao falada.
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
