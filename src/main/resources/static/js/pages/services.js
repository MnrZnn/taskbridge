import { consulta, del, get, post } from '../core/api.js';
import { avatar, confirmar, paginacao, seloCategoria } from '../core/componentes.js';
import { carregando, el, icone, substituir, vazio } from '../core/dom.js';
import { moeda, prazo, status } from '../core/formato.js';
import { avisar, iniciarPagina } from '../core/layout.js';

const usuario = await iniciarPagina({ trilha: [{ texto: 'Início', href: '/' }, { texto: 'Serviços' }] });
const parametros = new URLSearchParams(window.location.search);
const prestador = usuario?.tipoConta === 'PRESTADOR';
let aba = prestador && parametros.get('aba') === 'meus' ? 'meus' : 'catalogo';
let categoriaId = parametros.get('categoriaId') || '';

const categorias = await get('/api/categorias');
const abas = el('ul', { class: 'nav nav-tabs' });
const filtros = el('nav', { class: 'list-group list-group-flush', 'aria-label': 'Categorias' });
const lista = el('div');

substituir('pagina',
    el('div', { class: 'tb-cabecalho-pagina' },
        el('div', {},
            el('h1', { text: 'Serviços' }),
            el('p', { text: 'O que os profissionais oferecem. Para algo sob medida, descreva sua necessidade em um projeto.' })),
        prestador ? el('a', { class: 'btn btn-primary', href: '/services/new' }, icone('plus'), 'Novo serviço') : null),
    prestador ? abas : null,
    el('div', { class: 'row g-4' + (prestador ? ' mt-0' : '') },
        el('aside', { class: 'col-lg-3' }, el('section', { class: 'tb-painel' },
            el('div', { class: 'tb-painel-titulo' }, el('h2', { text: 'Categorias' })), filtros)),
        el('div', { class: 'col-lg-9' }, lista)));

desenharAbas();
desenharFiltros();
carregar(0);

function desenharAbas() {
    if (!prestador) return;
    const botaoAba = (id, rotulo) => el('li', { class: 'nav-item' }, el('button', {
        class: 'nav-link' + (aba === id ? ' active' : ''), type: 'button', text: rotulo, 'aria-selected': String(aba === id),
        on: { click: () => { aba = id; desenharAbas(); carregar(0); } },
    }));
    substituir(abas, botaoAba('catalogo', 'Catálogo'), botaoAba('meus', 'Meus serviços'));
}

function desenharFiltros() {
    const opcao = (id, rotulo, selo) => el('button', {
        type: 'button',
        class: 'list-group-item list-group-item-action d-flex gap-2 align-items-center small' + (String(categoriaId) === String(id) ? ' active' : ''),
        'aria-pressed': String(String(categoriaId) === String(id)),
        on: { click: () => { categoriaId = id; desenharFiltros(); aba = 'catalogo'; desenharAbas(); carregar(0); } },
    }, selo, rotulo);
    substituir(filtros, opcao('', 'Todas as categorias', null),
        categorias.map((categoria) => opcao(categoria.id, categoria.nome, seloPequeno(categoria))));
}

function seloPequeno(categoria) {
    const selo = seloCategoria(categoria);
    selo.classList.add('flex-none');
    selo.style.width = '1.8rem';
    selo.style.height = '1.8rem';
    selo.querySelector('span').style.fontSize = '0.55rem';
    return selo;
}

async function carregar(pagina) {
    substituir(lista, carregando());
    const caminho = aba === 'meus'
        ? `/api/servicos/meus?page=${pagina}&size=10`
        : '/api/servicos' + consulta({ categoriaId, page: pagina, size: 10 });
    try {
        const resultado = await get(caminho);
        substituir(lista, el('section', { class: 'tb-painel' },
            resultado.conteudo.length
                ? el('ul', { class: 'tb-lista' }, resultado.conteudo.map(itemServico))
                : el('div', { class: 'tb-painel-corpo' }, vazio(aba === 'meus' ? 'Você ainda não cadastrou serviços.' : 'Nenhum serviço nesta categoria.',
                    aba === 'meus' ? el('a', { class: 'btn btn-sm btn-primary', href: '/services/new', text: 'Cadastrar serviço' }) : null)),
            paginacao(resultado, carregar)));
    } catch (erro) {
        substituir(lista, el('div', { class: 'alert alert-danger', text: erro.message }));
    }
}

function itemServico(servico) {
    const meu = aba === 'meus';
    return el('li', { class: 'd-flex gap-3' },
        seloCategoria(servico.categoria),
        el('div', { class: 'flex-grow-1 min-w-0' },
            el('div', { class: 'd-flex flex-wrap gap-2 align-items-center' },
                el('strong', { class: 'text-dark', text: servico.titulo }),
                meu ? status(servico.status, servico.status === 'ATIVO' ? 'Ativo' : 'Pausado') : null),
            el('p', { class: 'small text-secondary mb-1 tb-quebra', text: servico.descricao }),
            el('div', { class: 'tb-meta' },
                el('span', { text: 'a partir de ' + moeda(servico.precoBase) }),
                el('span', { text: prazo(servico.prazoDias) }),
                el('span', { text: servico.categoria.nome }))),
        meu ? acoesDoDono(servico) : el('a', {
            class: 'd-flex flex-column align-items-center gap-1 text-decoration-none small text-center', href: `/u/${servico.prestador.nomeUsuario}`,
        }, avatar(servico.prestador), el('span', { class: 'text-truncate', text: servico.prestador.nomeCompleto })));
}

function acoesDoDono(servico) {
    const alternar = el('button', { class: 'btn btn-sm btn-outline-secondary', type: 'button' },
        icone(servico.status === 'ATIVO' ? 'pause' : 'play'), servico.status === 'ATIVO' ? 'Pausar' : 'Reativar');
    alternar.addEventListener('click', async () => {
        await post(`/api/servicos/${servico.id}/${servico.status === 'ATIVO' ? 'pausar' : 'reativar'}`);
        carregar(0);
    });
    const excluir = el('button', { class: 'btn btn-sm btn-outline-danger', type: 'button', 'aria-label': 'Excluir serviço' }, icone('trash-2'));
    excluir.addEventListener('click', async () => {
        if (!await confirmar({ titulo: 'Excluir serviço', mensagem: `Excluir "${servico.titulo}"?`, confirmar: 'Excluir', perigo: true })) return;
        await del(`/api/servicos/${servico.id}`);
        avisar('Serviço excluído.');
        carregar(0);
    });
    return el('div', { class: 'd-flex flex-column gap-1' },
        el('a', { class: 'btn btn-sm btn-outline-secondary', href: `/services/${servico.id}/edit` }, icone('pencil'), 'Editar'),
        alternar, excluir);
}
