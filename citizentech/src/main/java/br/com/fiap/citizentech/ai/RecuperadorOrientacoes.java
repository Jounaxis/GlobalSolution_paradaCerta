package br.com.fiap.citizentech.ai;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import br.com.fiap.citizentech.model.enums.TipoSinal;
import tools.jackson.databind.ObjectMapper;

@Component
class RecuperadorOrientacoes {

	private static final int LIMITE_TRECHOS = 3;

	private final List<OrientacaoIndexada> base;

	RecuperadorOrientacoes(ObjectMapper mapper,
			@Value("classpath:rag/orientacoes-embarque.json") Resource recurso) throws IOException {
		try (InputStream entrada = recurso.getInputStream()) {
			OrientacaoEmbarque[] orientacoes = mapper.readValue(entrada, OrientacaoEmbarque[].class);
			base = Arrays.stream(orientacoes)
					.map(orientacao -> new OrientacaoIndexada(orientacao, termosNormalizados(orientacao.termos())))
					.toList();
		}
	}

	List<OrientacaoEmbarque> buscar(ContextoMensagemVoz contexto) {
		List<String> consulta = new ArrayList<>();
		consulta.add("pedido criado");
		if (contexto.tipoNecessidade() != null) {
			consulta.add(contexto.tipoNecessidade().name());
		} else if (contexto.tipoSinal() == TipoSinal.BOTAO) {
			consulta.add("botao sem passageiro");
		}
		if (!StringUtils.hasText(contexto.letreiroLinha())) {
			consulta.add("linha ausente");
		}
		if (!StringUtils.hasText(contexto.destinoLinha())) {
			consulta.add("destino ausente");
		}
		return buscar(consulta);
	}

	List<OrientacaoEmbarque> buscar(List<String> consulta) {
		Set<String> termos = termosNormalizados(consulta);
		return base.stream()
				.map(item -> new Correspondencia(item.orientacao(), item.termos().stream().filter(termos::contains).count()))
				.filter(item -> item.pontos() > 0)
				.sorted(Comparator.comparingLong(Correspondencia::pontos).reversed()
						.thenComparing(item -> item.orientacao().id()))
				.limit(LIMITE_TRECHOS)
				.map(Correspondencia::orientacao)
				.toList();
	}

	private static Set<String> termosNormalizados(List<String> termos) {
		return termos.stream()
				.map(termo -> Normalizer.normalize(termo, Normalizer.Form.NFD)
						.toLowerCase(Locale.ROOT)
						.replaceAll("\\p{M}", "")
						.replaceAll("[^\\p{L}\\p{N}]+", " ")
						.strip())
				.filter(termo -> !termo.isEmpty())
				.collect(Collectors.toUnmodifiableSet());
	}

	private record OrientacaoIndexada(OrientacaoEmbarque orientacao, Set<String> termos) {
	}

	private record Correspondencia(OrientacaoEmbarque orientacao, long pontos) {
	}
}
