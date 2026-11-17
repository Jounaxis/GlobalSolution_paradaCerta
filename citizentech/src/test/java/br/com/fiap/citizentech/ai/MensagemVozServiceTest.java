package br.com.fiap.citizentech.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.PromptUserSpec;
import org.springframework.core.io.ClassPathResource;

import br.com.fiap.citizentech.ai.ResultadoMensagemVoz.Origem;
import br.com.fiap.citizentech.model.Linha;
import br.com.fiap.citizentech.model.enums.StatusSolicitacao;
import br.com.fiap.citizentech.model.enums.TipoNecessidade;
import br.com.fiap.citizentech.model.enums.TipoSinal;
import reactor.core.publisher.Flux;
import tools.jackson.databind.json.JsonMapper;

/** Usa respostas controladas sem chamar o modelo. */
class MensagemVozServiceTest {

	private static final ContextoMensagemVoz BAIXA_VISAO_COM_LINHA = new ContextoMensagemVoz(
			TipoNecessidade.BAIXA_VISAO, "6091-10", "Terminal Varginha", TipoSinal.CARTAO);
	private static final ContextoMensagemVoz BOTAO = new ContextoMensagemVoz(null, null, null, TipoSinal.BOTAO);

	private ChatClient chatClient;
	private MensagemVozService service;
	private ChatClient.Builder builder;
	private RecuperadorOrientacoes recuperador;

	@BeforeEach
	void configurar() throws IOException {
		chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
		builder = mock(ChatClient.Builder.class);
		when(builder.defaultSystem(anyString())).thenReturn(builder);
		when(builder.build()).thenReturn(chatClient);
		recuperador = new RecuperadorOrientacoes(new JsonMapper(),
				new ClassPathResource("rag/orientacoes-embarque.json"));
		service = new MensagemVozService(builder, recuperador, true);
	}

	@Test
	void combinaOsFatosDaApiComAOrientacaoDoModelo() {
		respostaDaIa("  Por favor, espere perto do\n totem.  ");

		ResultadoMensagemVoz mensagem = service.gerar(BAIXA_VISAO_COM_LINHA);

		assertThat(mensagem.texto()).isEqualTo("Pedido de embarque registrado para a linha 6091-10, sentido Terminal Varginha. "
				+ "Por favor, espere perto do totem.");
		assertThat(mensagem.detalhes().origem()).isEqualTo(Origem.MODELO);
		assertThat(mensagem.detalhes().referencias()).extracting(ResultadoMensagemVoz.Referencia::id)
				.containsExactly("PC-01", "PC-04");
	}

	@Test
	void enviaApenasOTrechoDeOrientacaoAoModelo() {
		respostaDaIa("Aguarde perto do totem.");

		service.gerar(BAIXA_VISAO_COM_LINHA);

		PromptUserSpec pedido = capturarPedidoDoModelo();

		ArgumentCaptor<String> orientacoes = ArgumentCaptor.forClass(String.class);
		verify(pedido).text("Orientações recuperadas:\n{orientacoes}\n");
		verify(pedido).param(eq("orientacoes"), orientacoes.capture());
		verifyNoMoreInteractions(pedido);
		assertThat(orientacoes.getValue())
				.contains("Confirme em palavras simples", "aguardar perto do totem")
				.doesNotContain("BAIXA_VISAO", "6091-10", "Terminal Varginha", "PC-04");
	}

	@Test
	void comRagDesabilitadoAindaGeraMensagemSemEnviarOrientacoes() {
		MensagemVozService semRag = new MensagemVozService(builder, recuperador, false);
		respostaDaIa("Aguarde na parada.");

		ResultadoMensagemVoz mensagem = semRag.gerar(BAIXA_VISAO_COM_LINHA);

		assertThat(mensagem.detalhes().origem()).isEqualTo(Origem.MODELO);
		assertThat(mensagem.detalhes().referencias()).isEmpty();
		verify(capturarPedidoDoModelo()).param("orientacoes", "");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"Seu ônibus chegará em 4 minutos.",
			"Aguarde na parada. A rampa está pronta.",
			"Espere perto do totem. O motorista já foi avisado.",
			"  "
	})
	void usaContingenciaQuandoARespostaNaoSegueOFormatoSeguro(String resposta) {
		respostaDaIa(resposta);

		ResultadoMensagemVoz mensagem = service.gerar(BOTAO);

		assertThat(mensagem.texto()).isEqualTo("Pedido de embarque registrado. Aguarde na parada.");
		assertThat(mensagem.detalhes().origem()).isEqualTo(Origem.CONTINGENCIA);
	}

	@Test
	void contingenciaPreservaReferenciaFalavelSemPromessa() {
		falhaDaIa();

		ResultadoMensagemVoz mensagem = service.gerar(BAIXA_VISAO_COM_LINHA);

		assertThat(mensagem.texto()).isEqualTo(
				"Pedido de embarque registrado para a linha 6091-10, sentido Terminal Varginha. Aguarde perto do totem.");
		assertThat(mensagem.detalhes().origem()).isEqualTo(Origem.CONTINGENCIA);
	}

	@Test
	void omiteDestinoQuandoFaltaOuExcedeOLimiteEmBytes() {
		respostaDaIa("Aguarde na parada.");
		for (String destino : new String[] { null, "  ", "車".repeat(120) }) {
			String mensagem = service.gerar(new ContextoMensagemVoz(null, "6091-10", destino, TipoSinal.BOTAO)).texto();
			assertThat(mensagem.getBytes(StandardCharsets.UTF_8)).hasSizeLessThanOrEqualTo(300);
			assertThat(mensagem).isEqualTo("Pedido de embarque registrado para a linha 6091-10. Aguarde na parada.");
		}
	}

	@Test
	void usaRotuloEmUmaFraseQuandoODestinoTemPontuacao() {
		respostaDaIa("Aguarde na parada.");
		ContextoMensagemVoz pedido = new ContextoMensagemVoz(null, "6091-10", "Term.\nVarginha", TipoSinal.BOTAO);

		assertThat(service.gerar(pedido).texto()).isEqualTo(
				"Pedido de embarque registrado para a linha 6091-10, sentido Term Varginha. Aguarde na parada.");
	}

	@Test
	void preservaOrientacaoValidadaQuandoOAvisoERegistrado() {
		Linha linha = Linha.builder().letreiro("6091-10").build();

		assertThat(service.mensagemDeStatus(StatusSolicitacao.MOTORISTA_AVISADO, linha,
				"Pedido de embarque registrado. Espere perto do totem."))
				.isEqualTo("Aviso de embarque registrado para a linha 6091-10. Espere perto do totem.");
		assertThat(service.mensagemDeStatus(StatusSolicitacao.MOTORISTA_AVISADO, linha,
				"Pedido de embarque registrado. O motorista vai preparar a rampa."))
				.isEqualTo("Aviso de embarque registrado para a linha 6091-10. Aguarde na parada.");
	}

	@ParameterizedTest
	@CsvSource({
			"EMBARCADO, Embarque confirmado.",
			"NAO_ATENDIDA, O pedido ainda não foi atendido.",
			"SEM_ONIBUS, Nenhum ônibus foi encontrado.",
			"CANCELADA, Pedido de embarque cancelado."
	})
	void atualizaFraseParaOEstadoDoPedido(StatusSolicitacao status, String texto) {
		assertThat(service.mensagemDeStatus(status, null, "Pedido registrado. Aguarde perto do totem."))
				.isEqualTo(texto);
	}

	@SuppressWarnings("unchecked")
	private PromptUserSpec capturarPedidoDoModelo() {
		ArgumentCaptor<Consumer<PromptUserSpec>> captor = ArgumentCaptor.forClass(Consumer.class);
		// A ultima chamada e a feita pelo servico, apos preparar os mocks.
		verify(chatClient.prompt(), atLeastOnce()).user(captor.capture());
		PromptUserSpec pedido = mock(PromptUserSpec.class, RETURNS_SELF);
		captor.getAllValues().getLast().accept(pedido);
		return pedido;
	}

	@SuppressWarnings("unchecked")
	private void falhaDaIa() {
		when(chatClient.prompt().user(any(Consumer.class)).stream().content())
				.thenReturn(Flux.error(new RuntimeException("Provedor fora do ar")));
	}

	@SuppressWarnings("unchecked")
	private void respostaDaIa(String texto) {
		when(chatClient.prompt().user(any(Consumer.class)).stream().content()).thenReturn(Flux.just(texto));
	}
}
