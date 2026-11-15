package br.com.fiap.citizentech.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Onibus (tabela VEICULO).
 * Simplificacao do MVP: 1 veiculo = 1 linha fixa e 1 motorista fixo.
 */
@Entity
@Table(name = "veiculo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Veiculo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	/** Prefixo do onibus na API Olho Vivo (campo "p"). */
	@Column(name = "prefixo", nullable = false, unique = true, length = 10)
	private String prefixo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "linha_id", nullable = false)
	private Linha linha;

	/** Um motorista dirige no maximo um veiculo por vez (uq_veiculo_motorista). */
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "motorista_id", unique = true)
	private Usuario motorista;

}
