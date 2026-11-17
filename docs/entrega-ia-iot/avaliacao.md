# Avaliação da mensagem de embarque

A avaliação executa 12 cenários pelo endpoint autenticado `POST /api/solicitacoes` e verifica o texto no `GET`. As entradas usam dados fictícios. O mesmo Qwen3 local roda em três comparações com o RAG desativado. As respostas completas e os critérios por cenário são registrados em JSON.

## Critérios

| Critério | Como foi verificado |
| --- | --- |
| Recuperação | Os IDs recuperados correspondem à lista esperada, na ordem esperada. Na comparação sem RAG, a lista esperada é vazia. |
| Fidelidade | A primeira frase confirma o estado registrado, usa linha e destino quando presentes e omite previsão. A orientação de espera corresponde ao formato aceito, que não permite afirmar fatos sobre a chegada, a rampa ou o motorista. |
| Clareza | A mensagem confirma o pedido e traz uma instrução de espera, com palavras comuns. O script confere a estrutura, e a interpretação considera o texto observado e a utilidade da orientação. |
| Tamanho | Até duas frases e 300 bytes UTF-8. O JSON também registra a quantidade de caracteres. |
| Persistência | A consulta encontra o texto do estado atual. O caso de confirmação verifica o texto persistido após o `PATCH` de embarque. |
| Origem | A API devolve `MODELO` ou `CONTINGENCIA`. O cenário de timeout e o de endpoint indisponível devem usar a contingência. |
| Tempo | O cronômetro mede o `POST` completo, incluindo geração, validação, banco e registro do evento. A espera pela geração é limitada a 3 segundos. A carga do modelo é explicitada em cada condição. |

Os testes automatizados do Maven usam respostas controladas ou endpoints de teste indisponíveis. Eles verificam a busca para todas as necessidades, a consulta fora do vocabulário, a injeção dos trechos, as regras do texto e as transições. Os casos abaixo são uma execução com a API Java e o provedor local, incluindo as falhas provocadas.

## Reproduzir a execução

Use JDK 21, Maven Wrapper, PowerShell 7 e o [modelo local configurado](preparacao.md#reproduzir-a-geração-com-o-modelo-local). O script usa `127.0.0.1`, autentica com a conta de demonstração e cria os dados fictícios necessários no H2. Inicie três cópias da API em terminais separados com `SPRING_PROFILES_ACTIVE=dev,ia-local` e `SERVER_ADDRESS=127.0.0.1`. Em cada terminal, aplique os valores da tabela antes de rodar `.\mvnw.cmd spring-boot:run`.

| Instância | `SERVER_PORT` | `CITIZENTECH_IA_RAG_HABILITADO` | `OLLAMA_BASE_URL` |
| --- | ---: | --- | --- |
| Principal | `18082` | `true` | `http://127.0.0.1:11434/v1` |
| Controle sem RAG | `18084` | `false` | `http://127.0.0.1:11434/v1` |
| Provedor indisponível | `18083` | `true` | `http://127.0.0.1:9/v1` |

O endereço na porta 9 deve estar livre. Com as APIs prontas, na pasta `citizentech`, execute:

```powershell
.\scripts\avaliar-mensagem-voz.ps1 -Saida "$env:TEMP\parada-certa-avaliacao.json"
```

O script carrega o modelo para os casos normais e o descarrega para provocar o cenário de primeira carga. Em uma máquina que carregue o modelo dentro do prazo, esse cenário pode não acionar a contingência. As três comparações sem busca usam as mesmas entradas de botão, baixa visão e mobilidade reduzida, com o modelo carregado e as mesmas regras da API.

## Resultado em 17/11/2026

Ambiente com Qwen3 4B Instruct 2507 em Q4_K_M, Ollama 0.40.16, Radeon RX 7600 via Vulkan e a API no perfil `dev,ia-local`. A suíte Maven obteve 44 aprovações. A [saída real em JSON](resultados-avaliacao.json) inclui entradas, frases exatas, referências com fontes, resultado de cada critério, tempos e texto encontrado no `GET`.

| ID | Cenário | Referências recuperadas | Origem | Bytes | `POST` (ms) |
| --- | --- | --- | --- | ---: | ---: |
| 01 | Botão sem identificação | `PC-03`, `PC-01`, `PC-02` | Modelo | 59 | 387 |
| 02 | Cartão de baixa visão | `PC-01`, `PC-04` | Modelo | 112 | 347 |
| 03 | Cartão de cadeira de rodas | `PC-01`, `PC-05` | Modelo | 107 | 267 |
| 04 | Mobilidade reduzida sem destino | `PC-01`, `PC-03`, `PC-06` | Modelo | 80 | 306 |
| 05 | Cartão de pessoa idosa | `PC-01`, `PC-07` | Contingência | 99 | 300 |
| 06 | Botão com linha confirmada | `PC-01`, `PC-02` | Modelo | 107 | 300 |
| 07 | Baixa visão sem linha | `PC-03`, `PC-01`, `PC-04` | Modelo | 59 | 317 |
| 08 | Linha e destino fora do corpus | `PC-01`, `PC-02` | Modelo | 108 | 348 |
| 09 | Entrada legada fora do corpus e previsão inventada | `PC-03`, `PC-01`, `PC-02` | Modelo | 59 | 312 |
| 10 | Modelo descarregado | `PC-03`, `PC-01`, `PC-02` | Contingência | 49 | 3.027 |
| 11 | Endpoint do provedor indisponível | `PC-03`, `PC-01`, `PC-02` | Contingência | 49 | 508 |
| 12 | Aviso e confirmação de embarque | `PC-01`, `PC-05` | Modelo | 107 | 660 |

Os 12 pedidos retornaram HTTP 201, recuperaram as referências esperadas e passaram nos critérios automatizados da tabela. Todos produziram duas frases com 49 a 112 bytes. O Qwen3 forneceu a orientação aceita em nove dos dez casos com o modelo disponível. O caso 05 caiu na contingência por resposta fora do formato (`resposta_invalida`). O caso 10 registrou `tempo_limite`. O HTTP 201 em 3.027 ms inclui o trabalho da API além dos três segundos de espera pelo modelo. O caso 11 preservou a mensagem mesmo com a porta do provedor sem serviço.

No caso 08, a API usou a linha `042X-10` e o destino `Terminal Horizonte` do cadastro fictício, e o RAG recuperou as orientações gerais do botão. No caso 09, a mensagem foi “Pedido de embarque registrado. Aguarde com calma na parada.”, sem o saldo nem o horário do texto enviado pelo cliente. O caso 12 passou de `CRIADA` para `MOTORISTA_AVISADO` e, após a confirmação, o `GET` devolveu `EMBARCADO` com “Embarque confirmado para a linha 6091-10, sentido Terminal Varginha.”

## Comparação com e sem RAG

As três execuções sem RAG retornaram HTTP 201, `origem=MODELO` e lista de referências vazia. A primeira frase manteve os mesmos fatos em cada par. As diferenças na segunda frase foram:

| Cenário | Com RAG | Sem RAG |
| --- | --- | --- |
| Botão sem identificação | Aguarde com calma na parada. | Aguarde com calma na parada. |
| Cartão de baixa visão | Aguarde com calma perto do totem. | Aguarde com calma na parada. |
| Mobilidade reduzida sem destino | Aguarde com calma na parada. | Aguarde com calma na parada. |

O efeito observado foi uma referência espacial mais específica no cartão de baixa visão. No caso de baixa visão sem linha (`07`), o trecho `PC-04` também foi recuperado, mas a resposta ficou genérica. A presença da orientação no prompt amplia a informação disponível ao modelo, e a escolha da frase continua variável. A fidelidade a linha, destino e status vem da composição pela API.

## Limitações da avaliação

- A execução fez uma passagem por cenário nesta máquina. Os tempos dos casos normais variaram de 267 a 660 ms. O controle sem RAG começou em outra JVM, de modo que esses tempos servem para reproduzir a execução observada e não para atribuir diferenças de velocidade à busca.
- A verificação de clareza usa um critério estrutural. A amostra não mede compreensão com passageiros nem inteligibilidade do áudio em uma parada real.
- O fluxo inicial não fornece previsão confirmada. Os casos cobrem a omissão de previsão e o descarte de um horário inventado no payload.
- A orientação específica apareceu em uma das duas entradas de baixa visão com RAG. Uma dessas entradas foi comparada com a versão sem busca.
