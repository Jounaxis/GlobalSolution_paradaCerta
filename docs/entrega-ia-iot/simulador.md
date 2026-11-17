# Simulador da parada

Com a API executando em `dev,ia-local`, acesse [o simulador local](http://127.0.0.1:18082/simulador). A página e seus recursos são servidos pela própria API no perfil `dev`. O [roteiro do modelo local](preparacao.md#reproduzir-a-geração-com-o-modelo-local) descreve a inicialização da API e do Ollama.

1. Selecione **Usar conta de demonstração** para entrar com o gestor fictício do perfil `dev`. Também é possível fazer login informando o e-mail e senha de um gestor desse ambiente.
2. Selecione um cenário e clique em **Criar solicitação**. O simulador usa o JWT para enviar o pedido ao endpoint normal de embarque.
3. Confira a mensagem retornada e clique em **Ouvir mensagem**. O navegador tenta usar uma voz em português do Brasil. A reprodução depende da síntese de voz disponível no dispositivo.
4. Em **Detalhes técnicos**, confira o ID do pedido, a origem `MODELO` ou `CONTINGENCIA` e os IDs, assuntos e fontes das referências recuperadas.

## Cenários fictícios

| Cenário | Dados enviados à API | Referências esperadas |
| --- | --- | --- |
| Botão de ajuda | Sem passageiro, linha ou destino | `PC-03`, `PC-01`, `PC-02` |
| Cartão · Baixa visão | Linha `6091-10`, Terminal Varginha | `PC-01`, `PC-04` |
| Cartão · Cadeira de rodas | Linha `6091-10`, Terminal Varginha | `PC-01`, `PC-05` |
| Cartão · Mobilidade reduzida | Linha `7012-10`, sem destino | `PC-01`, `PC-03`, `PC-06` |
| Cartão · Passageiro idoso | Linha `875A-10`, Aclimação | `PC-01`, `PC-07` |

Cada teste com cartão cria um passageiro fictício vinculado ao cuidador de demonstração no banco em memória. Isso permite repetir o cenário sem esbarrar na regra de uma solicitação ativa por passageiro. O botão funciona sem cadastro. Os pedidos, os IDs e as mensagens são reais para a instância `dev`, e os dados são descartados quando ela reinicia. O JWT fica em memória durante a sessão da página.

## Verificação em 17/11/2026

Os cinco cenários foram executados no navegador com a API Java e o Qwen3 local. A criação retornou um ID e a consulta autenticada encontrou todos os pedidos. As referências exibidas corresponderam à tabela. Na repetição da baixa visão, a API criou um novo pedido para outro passageiro fictício. A síntese de voz informou “Leitura concluída” no cenário do botão. Ao sair, a página voltou ao login e limpou a mensagem exibida.

| Cenário | ID no ambiente de teste | Origem observada |
| --- | ---: | --- |
| Botão de ajuda | 1 | `MODELO` |
| Cartão · Baixa visão | 2 | `MODELO` |
| Cartão · Cadeira de rodas | 3 | `MODELO` |
| Cartão · Mobilidade reduzida | 4 | `MODELO` |
| Cartão · Passageiro idoso | 5 | `CONTINGENCIA` |
| Cartão · Baixa visão, repetição | 6 | `MODELO` |

No cenário de passageiro idoso, o log marcou `motivo=resposta_invalida`. O simulador exibiu a frase padrão e as referências recuperadas. A frase confirmou o pedido para a linha `875A-10` e orientou a espera na parada.

A suíte Maven passou com 43 testes. O acesso à página autenticado fora de `dev` e a criação do pedido com papel `CUIDADOR` receberam HTTP 403. Na instância `dev`, a página recebeu HTTP 200 e o `POST` sem JWT recebeu HTTP 401.
