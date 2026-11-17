# Roteiro da apresentação

## Antes da demonstração

Inicie a API em `dev,ia-local`, [pré-carregue o Qwen3](fluxo-mensagem.md#conferir-no-ambiente-local) e abra o [simulador](http://127.0.0.1:18082/simulador). Confira o volume e a voz do navegador e entre com a conta de demonstração. Deixe o [relatório de avaliação](avaliacao.md) aberto para mostrar a comparação.

## 0:00 a 0:35 · Problema e proposta

“Na parada, o passageiro precisa saber se o pedido foi registrado e onde aguardar. O Parada Certa transforma essa confirmação em uma mensagem curta que pode ser ouvida no totem. Este é o recorte do RF11 e do RNF07.”

## 0:35 a 1:30 · Demonstrar o simulador

1. Escolha **Botão de ajuda** e envie. Leia a confirmação de registro e mostre as referências para botão sem identificação e dados ausentes.
2. Escolha **Cartão · Baixa visão** e envie. Mostre a linha e o destino da API e a referência `PC-04` em **Detalhes técnicos**.
3. Clique em **Ouvir mensagem**. Mostre a origem que a API retornou naquela execução. O navegador reproduz o mesmo texto exibido quando a síntese está disponível.

## 1:30 a 2:15 · Explicar o fluxo

Abra o diagrama no README. O simulador cria a solicitação com JWT. A busca seleciona até três orientações por correspondência textual na base JSON. O Qwen3 redige a instrução de espera e a API valida a frase, compõe o texto com os dados confirmados e persiste a mensagem. O registro posterior do aviso atualiza o estado e o texto da solicitação.

## 2:15 a 3:15 · Apresentar os resultados

Mostre a tabela dos 12 casos autenticados e os 44 testes automatizados. A comparação do cartão de baixa visão encontrou “perto do totem” com a recuperação e “na parada” no controle. Em outro caso de baixa visão, o modelo manteve a orientação genérica apesar da referência específica. Mostre também a primeira carga registrada em 3.027 ms com contingência e a indisponibilidade do provedor em 508 ms. O motivo e a origem permitem explicar o que aconteceu durante a demonstração.

## 3:15 a 3:45 · Encerrar

“A aplicação mantém uma mensagem utilizável quando o modelo falha. O modelo acrescenta uma orientação de espera, e a API controla as informações que o passageiro ouve. A próxima validação seria medir a compreensão em uma parada real.”
