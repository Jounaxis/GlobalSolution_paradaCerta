package br.com.fiap.citizentech.ai;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.openai.errors.RateLimitException;

import br.com.fiap.citizentech.ai.ResultadoMensagemVoz.Detalhes;
import br.com.fiap.citizentech.ai.ResultadoMensagemVoz.Origem;
import br.com.fiap.citizentech.ai.ResultadoMensagemVoz.Referencia;
import br.com.fiap.citizentech.model.Linha;
import br.com.fiap.citizentech.model.enums.StatusSolicitacao;
import br.com.fiap.citizentech.model.enums.TipoNecessidade;

/** Gera a orientação falada e monta a mensagem com os fatos confirmados pela API. */
@Service
public class MensagemVozService {

	private static final Logger log = LoggerFactory.getLogger(MensagemVozService.class);
	static final int TAMANHO_MAXIMO = 300;
	private static final Duration PRAZO_MODELO = Duration.ofSeconds(3);
	private static final String ESPERA_GERAL = "Aguarde na parada.";
	private static final Pattern ORIENTACAO_SEGURA = Pattern.compile(
			"(?:por favor, )?(?:aguarde|espere)(?: com calma)? (?:na parada|perto do totem)\\.",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final String INSTRUCOES = """
			Escreva uma única instrução de espera falada por um totem de ônibus, em português do Brasil.
			A API já escreve a confirmação do pedido e os dados da viagem.
			Use as orientações recuperadas apenas para escolher a forma de dizer onde aguardar.
			Combine as seguintes partes na ordem:
			- "Por favor, " (opcional).
			- "Aguarde" ou "Espere".
			- " com calma" (opcional).
			- Use " na parada" como padrão. Use " perto do totem" se o trecho orientar a esperar perto do totem.
			Termine com ponto. Entregue somente a frase, sem aspas ou explicações.
			""";

	private static final String PEDIDO = """
			Orientações recuperadas:
			{orientacoes}
			""";

	private final ChatClient chatClient;
	private final RecuperadorOrientacoes recuperador;
	private final boolean ragHabilitado;

	public MensagemVozService(ChatClient.Builder chatClientBuilder, RecuperadorOrientacoes recuperador,
			@Value("${citizentech.ia.rag.habilitado:true}") boolean ragHabilitado) {
		this.chatClient = chatClientBuilder.defaultSystem(INSTRUCOES).build();
		this.recuperador = recuperador;
		this.ragHabilitado = ragHabilitado;
	}

	public ResultadoMensagemVoz gerar(ContextoMensagemVoz contexto) {
		List<OrientacaoEmbarque> orientacoes = ragHabilitado ? recuperador.buscar(contexto) : List.of();
		log.debug("Orientacoes RAG recuperadas: {}", orientacoes);
		List<Referencia> referencias = orientacoes.stream()
				.map(trecho -> new Referencia(trecho.id(), trecho.assunto(), trecho.origem()))
				.toList();
		String trechos = orientacoes.stream()
				.map(orientacao -> "- " + orientacao.texto())
				.collect(Collectors.joining("\n"));
		try {
			String resposta = chatClient.prompt()
					.user(usuario -> usuario.text(PEDIDO).param("orientacoes", trechos))
					.stream()
					.content()
					.collect(Collectors.joining())
					.block(PRAZO_MODELO);
			String orientacao = resposta == null ? "" : resposta.strip().replaceAll("\\s+", " ");
			if (ORIENTACAO_SEGURA.matcher(orientacao).matches()) {
				log.debug("Mensagem de voz: origem=MODELO");
				return resultado(contexto, orientacao, Origem.MODELO, referencias);
			}
			log.warn("Mensagem de voz: origem=CONTINGENCIA motivo=resposta_invalida");
		} catch (RateLimitException e) {
			String codigo = e.code().orElse("");
			String codigoSeguro = switch (codigo) {
				case "insufficient_quota", "credit_balance_exhausted", "usage_limit_exceeded",
						"organization_spend_limit_exceeded", "project_spend_limit_exceeded",
						"organization_usage_limit_exceeded", "rate_limit_exceeded", "slow_down" -> codigo;
				default -> e.type().filter("insufficient_quota"::equals).orElse("nao_classificado");
			};
			log.warn("Mensagem de voz: origem=CONTINGENCIA motivo=RateLimitException codigo={}", codigoSeguro);
		} catch (RuntimeException e) {
			String motivo = e.getCause() instanceof TimeoutException ? "tempo_limite" : e.getClass().getSimpleName();
			log.warn("Mensagem de voz: origem=CONTINGENCIA motivo={}", motivo);
		}
		String orientacao = switch (contexto.tipoNecessidade()) {
			case BAIXA_VISAO -> "Aguarde perto do totem.";
			case MOBILIDADE_REDUZIDA, IDOSO -> "Aguarde com calma na parada.";
			case null, default -> ESPERA_GERAL;
		};
		return resultado(contexto, orientacao, Origem.CONTINGENCIA, referencias);
	}

	public String mensagemDeStatus(StatusSolicitacao status, Linha linha, String mensagemAnterior) {
		String orientacao = status == StatusSolicitacao.MOTORISTA_AVISADO ? orientacaoAnterior(mensagemAnterior) : "";
		return montar(status, linha == null ? null : linha.getLetreiro(), linha == null ? null : linha.getDestino(),
				orientacao);
	}

	private ResultadoMensagemVoz resultado(ContextoMensagemVoz contexto, String orientacao, Origem origem,
			List<Referencia> referencias) {
		String texto = montar(StatusSolicitacao.CRIADA, contexto.letreiroLinha(), contexto.destinoLinha(), orientacao);
		return new ResultadoMensagemVoz(texto, new Detalhes(origem, referencias));
	}

	private String orientacaoAnterior(String mensagem) {
		int separador = mensagem == null ? -1 : mensagem.indexOf(". ");
		if (separador < 0) {
			return ESPERA_GERAL;
		}
		String orientacao = mensagem.substring(separador + 2);
		return ORIENTACAO_SEGURA.matcher(orientacao).matches() ? orientacao : ESPERA_GERAL;
	}

	private String montar(StatusSolicitacao status, String linha, String destino, String orientacao) {
		String inicio = switch (status) {
			case CRIADA -> "Pedido de embarque registrado";
			case MOTORISTA_AVISADO -> "Aviso de embarque registrado";
			case EMBARCADO -> "Embarque confirmado";
			case NAO_ATENDIDA -> "O pedido ainda não foi atendido";
			case SEM_ONIBUS -> "Nenhum ônibus foi encontrado";
			case CANCELADA -> "Pedido de embarque cancelado";
		};
		String finalDaMensagem = orientacao.isEmpty() ? "." : ". " + orientacao;
		String letreiro = rotuloFalado(linha);
		if (letreiro.isEmpty()) {
			return inicio + finalDaMensagem;
		}
		String comLinha = inicio + " para a linha " + letreiro;
		String destinoFalado = rotuloFalado(destino);
		if (!destinoFalado.isEmpty()) {
			String comDestino = comLinha + ", sentido " + destinoFalado + finalDaMensagem;
			if (comDestino.getBytes(StandardCharsets.UTF_8).length <= TAMANHO_MAXIMO) {
				return comDestino;
			}
		}
		String semDestino = comLinha + finalDaMensagem;
		return semDestino.getBytes(StandardCharsets.UTF_8).length <= TAMANHO_MAXIMO
				? semDestino : inicio + finalDaMensagem;
	}

	private String rotuloFalado(String valor) {
		return valor == null ? "" : valor.replaceAll("[.!?…。！？\\p{C}\\p{Z}\\s]+", " ").strip();
	}
}
