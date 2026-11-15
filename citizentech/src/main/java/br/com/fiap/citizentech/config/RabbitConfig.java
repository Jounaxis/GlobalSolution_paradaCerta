package br.com.fiap.citizentech.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Mensageria assincrona com RabbitMQ (Requisito 5 - Opcao A).
 *
 * <pre>
 * SolicitacaoEmbarqueService --(evento, apos o commit)--> exchange citizentech.solicitacoes
 *     --routing key solicitacao.criada--> fila citizentech.solicitacao.criada --> AvisoMotoristaListener
 *
 * Mensagem que falha 3 vezes no consumidor --> fila citizentech.solicitacao.criada.dlq
 * </pre>
 *
 * Exchange, filas e bindings sao criados automaticamente no RabbitMQ quando a aplicacao conecta.
 */
@Configuration
public class RabbitConfig {

	public static final String EXCHANGE_SOLICITACOES = "citizentech.solicitacoes";
	public static final String ROUTING_KEY_SOLICITACAO_CRIADA = "solicitacao.criada";
	public static final String FILA_SOLICITACAO_CRIADA = "citizentech.solicitacao.criada";

	static final String EXCHANGE_DLX = "citizentech.solicitacoes.dlx";
	static final String FILA_SOLICITACAO_CRIADA_DLQ = "citizentech.solicitacao.criada.dlq";

	@Bean
	public DirectExchange solicitacoesExchange() {
		return new DirectExchange(EXCHANGE_SOLICITACOES);
	}

	/** Fila duravel; mensagens rejeitadas vao para a DLQ em vez de se perderem ou ficarem em loop. */
	@Bean
	public Queue filaSolicitacaoCriada() {
		return QueueBuilder.durable(FILA_SOLICITACAO_CRIADA)
				.deadLetterExchange(EXCHANGE_DLX)
				.deadLetterRoutingKey(ROUTING_KEY_SOLICITACAO_CRIADA)
				.build();
	}

	@Bean
	public Binding bindingSolicitacaoCriada() {
		return BindingBuilder.bind(filaSolicitacaoCriada())
				.to(solicitacoesExchange())
				.with(ROUTING_KEY_SOLICITACAO_CRIADA);
	}

	@Bean
	public DirectExchange deadLetterExchange() {
		return new DirectExchange(EXCHANGE_DLX);
	}

	/** Dead letter queue: guarda os eventos que nao puderam ser processados, para analise. */
	@Bean
	public Queue filaSolicitacaoCriadaDlq() {
		return QueueBuilder.durable(FILA_SOLICITACAO_CRIADA_DLQ).build();
	}

	@Bean
	public Binding bindingSolicitacaoCriadaDlq() {
		return BindingBuilder.bind(filaSolicitacaoCriadaDlq())
				.to(deadLetterExchange())
				.with(ROUTING_KEY_SOLICITACAO_CRIADA);
	}

	/** Mensagens em JSON. So aceita desserializar tipos do pacote de mensageria da aplicacao. */
	@Bean
	public MessageConverter messageConverter() {
		return new JacksonJsonMessageConverter("br.com.fiap.citizentech.messaging");
	}

}
