# Mensagem de voz no fluxo de embarque

`POST /api/solicitacoes` continua protegido pelo JWT de gestor. A API monta a primeira frase com o estado `CRIADA` e com o letreiro e o destino cadastrados, quando disponíveis. Neste fluxo, a previsão de chegada ainda não é fornecida à criação do pedido. A mensagem omite horários e minutos. O campo legado `mensagemVoz` continua aceito no corpo do pedido, mas a API usa o texto que ela própria compõe.

O Qwen3 recebe apenas os textos recuperados do RAG. Ele redige a segunda frase, uma instrução de espera em português que pode indicar a parada ou a proximidade do totem, com variações como “espere”, “aguarde” ou “com calma”. A API valida essa forma antes de aceitar a resposta. Texto fora do formato, falha ou espera de 3 segundos pela geração acionam uma orientação de contingência. A aplicação interrompe a espera pela resposta parcial ou completa nesse prazo. A instrução de baixa visão pode ser preservada mesmo na contingência.

A composição limita o texto a duas frases e 300 bytes UTF-8, um limite conservador para a coluna Oracle `VARCHAR2(300)`. A API trata pontuação de fim de frase nos rótulos de linha e destino antes da composição. Se uma combinação exceder o limite, omite primeiro o destino e depois a linha, preservando o estado e a orientação.

O texto é salvo junto do pedido. Depois que o evento registra o aviso, a mensagem do `GET` passa a confirmar o registro do aviso e conserva a orientação de espera validada. Alterações de status pela API atualizam a mensagem com o estado correspondente, por exemplo, `EMBARCADO` gera “Embarque confirmado”. Essas atualizações usam as regras da aplicação e dispensam outra chamada ao modelo.

A resposta de criação traz `geracaoMensagemVoz.origem` (`MODELO` ou `CONTINGENCIA`) e `geracaoMensagemVoz.referencias`, com ID, assunto e origem. Esses metadados descrevem aquela geração e são retornados no `POST`; as consultas devolvem a mensagem armazenada para o estado atual.

## Conferir no ambiente local

Na pasta `citizentech`, com a instância `dev,ia-local` na porta `18082`, execute `./scripts/testar-mensagem-voz.ps1 -Porta 18082`. O teste cria um pedido fictício pelo botão, compara o `POST` ao `GET` e exibe origem, referências, tamanho e tempo. O pedido continua `CRIADA`. O texto confirma o registro e orienta a espera.

Antes da demonstração, é possível carregar o modelo no Ollama para retirar o tempo de carregamento do primeiro pedido.

```powershell
Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:11434/api/generate' `
    -ContentType 'application/json' `
    -Body '{"model":"qwen3:4b-instruct-2507-q4_K_M","keep_alive":"10m"}' | Out-Null
```

## Evidências de 17/11/2026

A suíte Maven passou com 42 testes, incluindo respostas controladas que tentaram atribuir horário, rampa pronta ou aviso ao motorista, destino ausente ou longo demais, persistência da mensagem e atualizações de status.

Os quatro testes de criação abaixo foram executados com JWT de gestor e Qwen3 local. Todos retornaram HTTP 201. O tempo inclui o `POST` inteiro. O prazo de três segundos cobre a espera pela geração; login e consultas de conferência foram medidos fora desse tempo.

| Cenário | Origem | Referências | Bytes UTF-8 | Tempo do `POST` | Mensagem na criação |
| --- | --- | --- | ---: | ---: | --- |
| Botão com o modelo descarregado | `CONTINGENCIA` | `PC-03`, `PC-01`, `PC-02` | 49 | 3.188 ms | Pedido de embarque registrado. Aguarde na parada. |
| Botão sem identificação nem linha, com modelo carregado | `MODELO` | `PC-03`, `PC-01`, `PC-02` | 59 | 480 ms | Pedido de embarque registrado. Aguarde com calma na parada. |
| Cartão de baixa visão com linha e destino | `MODELO` | `PC-01`, `PC-04` | 112 | 386 ms | Pedido de embarque registrado para a linha 6091-10, sentido Terminal Varginha. Aguarde com calma perto do totem. |
| Linha sem destino, com texto legado enviado pelo cliente | `MODELO` | `PC-01`, `PC-02`, `PC-03` | 80 | 312 ms | Pedido de embarque registrado para a linha 7012-10. Aguarde com calma na parada. |

Nos pedidos sem linha, o `GET` preservou a mensagem de criação e o status `CRIADA`. Nos pedidos com linha, depois do aviso o `GET` devolveu `MOTORISTA_AVISADO` e uma primeira frase iniciada por “Aviso de embarque registrado”, conservando a orientação. Após o `PATCH` de confirmação do cartão, o `GET` devolveu `EMBARCADO` e “Embarque confirmado para a linha 6091-10, sentido Terminal Varginha.”

Na primeira execução desta etapa, somente o timeout configurado no cliente permitiu que o pedido a frio esperasse 9.535 ms. Após aplicar o prazo na espera pela geração, o log registrou `origem=CONTINGENCIA motivo=tempo_limite`. O total de 3.188 ms inclui o trabalho da API antes e depois da geração.
