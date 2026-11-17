# Recuperação de orientações para a mensagem de embarque

Cada trecho na [base de orientações](../../citizentech/src/main/resources/rag/orientacoes-embarque.json) tem ID, assunto, origem, termos de busca e texto. A base resume as passagens da documentação do Parada Certa sobre registro do pedido, linguagem simples, botão sem cartão, dados ausentes e as quatro necessidades cadastradas. A origem de cada trecho indica as páginas usadas. Os textos orientam a comunicação e não fornecem dados sobre um pedido específico.

## Como a recuperação funciona

1. Na criação de um pedido, a consulta inclui a expressão de confirmação e, conforme os campos disponíveis, a necessidade cadastrada ou o botão sem identificação, a ausência de linha e a ausência de destino.
2. O recuperador normaliza maiúsculas, acentos e separadores. Cada expressão distinta em comum entre a consulta e os termos de um trecho vale um ponto.
3. Seleciona até três trechos com pontuação positiva. A pontuação mais alta vem primeiro e o ID desempata. Uma consulta sem expressão correspondente devolve lista vazia.
4. O Spring AI recebe o texto dos trechos selecionados, ao lado dos dados de viagem usados pelo prompt. O tipo de necessidade fica no backend e orienta a recuperação. Cada referência pode ser conferida no log `DEBUG` da API.

Com sete trechos e termos derivados de campos estruturados, a busca por expressão permite prever e verificar a ordem das referências sem adicionar um serviço de embeddings ou um índice externo. A base tem vocabulário controlado. Sinônimos ausentes dos termos cadastrados podem ficar sem correspondência.

## Conferir os trechos

Inicie a API com `dev,ia-local` e `LOGGING_LEVEL_BR_COM_FIAP_CITIZENTECH_AI=DEBUG`, como descrito no [roteiro do modelo local](preparacao.md#reproduzir-a-geração-com-o-modelo-local). Na pasta `citizentech`, rode `./scripts/testar-mensagem-voz.ps1 -Porta 18082`. O teste usa o JWT do gestor fictício e verifica o texto persistido.

Para o botão sem passageiro e sem linha, procure `Orientacoes RAG recuperadas` no log. A ordem esperada é `PC-03`, `PC-01`, `PC-02`. A mesma linha registra o assunto, a origem e o texto de cada trecho. A linha `Mensagem de voz: origem=MODELO` indica que o modelo retornou o texto; o HTTP 201 indica que o pedido foi criado.

## Testes automatizados

A suíte Maven passou com 33 testes. O recuperador foi verificado para as quatro necessidades, o botão sem identificação, a ausência de destino, a ordenação e a consulta fora da base. O teste do `MensagemVozService` usa um `ChatClient` controlado e confere o texto injetado no prompt e a ausência do campo de tipo de necessidade.

## Execução com o modelo real em 17/11/2026

Os dois pedidos fictícios foram enviados à API autenticada no perfil `dev,ia-local`, com o Qwen3 4B no Ollama. Ambos retornaram HTTP 201 e preservaram a mensagem no `GET`. O log registrou `origem=MODELO` em ambos.

| Cenário | Referências recuperadas | Caracteres | Tempo do `POST` | Mensagem persistida |
| --- | --- | ---: | ---: | --- |
| Botão sem identidade, linha ou destino | `PC-03`, `PC-01`, `PC-02` | 75 | 821 ms | Pedido de embarque registrado. Aguarde na parada Av. Paulista, 1578 - MASP. |
| Cartão, baixa visão, linha 6091-10 e Terminal Varginha | `PC-01`, `PC-04` | 68 | 533 ms | Pedido de embarque registrado. Linha 6091-10 para Terminal Varginha. |

O primeiro pedido recebeu orientação geral de espera e continuou com status `CRIADA`. No segundo, o `POST` retornou `CRIADA` e o `GET` retornou `MOTORISTA_AVISADO` após o evento local. O modelo preservou linha e destino, mas omitiu a orientação de espera perto do totem presente no trecho `PC-04`. A recuperação pode ser verificada no log mesmo quando a geração omite parte do conteúdo recuperado.
