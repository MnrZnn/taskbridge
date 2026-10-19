import { consulta, get } from '../core/api.js';
import { blocoCompatibilidade, itemProjeto, paginacao, painel } from '../core/componentes.js';
import { carregando, el, icone, substituir, vazio } from '../core/dom.js';
import { iniciarPagina } from '../core/layout.js';

const usuario = await iniciarPagina({ trilha: [{ texto: 'Início', href: '/' }, { texto: 'Projetos' }] });
const parametros = new URLSearchParams(window.location.search);

const abas = [{ id: 'abertos', rotulo: 'Projetos abertos' }];
if (usuario?.tipoConta === 'CLIENTE') abas.push({ id: 'meus', rotulo: 'Meus projetos' });
if (usuario?.tipoConta === 'PRESTADOR') abas.push({ id: 'compativeis', rotulo: 'Compatíveis comigo' });
let abaAtual = abas.some((aba) => aba.id === parametros.get('aba')) ? parametros.get('aba') : 'abertos';

const categorias = await get('/api/categorias');
const conteudo = el('div');
const navegacaoAbas = el('ul', { class: 'nav nav-tabs mb-0', role: 'tablist' });

substituir('pagina',
    el('div', { class: 'tb-cabecalho-pagina' },
        el('div', {},
            el('h1', { text: 'Projetos' }),
            el('p', { text: 'Necessidades publicadas por clientes, já com escopo, orçamento e prazo definidos.' })),
        usuario?.tipoConta === 'CLIENTE' ? el('a', { class: 'btn btn-primary', href: '/projects/new' }, icone('plus'), 'Nova necessidade') : null),
    navegacaoAbas,
    conteudo);

desenharAbas();
mostrarAba(abaAtual);

function desenharAbas() {
    substituir(navegacaoAbas, abas.map((aba) => el('li', { class: 'nav-item', role: 'presentation' },
        el('button', {
            class: 'nav-link' + (aba.id === abaAtual ? ' active' : ''), type: 'button', role: 'tab',
            'aria-selected': String(aba.id === abaAtual), text: aba.rotulo,
            on: { click: () => mostrarAba(aba.id) },
        }))));
}

function mostrarAba(id) {
    abaAtual = id;
    desenharAbas();
    history.replaceState(null, '', id === 'abertos' ? '/projects' : `/projects?aba=${id}`);
    if (id === 'meus') carregarMeus(0);
    else if (id === 'compativeis') carregarCompativeis();
    else desenharBusca();
}

function desenharBusca() {
    const formulario = el('form', { class: 'row g-2 align-items-end tb-painel-corpo border-bottom', role: 'search' },
        el('div', { class: 'col-md-5' },
            el('label', { class: 'form-label', for: 'busca-texto', text: 'Buscar' }),
            el('input', { class: 'form-control', id: 'busca-texto', name: 'texto', maxlength: 100, placeholder: 'Ex.: site, logotipo, planilha', value: parametros.get('texto') || '' })),
        el('div', { class: 'col-md-3' },
            el('label', { class: 'form-label', for: 'busca-categoria', text: 'Categoria' }),
            el('select', { class: 'form-select', id: 'busca-categoria', name: 'categoriaId' },
                el('option', { value: '', text: 'Todas' }),
                categorias.map((categoria) => el('option', { value: categoria.id, text: categoria.nome })))),
        el('div', { class: 'col-md-2' },
            el('label', { class: 'form-label', for: 'busca-orcamento', text: 'Paga ao menos (R$)' }),
            el('input', { class: 'form-control', id: 'busca-orcamento', name: 'orcamentoMinimo', type: 'number', min: 0, step: 100 })),
        el('div', { class: 'col-md-2 d-grid' }, el('button', { class: 'btn btn-dark', type: 'submit' }, icone('search'), 'Buscar')));
    formulario.categoriaId.value = parametros.get('categoriaId') || '';
    const resultados = el('div');
    formulario.addEventListener('submit', (evento) => {
        evento.preventDefault();
        buscarAbertos(formulario, resultados, 0);
    });
    substituir(conteudo, el('section', { class: 'tb-painel border-top-0' }, formulario, resultados));
    buscarAbertos(formulario, resultados, 0);
}

async function buscarAbertos(formulario, alvo, pagina) {
    substituir(alvo, carregando());
    const filtros = {
        texto: formulario.texto.value.trim(),
        categoriaId: formulario.categoriaId.value,
        orcamentoMinimo: formulario.orcamentoMinimo.value,
        page: pagina,
        size: 10,
    };
    try {
        const resultado = await get('/api/projetos' + consulta(filtros));
        substituir(alvo, resultado.conteudo.length
            ? el('ul', { class: 'tb-lista' }, resultado.conteudo.map((projeto) => itemProjeto(projeto, { mostrarStatus: false })))
            : el('div', { class: 'tb-painel-corpo' }, vazio('Nenhum projeto aberto encontrado com esses filtros.')),
        paginacao(resultado, (destino) => buscarAbertos(formulario, alvo, destino)));
    } catch (erro) {
        substituir(alvo, el('div', { class: 'alert alert-danger m-3', text: erro.message }));
    }
}

async function carregarMeus(pagina) {
    substituir(conteudo, carregando());
    const resultado = await get(`/api/projetos/meus?page=${pagina}&size=10`);
    substituir(conteudo, el('section', { class: 'tb-painel border-top-0' },
        resultado.conteudo.length
            ? el('ul', { class: 'tb-lista' }, resultado.conteudo.map((projeto) => itemProjeto(projeto)))
            : el('div', { class: 'tb-painel-corpo' }, vazio('Você ainda não criou projetos.',
                el('a', { class: 'btn btn-sm btn-primary', href: '/projects/new', text: 'Descrever uma necessidade' }))),
        paginacao(resultado, carregarMeus)));
}

async function carregarCompativeis() {
    substituir(conteudo, carregando('Calculando compatibilidade…'));
    const itens = await get('/api/projetos/compativeis');
    if (!itens.length) {
        substituir(conteudo, el('section', { class: 'tb-painel border-top-0' }, el('div', { class: 'tb-painel-corpo' },
            vazio('Sem sugestões ainda. Um perfil profissional completo e serviços ativos melhoram o matching.',
                el('a', { class: 'btn btn-sm btn-primary', href: '/settings?secao=profissional', text: 'Completar perfil' })))));
        return;
    }
    substituir(conteudo, painel('Ordenados por compatibilidade', el('ul', { class: 'tb-lista' }, itens.map((item) => {
        const bloco = blocoCompatibilidade(item.pontuacao, item.criterios);
        return el('li', { class: 'tb-match' },
            bloco.pontuacao,
            el('div', { class: 'min-w-0' },
                el('a', { class: 'tb-lista-titulo', href: `/projects/${item.projeto.id}`, text: item.projeto.titulo }),
                el('div', { class: 'small text-secondary', text: item.projeto.necessidade }),
                bloco.criterios),
            el('div', { class: 'tb-match-acoes' }, item.propostaEnviada
                ? el('span', { class: 'tb-status', dataset: { tom: 'verde' }, text: 'Proposta enviada' })
                : el('a', { class: 'btn btn-sm btn-primary', href: `/projects/${item.projeto.id}#proposta`, text: 'Enviar proposta' })));
    })), { classe: 'border-top-0' }));
}
