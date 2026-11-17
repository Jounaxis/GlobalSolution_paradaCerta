package br.com.fiap.citizentech.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.ClassPathResource;

import br.com.fiap.citizentech.model.enums.TipoNecessidade;
import br.com.fiap.citizentech.model.enums.TipoSinal;
import tools.jackson.databind.json.JsonMapper;

class RecuperadorOrientacoesTest {

	private RecuperadorOrientacoes recuperador;

	@BeforeEach
	void carregarBase() throws IOException {
		recuperador = new RecuperadorOrientacoes(new JsonMapper(),
				new ClassPathResource("rag/orientacoes-embarque.json"));
	}

	@ParameterizedTest
	@CsvSource({ "BAIXA_VISAO,PC-04", "CADEIRA_DE_RODAS,PC-05", "MOBILIDADE_REDUZIDA,PC-06", "IDOSO,PC-07" })
	void recuperaAOrientacaoDeCadaNecessidade(TipoNecessidade necessidade, String id) {
		ContextoMensagemVoz pedido = new ContextoMensagemVoz(
				necessidade, "6091-10", "Terminal Varginha", TipoSinal.CARTAO);

		List<OrientacaoEmbarque> orientacoes = recuperador.buscar(pedido);

		assertThat(orientacoes).extracting(OrientacaoEmbarque::id).containsExactly("PC-01", id);
		assertThat(orientacoes).allSatisfy(trecho -> {
			assertThat(trecho.assunto()).isNotBlank();
			assertThat(trecho.origem()).contains("Parada-Certa-Documentacao.pdf");
			assertThat(trecho.texto()).isNotBlank();
		});
	}

	@Test
	void recuperaOrientacoesParaBotaoSemIdentificacaoEDadosAusentes() {
		ContextoMensagemVoz pedido = new ContextoMensagemVoz(null, " ", null, TipoSinal.BOTAO);

		assertThat(recuperador.buscar(pedido)).extracting(OrientacaoEmbarque::id)
				.containsExactly("PC-03", "PC-01", "PC-02");
	}

	@Test
	void recuperaDadosIncompletosQuandoSoODestinoFalta() {
		ContextoMensagemVoz pedido = new ContextoMensagemVoz(
				TipoNecessidade.IDOSO, "6091-10", null, TipoSinal.CARTAO);

		assertThat(recuperador.buscar(pedido)).extracting(OrientacaoEmbarque::id)
				.containsExactly("PC-01", "PC-03", "PC-07");
	}

	@Test
	void ordenaPorTermosDistintosEDesempataPorIdComLimiteDeTres() {
		assertThat(recuperador.buscar(List.of("LINHA_AUSENTE", "Destino ausente", "Pedido criado",
				"BOTÃO SEM PASSAGEIRO", "BAIXA-VISÃO", "baixa visao")))
				.extracting(OrientacaoEmbarque::id)
				.containsExactly("PC-03", "PC-01", "PC-02");
	}

	@Test
	void devolveListaVaziaParaConsultaForaDaBase() {
		assertThat(recuperador.buscar(List.of("saldo do cartão", "horário de outra cidade"))).isEmpty();
	}
}
