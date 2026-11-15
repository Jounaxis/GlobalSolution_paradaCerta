package br.com.fiap.citizentech.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.citizentech.model.SolicitacaoEmbarque;
import br.com.fiap.citizentech.model.TentativaEmbarque;
import br.com.fiap.citizentech.model.enums.ResultadoTentativa;
import br.com.fiap.citizentech.model.enums.StatusSolicitacao;
import br.com.fiap.citizentech.repository.SolicitacaoEmbarqueRepository;
import br.com.fiap.citizentech.repository.TentativaEmbarqueRepository;
import lombok.RequiredArgsConstructor;

/**
 * Avisa os motoristas sobre uma nova solicitacao. Chamado pelo consumidor do RabbitMQ.
 * <p>
 * Registra uma {@link TentativaEmbarque} PENDENTE para a linha pedida e muda a solicitacao
 * para MOTORISTA_AVISADO. Sem a previsao de chegada da Olho Vivo, o alerta vai para a linha toda
 * (veiculo NULL, como preve a RNF10) e os apps dos motoristas da linha exibem o pedido.
 * <p>
 * Idempotente: o RabbitMQ pode entregar a mesma mensagem mais de uma vez, entao uma
 * solicitacao que ja saiu de CRIADA e ignorada.
 */
@Service
@RequiredArgsConstructor
public class AvisoMotoristaService {

	private static final Logger log = LoggerFactory.getLogger(AvisoMotoristaService.class);

	private final SolicitacaoEmbarqueRepository solicitacaoRepository;
	private final TentativaEmbarqueRepository tentativaRepository;

	@Transactional
	public void avisar(Long solicitacaoId) {
		SolicitacaoEmbarque solicitacao = solicitacaoRepository.findById(solicitacaoId).orElse(null);
		if (solicitacao == null) {
			log.warn("Solicitacao {} nao existe mais; evento ignorado", solicitacaoId);
			return;
		}
		if (solicitacao.getStatus() != StatusSolicitacao.CRIADA) {
			log.info("Solicitacao {} ja esta como {}; evento repetido ignorado", solicitacaoId, solicitacao.getStatus());
			return;
		}
		if (solicitacao.getLinha() == null) {
			// Pedido pelo botao, sem cartao: a linha ainda nao e conhecida e o pedido fica para o gestor
			log.info("Solicitacao {} sem linha definida; nenhum motorista avisado", solicitacaoId);
			return;
		}

		TentativaEmbarque tentativa = TentativaEmbarque.builder()
				.solicitacao(solicitacao)
				.linha(solicitacao.getLinha())
				.resultado(ResultadoTentativa.PENDENTE)
				.build();
		tentativaRepository.save(tentativa);

		solicitacao.setStatus(StatusSolicitacao.MOTORISTA_AVISADO);
		log.info("Motoristas da linha {} avisados sobre a solicitacao {}",
				solicitacao.getLinha().getLetreiro(), solicitacaoId);
	}

}
