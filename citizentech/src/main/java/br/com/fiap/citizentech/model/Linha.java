package br.com.fiap.citizentech.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Linha de onibus em um sentido (tabela LINHA).
 */
@Entity
@Table(name = "linha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Linha {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	/** Codigo da linha na API Olho Vivo da SPTrans (campo "cl"). */
	@Column(name = "codigo_sptrans", nullable = false, unique = true, precision = 10)
	private Long codigoSptrans;

	/** Letreiro exibido no onibus, ex.: 6091-10. */
	@Column(name = "letreiro", nullable = false, length = 20)
	private String letreiro;

	/** 1 = terminal principal para secundario, 2 = sentido inverso. */
	@Column(name = "sentido", nullable = false, precision = 1)
	private Integer sentido;

	/** Destino falado na mensagem de voz, ex.: Terminal Varginha. */
	@Column(name = "destino", length = 120)
	private String destino;

}
