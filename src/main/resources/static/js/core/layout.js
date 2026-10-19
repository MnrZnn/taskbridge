import { get, post } from './api.js';
import { el, icone, linkSeguro, renderizarIcones, substituir } from './dom.js';
import { dataHora, iniciais } from './formato.js';

let sessaoAtual;

/** Usuário logado (ou null). A API é a fonte da verdade; o front só decide o que desenhar. */
export function sessao() {
    if (!sessaoAtual) {
        sessaoAtual = get('/api/me').catch((erro) => {
            if (erro.status === 401) return null;
            throw erro;
        });
    }
    return sessaoAtual;
}

/**
 * Monta cabeçalho, trilha e rodapé. Páginas protegidas redirecionam para o login;
 * isso é só conforto de navegação — quem protege os dados é o backend.
 */
export async function iniciarPagina({ protegida = false, tipoConta = null, trilha = [] } = {}) {
    const usuario = await sessao().catch(() => null);
    if (protegida && !usuario) {
        const destino = window.location.pathname + window.location.search;
        window.location.replace('/login?next=' + encodeURIComponent(destino));
        return new Promise(() => {});
    }
    renderizarCabecalho(usuario);
    renderizarTrilha(trilha);
    renderizarRodape();
    renderizarIcones();
    if (tipoConta && usuario && usuario.tipoConta !== tipoConta) {
        const conteudo = document.getElementById('conteudo');
        substituir(conteudo, el('div', { class: 'alert alert-warning mt-3' },
            `Esta área é para contas de ${tipoConta === 'CLIENTE' ? 'cliente' : 'prestador'}.`));
        return new Promise(() => {});
    }
    return usuario;
}

function itensDeNavegacao(usuario) {
    const publicos = [
        { href: '/projects', texto: 'Projetos', icone: 'folder-open' },
        { href: '/services', texto: 'Serviços', icone: 'store' },
    ];
    if (!usuario) return [{ href: '/', texto: 'Início', icone: 'house' }, ...publicos];
    return [
        { href: '/dashboard', texto: 'Painel', icone: 'layout-dashboard' },
        ...publicos,
        { href: '/proposals', texto: 'Propostas', icone: 'send' },
        { href: '/contracts', texto: 'Contratações', icone: 'handshake' },
    ];
}

function linkAtivo(href) {
    const caminho = window.location.pathname;
    return href === '/' ? caminho === '/' : caminho === href || caminho.startsWith(href + '/');
}

function renderizarCabecalho(usuario) {
    const alvo = document.getElementById('tb-cabecalho');
    if (!alvo) return;
    const navegacao = el('ul', { class: 'navbar-nav me-auto' },
        itensDeNavegacao(usuario).map((item) => el('li', { class: 'nav-item' },
            el('a', {
                class: 'nav-link' + (linkAtivo(item.href) ? ' active' : ''),
                href: item.href,
                'aria-current': linkAtivo(item.href) ? 'page' : null,
            }, icone(item.icone), item.texto))));

    const lateral = usuario ? areaDoUsuario(usuario) : el('div', { class: 'd-flex gap-2 py-2 py-lg-0' },
        el('a', { class: 'btn btn-sm btn-outline-secondary', href: '/login' }, 'Entrar'),
        el('a', { class: 'btn btn-sm btn-primary', href: '/register' }, 'Criar conta'));

    substituir(alvo, el('div', { class: 'tb-topo' },
        el('nav', { class: 'navbar navbar-expand-lg navbar-dark', 'aria-label': 'Principal' },
            el('div', { class: 'container-lg' },
                el('a', { class: 'tb-marca me-4', href: usuario ? '/dashboard' : '/' },
                    el('span', { class: 'tb-marca-selo', 'aria-hidden': 'true', text: 'TB' }),
                    'TaskBridge',
                    el('small', { class: 'd-none d-xl-inline', text: 'escopo · match · entrega' })),
                el('button', {
                    class: 'navbar-toggler', type: 'button', 'data-bs-toggle': 'collapse',
                    'data-bs-target': '#tb-menu', 'aria-controls': 'tb-menu', 'aria-expanded': 'false',
                    'aria-label': 'Abrir menu',
                }, icone('menu')),
                el('div', { class: 'collapse navbar-collapse', id: 'tb-menu' }, navegacao, lateral)))));

    if (usuario) atualizarContadorNotificacoes();
}

function areaDoUsuario(usuario) {
    const avatar = usuario.fotoUrl
        ? el('img', { class: 'tb-avatar', src: usuario.fotoUrl, alt: '' })
        : el('span', { class: 'tb-avatar', 'aria-hidden': 'true', text: iniciais(usuario.nomeCompleto) });

    const novoProjeto = usuario.tipoConta === 'CLIENTE'
        ? el('li', { class: 'nav-item me-lg-2 py-2 py-lg-0' },
            el('a', { class: 'btn btn-sm btn-primary', href: '/projects/new' }, icone('plus'), 'Novo projeto'))
        : null;

    const listaNotificacoes = el('div', { class: 'tb-notificacoes-lista' });
    const sino = el('li', { class: 'nav-item dropdown tb-sino' },
        el('a', {
            class: 'nav-link', href: '#', role: 'button', 'data-bs-toggle': 'dropdown', 'aria-expanded': 'false',
            id: 'tb-sino', on: { 'show.bs.dropdown': () => carregarNotificacoes(listaNotificacoes) },
        }, icone('bell', 'Notificações'), el('span', { class: 'tb-sino-contador', id: 'tb-sino-contador', hidden: true })),
        el('div', { class: 'dropdown-menu dropdown-menu-end' },
            el('h6', { class: 'dropdown-header', text: 'Notificações' }),
            listaNotificacoes,
            el('a', { class: 'dropdown-item border-top justify-content-center small', href: '/notifications', text: 'Ver todas' })));

    const menuConta = el('li', { class: 'nav-item dropdown' },
        el('a', {
            class: 'nav-link dropdown-toggle', href: '#', role: 'button', 'data-bs-toggle': 'dropdown', 'aria-expanded': 'false',
        }, avatar, el('span', { class: 'ms-1', text: '@' + usuario.nomeUsuario })),
        el('ul', { class: 'dropdown-menu dropdown-menu-end' },
            el('li', {}, el('h6', { class: 'dropdown-header', text: usuario.tipoConta === 'CLIENTE' ? 'Conta de cliente' : 'Conta de prestador' })),
            el('li', {}, el('a', { class: 'dropdown-item', href: '/profile' }, icone('user'), 'Meu perfil')),
            el('li', {}, el('a', { class: 'dropdown-item', href: '/settings' }, icone('settings'), 'Configurações')),
            el('li', {}, el('a', { class: 'dropdown-item', href: '/security' }, icone('shield-check'), 'Segurança')),
            el('li', {}, el('hr', { class: 'dropdown-divider m-0' })),
            el('li', {}, el('button', { class: 'dropdown-item', type: 'button', on: { click: sair } }, icone('log-out'), 'Sair'))));

    return el('ul', { class: 'navbar-nav align-items-lg-center' }, novoProjeto, sino, menuConta);
}

async function atualizarContadorNotificacoes() {
    try {
        const { total } = await get('/api/notificacoes/nao-lidas');
        const contador = document.getElementById('tb-sino-contador');
        if (contador && total > 0) {
            contador.textContent = total > 9 ? '9+' : String(total);
            contador.hidden = false;
        }
    } catch {
        // contador é acessório; a página continua funcionando sem ele
    }
}

async function carregarNotificacoes(lista) {
    lista.replaceChildren(el('div', { class: 'tb-carregando', text: 'Carregando…' }));
    try {
        const pagina = await get('/api/notificacoes?size=5');
        if (!pagina.conteudo.length) {
            substituir(lista, el('div', { class: 'px-3 py-3 small text-secondary', text: 'Nenhuma notificação.' }));
            return;
        }
        substituir(lista, pagina.conteudo.map((notificacao) => el('a', {
            class: 'dropdown-item d-block small' + (notificacao.lida ? ' text-secondary' : ' fw-semibold'),
            href: linkSeguro(notificacao.link || '/notifications'),
        }, el('div', { class: 'text-wrap', text: notificacao.mensagem }),
            el('div', { class: 'tb-meta', text: dataHora(notificacao.criadaEm) }))));
    } catch {
        substituir(lista, el('div', { class: 'px-3 py-3 small text-danger', text: 'Não foi possível carregar.' }));
    }
}

export async function sair() {
    try {
        await post('/api/auth/logout');
    } finally {
        window.location.assign('/');
    }
}

function renderizarTrilha(trilha) {
    if (!trilha.length) return;
    const cabecalho = document.getElementById('tb-cabecalho');
    const itens = trilha.map((item, indice) => {
        const ultimo = indice === trilha.length - 1;
        return el('li', { class: 'breadcrumb-item' + (ultimo ? ' active' : ''), 'aria-current': ultimo ? 'page' : null },
            ultimo || !item.href ? item.texto : el('a', { href: item.href, text: item.texto }));
    });
    cabecalho.append(el('div', { class: 'tb-trilha' },
        el('div', { class: 'container-lg' },
            el('nav', { 'aria-label': 'Trilha de navegação' }, el('ol', { class: 'breadcrumb', id: 'tb-trilha' }, itens)))));
}

/** Permite à página trocar o último item da trilha depois de carregar o recurso (ex.: título do projeto). */
export function atualizarTrilha(texto) {
    const ultimo = document.querySelector('#tb-trilha .breadcrumb-item.active');
    if (ultimo) ultimo.textContent = texto;
}

function renderizarRodape() {
    const alvo = document.getElementById('tb-rodape');
    if (!alvo) return;
    substituir(alvo, el('div', { class: 'tb-rodape' },
        el('div', { class: 'container-lg' },
            el('div', { class: 'd-flex flex-wrap justify-content-between gap-3' },
                el('div', {},
                    el('strong', { class: 'text-white', text: 'TaskBridge' }),
                    el('div', { text: 'Da necessidade à entrega, com escopo claro.' })),
                el('ul', { class: 'tb-rodape-links' },
                    el('li', {}, el('a', { href: '/projects', text: 'Projetos' })),
                    el('li', {}, el('a', { href: '/services', text: 'Serviços' })),
                    el('li', {}, el('a', { href: '/privacy', text: 'Política de privacidade' })),
                    el('li', {}, el('a', { href: '/terms', text: 'Termos de uso' })))),
            el('div', { class: 'tb-rodape-aviso', text:
                'Projeto acadêmico. Pagamentos PIX funcionam apenas em modo de teste — nenhum valor real é cobrado.' }))));
}

// ------------------------------------------------------------------ avisos (toasts)

export function avisar(mensagem, tipo = 'info') {
    let area = document.querySelector('.tb-avisos');
    if (!area) {
        area = el('div', { class: 'tb-avisos', role: 'status', 'aria-live': 'polite' });
        document.body.append(area);
    }
    const aviso = el('div', { class: 'tb-aviso', dataset: { tipo }, text: mensagem });
    area.append(aviso);
    setTimeout(() => aviso.remove(), 5000);
}

// ------------------------------------------------------------------ erros em formulários

export function limparErros(formulario) {
    formulario.querySelectorAll('.is-invalid').forEach((campo) => campo.classList.remove('is-invalid'));
    formulario.querySelectorAll('.invalid-feedback[data-gerado]').forEach((feedback) => feedback.remove());
    const alerta = formulario.querySelector('[data-erro-formulario]');
    if (alerta) {
        alerta.hidden = true;
        alerta.replaceChildren();
    }
}

export function mostrarErro(formulario, erro) {
    limparErros(formulario);
    Object.entries(erro.campos || {}).forEach(([nome, mensagem]) => {
        const campo = formulario.querySelector(`[name="${CSS.escape(nome)}"]`);
        if (!campo) return;
        campo.classList.add('is-invalid');
        campo.insertAdjacentElement('afterend', el('div', { class: 'invalid-feedback', dataset: { gerado: '1' }, text: mensagem }));
    });
    const alerta = formulario.querySelector('[data-erro-formulario]');
    if (alerta) {
        substituir(alerta, el('strong', { text: erro.message }),
            erro.detalhes?.length ? el('ul', {}, erro.detalhes.map((detalhe) => el('li', { text: detalhe }))) : null);
        alerta.hidden = false;
        alerta.focus();
    } else {
        avisar(erro.message, 'erro');
    }
}

/** Desabilita o botão enquanto a requisição roda — evita duplo envio (ex.: duas propostas). */
export async function comBotaoOcupado(botao, acao) {
    const textoOriginal = botao.textContent;
    botao.disabled = true;
    botao.setAttribute('aria-busy', 'true');
    try {
        return await acao();
    } finally {
        botao.disabled = false;
        botao.removeAttribute('aria-busy');
        if (!botao.querySelector('svg')) botao.textContent = textoOriginal;
    }
}
