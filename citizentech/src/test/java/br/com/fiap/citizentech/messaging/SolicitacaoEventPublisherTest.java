package br.com.fiap.citizentech.messaging;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import br.com.fiap.citizentech.config.RabbitConfig;
import br.com.fiap.citizentech.model.enums.TipoSinal;

class SolicitacaoEventPublisherTest {

	private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
	private final SolicitacaoEventPublisher publisher = new SolicitacaoEventPublisher(rabbitTemplate);

	private final SolicitacaoCriadaEvent evento = new SolicitacaoCriadaEvent(
			10L, 2L, 3L, TipoSinal.CARTAO, LocalDateTime.now());

	@Test
	void enviaOEventoParaAExchangeEARoutingKeyCertas() {
		publisher.publicar(evento);

		verify(rabbitTemplate).convertAndSend(
				RabbitConfig.EXCHANGE_SOLICITACOES,
				RabbitConfig.ROUTING_KEY_SOLICITACAO_CRIADA,
				evento);
	}

	@Test
	void naoDerrubaAApiQuandoORabbitEstaForaDoAr() {
		doThrow(new AmqpConnectException(new RuntimeException("Connection refused")))
				.when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

		assertThatCode(() -> publisher.publicar(evento)).doesNotThrowAnyException();
	}

}
