package br.com.fiap.citizentech.ai;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.openai.errors.RateLimitException;

import br.com.fiap.citizentech.model.enums.TipoNecessidade;
import br.com.fiap.citizentech.model.enums.TipoSinal;

/**
 * Funcionalidade de IA generativa da Parada Certa (Spring AI + OpenAI).
 * <p>
 * Quando uma solicitacao de embarque e criada, o LLM escreve a frase que o totem
 * fala ao passageiro, adaptada ao tipo de necessidade, a linha e ao destino.
 * As orientacoes de acessibilidade sao recuperadas de uma base textual do projeto.
 * <p>
 * Se a OpenAI falhar ou demorar, entra uma frase padrao montada aqui mesmo:
 * o totem nunca fica sem resposta.
 */
@Service
public class MensagemVozService {

	private static final Logger log = LoggerFactory.getLogger(MensagemVozService.class);

	/** Mesmo tamanho da coluna solicitacao_embarque.mensagem_voz. */
	static final int TAMANHO_MAXIMO = 300;

	private static final String INSTRUCOES = """
			Voce escreve as frases faladas por um totem de parada de onibus em Sao Paulo.
			O totem registra o pedido de ajuda para embarcar.

			Regras da frase:
			- Portugues do Brasil, tom acolhedor e direto, no maximo 2 frases e 250 caracteres.
			- Texto puro para leitura em voz alta: sem emojis, listas, aspas, siglas ou numeros de telefone.
			- Confirme que o pedido de embarque foi registrado.
			- Se a linha e o destino forem informados, cite os dois.
			- Nao prometa horario de chegada nem diga quantos minutos faltam.
			- Use as orientacoes recuperadas para adaptar a fala aos dados disponiveis.
			- As orientacoes nao confirmam fatos sobre este pedido. Evite mencionar a necessidade do passageiro.
			Responda somente com a frase.
			""";

	private static final String PEDIDO = """
			Linha: {linha}
			Destino: {destino}
			Parada: {parada}
			Acionamento: {sinal}

			Orientacoes recuperadas:
			{orientacoes}
			""";

	private final ChatClient chatClient;
	private final RecuperadorOrientacoes recuperador;

	public MensagemVozService(ChatClient.Builder chatClientBuilder, RecuperadorOrientacoes recuperador) {
		this.chatClient = chatClientBuilder.defaultSystem(INSTRUCOES).build();
		this.recuperador = recuperador;
	}

	public String gerar(ContextoMensagemVoz contexto) {
		try {
			List<OrientacaoEmbarque> orientacoes = recuperador.buscar(contexto);
			log.debug("Orientacoes RAG recuperadas: {}", orientacoes);
			String orientacoesRecuperadas = orientacoes.stream()
					.map(orientacao -> "- " + orientacao.texto())
					.collect(Collectors.joining("\n"));
			String resposta = chatClient.prompt()
					.user(usuario -> usuario.text(PEDIDO)
							.param("linha", valor(contexto.letreiroLinha(), "nao informada"))
							.param("destino", valor(contexto.destinoLinha(), "nao informado"))
							.param("parada", valor(contexto.nomeParada(), "nao informada"))
							.param("sinal", contexto.tipoSinal() == TipoSinal.CARTAO
									? "cartao do passageiro" : "botao do totem")
							.param("orientacoes", valor(orientacoesRecuperadas, "nenhuma orientacao especifica")))
					.call()
					.content();

			String mensagem = limpar(resposta);
			if (!mensagem.isEmpty()) {
				log.debug("Mensagem de voz: origem=MODELO");
				return mensagem;
			}
			log.warn("Mensagem de voz: origem=CONTINGENCIA motivo=resposta_vazia");
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
			log.warn("Mensagem de voz: origem=CONTINGENCIA motivo={}", e.getClass().getSimpleName());
		}
		return mensagemPadrao(contexto);
	}

	/** Frase usada quando a IA nao responde. Segue as mesmas regras do prompt. */
	String mensagemPadrao(ContextoMensagemVoz contexto) {
		StringBuilder frase = new StringBuilder("Pedido de embarque registrado");
		if (contexto.letreiroLinha() != null) {
			frase.append(" para a linha ").append(contexto.letreiroLinha());
			if (contexto.destinoLinha() != null) {
				frase.append(", sentido ").append(contexto.destinoLinha());
			}
		}
		frase.append(". ").append(orientacao(contexto.tipoNecessidade()));
		return frase.toString();
	}

	private String orientacao(TipoNecessidade necessidade) {
		if (necessidade == null) {
			return "Aguarde na parada, o motorista do proximo onibus sera avisado.";
		}
		return switch (necessidade) {
			case BAIXA_VISAO -> "Fique perto do totem, o onibus sera anunciado por voz quando chegar.";
			case CADEIRA_DE_RODAS -> "Aguarde na parada, o motorista vai preparar a rampa de acesso.";
			case MOBILIDADE_REDUZIDA, IDOSO -> "Aguarde na parada, o motorista vai esperar o seu embarque com calma.";
		};
	}

	/** Tira aspas e quebras de linha e corta no limite da coluna, sem partir palavras. */
	static String limpar(String resposta) {
		if (resposta == null) {
			return "";
		}
		String texto = resposta.replaceAll("[\"“”]", "").replaceAll("\\s+", " ").trim();
		if (texto.length() <= TAMANHO_MAXIMO) {
			return texto;
		}
		String cortado = texto.substring(0, TAMANHO_MAXIMO);
		int ultimoEspaco = cortado.lastIndexOf(' ');
		return (ultimoEspaco > 0 ? cortado.substring(0, ultimoEspaco) : cortado).trim();
	}

	private static String valor(String valor, String padrao) {
		return StringUtils.hasText(valor) ? valor : padrao;
	}

}
