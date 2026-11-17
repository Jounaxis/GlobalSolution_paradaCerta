const cenarios = [
  {
    id: 'botao', nome: 'Botão de ajuda', sinal: 'BOTAO',
    descricao: 'Pedido sem passageiro identificado, linha ou destino.',
  },
  {
    id: 'baixa-visao', nome: 'Cartão · Baixa visão', sinal: 'CARTAO',
    necessidade: 'BAIXA_VISAO', linha: '6091-10',
    descricao: 'Viagem na linha 6091-10 para Terminal Varginha. A orientação pode mencionar o totem.',
  },
  {
    id: 'cadeira', nome: 'Cartão · Cadeira de rodas', sinal: 'CARTAO',
    necessidade: 'CADEIRA_DE_RODAS', linha: '6091-10',
    descricao: 'Viagem para Terminal Varginha. A solicitação chega sem confirmação de rampa.',
  },
  {
    id: 'mobilidade', nome: 'Cartão · Mobilidade reduzida', sinal: 'CARTAO',
    necessidade: 'MOBILIDADE_REDUZIDA', linha: '7012-10',
    descricao: 'O letreiro 7012-10 está cadastrado sem destino.',
  },
  {
    id: 'idoso', nome: 'Cartão · Passageiro idoso', sinal: 'CARTAO',
    necessidade: 'IDOSO', linha: '875A-10',
    descricao: 'Viagem na linha 875A-10 para Aclimação.',
  },
];

const porId = id => document.getElementById(id);
const login = porId('login');
const embarque = porId('embarque');
const seletor = porId('cenario');
const entrar = porId('entrar');
const contaDev = porId('conta-dev');
const enviar = porId('enviar');
const sair = porId('sair');
const ouvir = porId('ouvir');
const estadoAudio = porId('estado-audio');
const temSintese = 'speechSynthesis' in window && 'SpeechSynthesisUtterance' in window;
const instrucaoInicial = porId('mensagem').textContent.trim();

let sessao;
let tentativa;
let resposta;
let falaAtual;

for (const cenario of cenarios) {
  seletor.add(new Option(cenario.nome, cenario.id));
}

function cenarioSelecionado() {
  return cenarios.find(cenario => cenario.id === seletor.value);
}

function atualizarCenario() {
  porId('descricao-cenario').textContent = cenarioSelecionado().descricao;
  tentativa = undefined;
}

function mostrarErro(mensagem = '') {
  const caixa = porId('erro');
  caixa.textContent = mensagem;
  caixa.hidden = !mensagem;
}

async function chamarApi(caminho, { metodo = 'GET', corpo, token = sessao?.token } = {}) {
  const headers = { Accept: 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  if (corpo) headers['Content-Type'] = 'application/json';
  const http = await fetch(caminho, {
    method: metodo,
    headers,
    body: corpo && JSON.stringify(corpo),
    signal: AbortSignal.timeout(15000),
  });
  const dados = await http.json().catch(() => null);
  if (!http.ok) {
    throw Object.assign(new Error(dados?.mensagem || `A API retornou HTTP ${http.status}.`), { status: http.status });
  }
  return dados;
}

function pararFala() {
  falaAtual = undefined;
  if (temSintese) window.speechSynthesis.cancel();
  estadoAudio.textContent = temSintese ? '' : 'A síntese de voz não está disponível neste navegador.';
}

function mostrarResposta(pedido) {
  pararFala();
  resposta = pedido;
  const geracao = pedido?.geracaoMensagemVoz;
  porId('mensagem').textContent = pedido?.mensagemVoz || instrucaoInicial;
  ouvir.disabled = !temSintese || !pedido?.mensagemVoz;
  porId('status-pedido').hidden = !pedido;
  porId('status-pedido').textContent = pedido?.status === 'CRIADA' ? 'Pedido registrado' : pedido?.status || '';
  porId('pedido-id').textContent = pedido ? `#${pedido.id}` : '—';
  const origens = { MODELO: 'Modelo (MODELO)', CONTINGENCIA: 'Frase padrão (CONTINGENCIA)' };
  porId('origem').textContent = origens[geracao?.origem] || (pedido ? 'Origem indisponível' : '—');

  const lista = porId('referencias');
  lista.replaceChildren();
  for (const referencia of geracao?.referencias ?? []) {
    const item = document.createElement('li');
    const titulo = document.createElement('strong');
    const fonte = document.createElement('small');
    titulo.textContent = `${referencia.id} · ${referencia.assunto}`;
    fonte.textContent = referencia.origem;
    item.append(titulo, fonte);
    lista.append(item);
  }
  lista.hidden = !lista.childElementCount;
  porId('refs-vazias').hidden = !lista.hidden;
  porId('refs-vazias').textContent = pedido ? 'Nenhuma referência recuperada.' : 'Aguardando solicitação.';
}

function encerrarSessao() {
  sessao = undefined;
  tentativa = undefined;
  embarque.hidden = true;
  login.hidden = false;
  sair.hidden = true;
  porId('estado-sessao').textContent = 'Sessão inativa';
  mostrarResposta(undefined);
  porId('email').focus();
}

async function carregarDadosDev(token) {
  const [totens, linhas, usuarios] = await Promise.all([
    chamarApi('/api/totens?size=100', { token }),
    chamarApi('/api/linhas?size=100', { token }),
    chamarApi('/api/usuarios?size=100', { token }),
  ]);
  const totem = totens.content.find(item => item.identificador === 'TOTEM-0001' && item.status === 'ATIVO');
  const cuidador = usuarios.content.find(item => item.email === 'cuidador@paradacerta.com' && item.perfil === 'CUIDADOR');
  if (!totem || !cuidador) throw new Error('Dados de demonstração ausentes. Confira se a API está no perfil dev.');
  return { totemId: totem.id, cuidadorId: cuidador.id, linhas: new Map(linhas.content.map(item => [item.letreiro, item.id])) };
}

login.addEventListener('submit', async event => {
  event.preventDefault();
  if (entrar.disabled) return;
  mostrarErro();
  entrar.disabled = contaDev.disabled = true;
  entrar.textContent = 'Entrando…';
  const corpo = { email: porId('email').value.trim(), senha: porId('senha').value };
  try {
    const acesso = await chamarApi('/auth/login', { metodo: 'POST', corpo, token: null });
    if (acesso.usuario.perfil !== 'GESTOR') throw new Error('Use uma conta de gestor para criar solicitações.');
    sessao = { token: acesso.token, ...await carregarDadosDev(acesso.token) };
    login.hidden = true;
    embarque.hidden = false;
    sair.hidden = false;
    porId('estado-sessao').textContent = 'Gestor autenticado';
    seletor.focus();
  } catch (erro) {
    mostrarErro(erro.name === 'TimeoutError' ? 'A API demorou para responder. Tente novamente.' : erro.message);
  } finally {
    porId('senha').value = '';
    entrar.disabled = contaDev.disabled = false;
    entrar.textContent = 'Entrar';
  }
});

contaDev.addEventListener('click', () => {
  porId('email').value = 'gestor@paradacerta.com';
  porId('senha').value = 'Gestor@123';
  login.requestSubmit();
});

sair.addEventListener('click', () => {
  mostrarErro();
  encerrarSessao();
});

seletor.addEventListener('change', atualizarCenario);
atualizarCenario();
mostrarResposta(undefined);

embarque.addEventListener('submit', async event => {
  event.preventDefault();
  if (!sessao || enviar.disabled) return;
  mostrarErro();
  const cenario = cenarioSelecionado();
  const linhaId = cenario.linha && sessao.linhas.get(cenario.linha);
  if (cenario.linha && !linhaId) {
    mostrarErro(`A linha de demonstração ${cenario.linha} não foi encontrada.`);
    return;
  }
  enviar.disabled = seletor.disabled = sair.disabled = true;
  enviar.textContent = 'Criando solicitação…';
  try {
    tentativa ??= { chaveIdempotencia: `totem-demo-${crypto.randomUUID()}` };
    if (cenario.necessidade && !tentativa.passageiroId) {
      const passageiro = await chamarApi('/api/passageiros', {
        metodo: 'POST',
        corpo: {
          cuidadorId: sessao.cuidadorId,
          nome: `Passageiro fictício (${cenario.nome})`,
          tipoNecessidade: cenario.necessidade,
          contatoEmergencia: '00000000000',
          consentimento: true,
          versaoTermo: 'demo',
        },
      });
      tentativa.passageiroId = passageiro.id;
    }
    const pedido = await chamarApi('/api/solicitacoes', {
      metodo: 'POST',
      corpo: { ...tentativa, tipoSinal: cenario.sinal, totemId: sessao.totemId, linhaId },
    });
    tentativa = undefined;
    mostrarResposta(pedido);
  } catch (erro) {
    if (erro.status === 401) {
      encerrarSessao();
      mostrarErro('A sessão expirou. Entre novamente para continuar.');
    } else if (erro.name === 'TimeoutError') {
      mostrarErro('A API demorou para responder. Tente novamente para usar a mesma chave do pedido.');
    } else {
      mostrarErro(erro.message);
    }
  } finally {
    enviar.disabled = seletor.disabled = sair.disabled = false;
    enviar.textContent = 'Criar solicitação';
  }
});

ouvir.addEventListener('click', () => {
  if (!temSintese || !resposta?.mensagemVoz) return;
  pararFala();
  const sintese = window.speechSynthesis;
  const fala = new SpeechSynthesisUtterance(resposta.mensagemVoz);
  falaAtual = fala;
  fala.lang = 'pt-BR';
  const vozes = sintese.getVoices();
  const voz = vozes.find(item => item.lang.toLowerCase() === 'pt-br')
    || vozes.find(item => item.lang.toLowerCase().startsWith('pt'));
  if (voz) fala.voice = voz;
  estadoAudio.textContent = 'Preparando a leitura…';
  fala.onstart = () => { if (falaAtual === fala) estadoAudio.textContent = 'Lendo a mensagem.'; };
  fala.onend = () => { if (falaAtual === fala) estadoAudio.textContent = 'Leitura concluída.'; };
  fala.onerror = () => {
    if (falaAtual === fala) estadoAudio.textContent = 'O navegador não conseguiu ler a mensagem neste dispositivo.';
  };
  sintese.speak(fala);
});
