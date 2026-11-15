package br.com.fiap.citizentech.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import br.com.fiap.citizentech.config.RabbitConfig;
import lombok.RequiredArgsConstructor;

/**
 * Envia o {@link SolicitacaoCriadaEvent} para o RabbitMQ.
 * <p>
 * So roda depois do COMMIT da transacao que criou a solicitacao: se o envio fosse antes,
 * o consumidor poderia receber o id de uma solicitacao que ainda nao esta no banco
 * (ou que foi desfeita por um erro).
 * <p>
 * Ativo no modo de mensageria "rabbit" (padrao). No modo "local", quem trata o evento e o
 * {@link AvisoMotoristaLocal}.
 */
@Component
@ConditionalOnProperty(name = "citizentech.mensageria.modo", havingValue = "rabbit", matchIfMissing = true)
@RequiredArgsConstructor
public class SolicitacaoEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(SolicitacaoEventPublisher.class);

	private final RabbitTemplate rabbitTemplate;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void publicar(SolicitacaoCriadaEvent evento) {
		try {
			rabbitTemplate.convertAndSend(
					RabbitConfig.EXCHANGE_SOLICITACOES,
					RabbitConfig.ROUTING_KEY_SOLICITACAO_CRIADA,
					evento);
			log.info("Evento solicitacao.criada publicado para a solicitacao {}", evento.solicitacaoId());
		} catch (AmqpException e) {
			// A solicitacao ja esta gravada e o totem ja recebeu a resposta: o erro e registrado
			// e a solicitacao fica como CRIADA, visivel para o gestor.
			log.error("Falha ao publicar o evento da solicitacao {} no RabbitMQ", evento.solicitacaoId(), e);
		}
	}

}
