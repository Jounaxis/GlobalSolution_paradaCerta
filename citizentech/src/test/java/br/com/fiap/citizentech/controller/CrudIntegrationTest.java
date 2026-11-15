package br.com.fiap.citizentech.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testa os endpoints de ponta a ponta (controller -> service -> repository -> H2).
 * Cada teste roda em uma transacao desfeita no final, entao um nao interfere no outro.
 * As chamadas usam um token de GESTOR, que tem acesso a todos os cadastros.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CrudIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void criaParadaEListaPaginado() throws Exception {
		enviar("/api/paradas", """
				{"codigoSptrans": 340015329, "nome": "Av. Paulista, 1000", "latitude": -23.564, "longitude": -46.652}
				""")
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.nome").value("Av. Paulista, 1000"));

		mockMvc.perform(get("/api/paradas").param("size", "5").with(gestor()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].codigoSptrans", hasItem(340015329)))
				.andExpect(jsonPath("$.page.size").value(5));
	}

	@Test
	void devolve400ComCamposInvalidos() throws Exception {
		enviar("/api/linhas", """
				{"codigoSptrans": 1, "letreiro": "", "sentido": 3}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.campos[*].campo", hasItem("letreiro")))
				.andExpect(jsonPath("$.campos[*].campo", hasItem("sentido")));
	}

	@Test
	void devolve404QuandoNaoExiste() throws Exception {
		mockMvc.perform(get("/api/paradas/999999").with(gestor()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensagem").value("Parada com id 999999 nao encontrado(a)"));
	}

	@Test
	void devolve409ParaCodigoSptransDuplicado() throws Exception {
		String linha = """
				{"codigoSptrans": 1273, "letreiro": "6091-10", "sentido": 1, "destino": "Terminal Varginha"}
				""";
		enviar("/api/linhas", linha).andExpect(status().isCreated());
		enviar("/api/linhas", linha).andExpect(status().isConflict());
	}

	@Test
	void passageiroExigeConsentimentoECuidador() throws Exception {
		long motoristaId = criar("/api/usuarios", """
				{"nome": "Joao", "email": "joao@teste.com", "senha": "senha12345", "perfil": "MOTORISTA"}
				""");

		// sem aceite do termo: 400
		enviar("/api/passageiros", passageiroJson(motoristaId, false))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[*].campo", hasItem("consentimento")));

		// com aceite, mas o usuario nao e CUIDADOR: 422
		enviar("/api/passageiros", passageiroJson(motoristaId, true))
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void solicitacaoSegueAsRegrasDeStatus() throws Exception {
		long cuidadorId = criar("/api/usuarios", """
				{"nome": "Maria", "email": "MARIA@Teste.com", "senha": "senha12345", "perfil": "CUIDADOR"}
				""");
		long passageiroId = criar("/api/passageiros", passageiroJson(cuidadorId, true));
		long paradaId = criar("/api/paradas", """
				{"codigoSptrans": 1001, "nome": "Parada UBS"}
				""");
		long totemId = criar("/api/totens", """
				{"paradaId": %d, "identificador": "TOTEM-0001", "token": "token-secreto-do-totem"}
				""".formatted(paradaId));

		long solicitacaoId = criar("/api/solicitacoes", solicitacaoJson(totemId, passageiroId, "chave-1"));

		// mesmo passageiro com outra solicitacao ativa: 409
		enviar("/api/solicitacoes", solicitacaoJson(totemId, passageiroId, "chave-2"))
				.andExpect(status().isConflict());

		// encerra a solicitacao e tenta mudar de novo: 422
		mudarStatus(solicitacaoId, "EMBARCADO").andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("EMBARCADO"));
		mudarStatus(solicitacaoId, "CANCELADA").andExpect(status().isUnprocessableContent());

		// com a anterior encerrada, o passageiro pode abrir outra
		enviar("/api/solicitacoes", solicitacaoJson(totemId, passageiroId, "chave-3"))
				.andExpect(status().isCreated());
	}

	/** Token JWT simulado de um GESTOR (o login de verdade e testado em SecurityIntegrationTest). */
	private RequestPostProcessor gestor() {
		return jwt().jwt(token -> token.subject("1").claim("perfil", "GESTOR"))
				.authorities(new SimpleGrantedAuthority("ROLE_GESTOR"));
	}

	private String passageiroJson(long cuidadorId, boolean consentimento) {
		return """
				{"cuidadorId": %d, "nome": "Dona Ana", "tipoNecessidade": "BAIXA_VISAO",
				 "contatoEmergencia": "11999990000", "consentimento": %b, "versaoTermo": "v1"}
				""".formatted(cuidadorId, consentimento);
	}

	private String solicitacaoJson(long totemId, long passageiroId, String chave) {
		return """
				{"totemId": %d, "passageiroId": %d, "tipoSinal": "CARTAO", "chaveIdempotencia": "%s"}
				""".formatted(totemId, passageiroId, chave);
	}

	private ResultActions mudarStatus(long solicitacaoId, String status) throws Exception {
		return mockMvc.perform(patch("/api/solicitacoes/{id}/status", solicitacaoId)
				.with(gestor())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\": \"" + status + "\"}"));
	}

	private ResultActions enviar(String url, String json) throws Exception {
		return mockMvc.perform(post(url)
				.with(gestor())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	/** Faz o POST, confere o 201 e devolve o id do registro criado. */
	private long criar(String url, String json) throws Exception {
		String corpo = enviar(url, json)
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(corpo);
		if (!matcher.find()) {
			throw new IllegalStateException("Resposta sem id: " + corpo);
		}
		return Long.parseLong(matcher.group(1));
	}

}
