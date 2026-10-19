import { get, post, put } from '../core/api.js';
import { painel, seloCategoria } from '../core/componentes.js';
import { el, icone, idDaUrl, substituir } from '../core/dom.js';
import { faixa, prazo, TIPOS_REQUISITO } from '../core/formato.js';
import { atualizarTrilha, avisar, comBotaoOcupado, iniciarPagina, limparErros, mostrarErro } from '../core/layout.js';

await iniciarPagina({
    protegida: true,
    tipoConta: 'CLIENTE',
    trilha: [{ texto: 'Projetos', href: '/projects?aba=meus' }, { texto: 'Nova necessidade' }],
});

const ETAPAS = ['Necessidade', 'Escopo', 'Orçamento e prazo', 'Requisitos', 'Revisão', 'Publicação'];
const categorias = await get('/api/categorias');

let projetoId = idDaUrl(2);
let etapa = 0;
let pendencias = [];
const estado = {
    titulo: '', necessidade: '', categoriaId: null, objetivo: '', descricao: '',
    orcamentoMinimo: '', orcamentoMaximo: '', prazoDias: '', habilidades: [], requisitos: [],
};

if (projetoId) {
    const projeto = await get(`/api/projetos/${projetoId}`);
    if (projeto.status !== 'DRAFT') {
        substituir('pagina', el('div', { class: 'alert alert-warning' }, 'Somente rascunhos podem ser editados. ',
            el('a', { href: `/projects/${projetoId}`, text: 'Voltar ao projeto' })));
        throw new Error('projeto não editável');
    }
    Object.assign(estado, {
        titulo: projeto.titulo, necessidade: projeto.necessidade, categoriaId: projeto.categoria?.id ?? null,
        objetivo: projeto.objetivo ?? '', descricao: projeto.descricao ?? '',
        orcamentoMinimo: projeto.orcamentoMinimo ?? '', orcamentoMaximo: projeto.orcamentoMaximo ?? '',
        prazoDias: projeto.prazoDias ?? '', habilidades: projeto.habilidades, requisitos: projeto.requisitos,
    });
    pendencias = projeto.pendenciasPublicacao;
    atualizarTrilha('Editar rascunho');
}

const trilhaEtapas = el('ol', { class: 'tb-etapas', 'aria-label': 'Etapas' });
const area = el('div');
substituir('pagina',
    el('div', { class: 'tb-cabecalho-pagina' }, el('div', {},
        el('div', { class: 'tb-sobretitulo', text: 'Assistente de projeto' }),
        el('h1', { text: projetoId ? 'Continuar rascunho' : 'Descreva sua necessidade' }),
        el('p', { text: 'O rascunho é salvo a cada etapa. Ele só fica visível para profissionais depois de publicado.' }))),
    trilhaEtapas,
    area);
desenhar();

function desenhar() {
    substituir(trilhaEtapas, ETAPAS.map((nome, indice) => el('li', {
        class: indice < etapa ? 'tb-concluida' : null,
        'aria-current': indice === etapa ? 'step' : null,
    }, el('span', { text: nome }))));
    const construtores = [etapaNecessidade, etapaEscopo, etapaOrcamento, etapaRequisitos, etapaRevisao, etapaPublicacao];
    substituir(area, construtores[etapa]());
    area.querySelector('input, textarea, select, button')?.focus();
}

function formularioEtapa(titulo, ajuda, campos, { salvar = true } = {}) {
    const formulario = el('form', { class: 'tb-painel-corpo', novalidate: true },
        el('div', { class: 'alert alert-danger', 'data-erro-formulario': true, tabindex: '-1', hidden: true }),
        ajuda ? el('p', { class: 'text-secondary small', text: ajuda }) : null,
        campos,
        el('div', { class: 'd-flex justify-content-between border-top pt-3 mt-3' },
            etapa > 0
                ? el('button', { class: 'btn btn-outline-secondary', type: 'button', on: { click: () => { etapa--; desenhar(); } } }, icone('arrow-left'), 'Voltar')
                : el('span'),
            el('button', { class: 'btn btn-primary', type: 'submit' }, salvar ? 'Salvar e continuar' : 'Continuar', icone('chevron-right'))));
    formulario.addEventListener('submit', async (evento) => {
        evento.preventDefault();
        limparErros(formulario);
        if (!formulario.checkValidity()) {
            formulario.reportValidity();
            return;
        }
        coletar(formulario);
        await comBotaoOcupado(formulario.querySelector('[type=submit]'), async () => {
            try {
                if (salvar) await salvarRascunho();
                etapa++;
                desenhar();
            } catch (erro) {
                mostrarErro(formulario, erro);
            }
        });
    });
    return painel(`Etapa ${etapa + 1} de ${ETAPAS.length} — ${titulo}`, formulario);
}

function campo(rotulo, entrada, ajuda) {
    const id = entrada.id || 'campo-' + entrada.name;
    entrada.id = id;
    return el('div', { class: 'mb-3' },
        el('label', { class: 'form-label', for: id, text: rotulo }),
        entrada,
        ajuda ? el('div', { class: 'form-text', text: ajuda }) : null);
}

function etapaNecessidade() {
    return formularioEtapa('Necessidade', 'Conte com suas palavras. Você não precisa saber o nome técnico do serviço.', [
        campo('Dê um título curto', el('input', { class: 'form-control', name: 'titulo', required: true, minlength: 5, maxlength: 150, value: estado.titulo, placeholder: 'Ex.: Site para minha confeitaria' })),
        campo('O que você precisa?', textarea('necessidade', estado.necessidade, 5, 2000, true, 10),
            'Ex.: "Preciso criar um site para minha pequena empresa, para mostrar os produtos e receber encomendas."'),
    ]);
}

function etapaEscopo() {
    const opcoes = el('div', { class: 'row g-2 mb-3', role: 'radiogroup', 'aria-label': 'Categoria' }, categorias.map((categoria) =>
        el('div', { class: 'col-sm-6 col-lg-3' }, el('div', { class: 'tb-opcao-conta' },
            el('input', { type: 'radio', name: 'categoriaId', id: `cat-${categoria.id}`, value: categoria.id, checked: estado.categoriaId === categoria.id }),
            el('label', { for: `cat-${categoria.id}`, class: 'd-flex gap-2 align-items-center' },
                seloCategoria(categoria), el('span', {}, el('strong', { text: categoria.nome })))))));
    return formularioEtapa('Escopo', 'Transforme a necessidade em escopo: qual é a área, aonde se quer chegar e o que está incluído.', [
        el('fieldset', {}, el('legend', { text: 'Categoria' }), opcoes),
        campo('Objetivo', el('input', { class: 'form-control', name: 'objetivo', maxlength: 500, value: estado.objetivo, placeholder: 'Ex.: Receber encomendas pela internet' })),
        campo('Descrição do escopo', textarea('descricao', estado.descricao, 6, 4000, false),
            'O que precisa ser feito, o que está fora do escopo, referências. Mínimo de 30 caracteres para publicar.'),
        campo('Habilidades desejadas', el('input', { class: 'form-control', name: 'habilidades', maxlength: 400, value: estado.habilidades.join(', '), placeholder: 'html, css, seo' }),
            'Separe por vírgula. Usado no cálculo de compatibilidade.'),
    ]);
}

function etapaOrcamento() {
    return formularioEtapa('Orçamento e prazo', 'Uma faixa realista ajuda o matching a encontrar profissionais compatíveis.', [
        el('div', { class: 'row g-3' },
            el('div', { class: 'col-sm-4' }, campo('Orçamento mínimo (R$)', el('input', { class: 'form-control', name: 'orcamentoMinimo', type: 'number', min: 1, step: '0.01', value: estado.orcamentoMinimo }))),
            el('div', { class: 'col-sm-4' }, campo('Orçamento máximo (R$)', el('input', { class: 'form-control', name: 'orcamentoMaximo', type: 'number', min: 1, step: '0.01', value: estado.orcamentoMaximo }))),
            el('div', { class: 'col-sm-4' }, campo('Prazo desejado (dias)', el('input', { class: 'form-control', name: 'prazoDias', type: 'number', min: 1, max: 365, value: estado.prazoDias })))),
    ]);
}

function etapaRequisitos() {
    const lista = el('div', { class: 'tb-itens-escopo' });
    const redesenhar = () => substituir(lista, estado.requisitos.length
        ? estado.requisitos.map((item, indice) => linhaRequisito(item, indice, redesenhar))
        : el('p', { class: 'text-secondary small', text: 'Nenhum item ainda. Adicione pelo menos um entregável e um critério de aceite.' }));
    redesenhar();
    const adicionar = (tipo) => {
        sincronizarRequisitos(lista);
        estado.requisitos.push({ tipo, descricao: '' });
        redesenhar();
        lista.querySelector('.tb-item-escopo:last-child input')?.focus();
    };
    return formularioEtapa('Requisitos', 'Entregáveis viram as etapas acompanhadas na contratação. Critérios de aceite dizem quando o trabalho está pronto.', [
        lista,
        el('div', { class: 'd-flex flex-wrap gap-2' },
            Object.entries(TIPOS_REQUISITO).map(([tipo, rotulo]) =>
                el('button', { class: 'btn btn-sm btn-outline-primary', type: 'button', on: { click: () => adicionar(tipo) } }, icone('plus'), rotulo))),
    ]);
}

function linhaRequisito(item, indice, redesenhar) {
    const seletor = el('select', { class: 'form-select form-select-sm', name: `requisitos[${indice}].tipo`, 'aria-label': 'Tipo' },
        Object.entries(TIPOS_REQUISITO).map(([tipo, rotulo]) => el('option', { value: tipo, text: rotulo, selected: item.tipo === tipo })));
    return el('div', { class: 'tb-item-escopo' },
        seletor,
        el('input', { class: 'form-control form-control-sm', name: `requisitos[${indice}].descricao`, 'aria-label': 'Descrição', required: true, maxlength: 500, value: item.descricao }),
        el('button', {
            class: 'btn btn-sm btn-outline-danger', type: 'button', 'aria-label': 'Remover item',
            on: { click: (evento) => { sincronizarRequisitos(evento.target.closest('.tb-itens-escopo')); estado.requisitos.splice(indice, 1); redesenhar(); } },
        }, icone('trash-2')));
}

function sincronizarRequisitos(lista) {
    lista.querySelectorAll('.tb-item-escopo').forEach((linha, indice) => {
        estado.requisitos[indice] = { tipo: linha.querySelector('select').value, descricao: linha.querySelector('input').value.trim() };
    });
}

function etapaRevisao() {
    const categoria = categorias.find((item) => item.id === estado.categoriaId);
    const porTipo = (tipo) => estado.requisitos.filter((item) => item.tipo === tipo).map((item) => item.descricao);
    const listaOuTraco = (itens) => itens.length ? el('ul', { class: 'mb-0 ps-3' }, itens.map((texto) => el('li', { text: texto }))) : '—';
    return formularioEtapa('Revisão', 'Confira antes de publicar. Você ainda pode voltar e ajustar.', [
        pendencias.length ? el('div', { class: 'alert alert-warning' }, el('strong', { text: 'Falta para publicar:' }),
            el('ul', {}, pendencias.map((texto) => el('li', { text: texto })))) : el('div', { class: 'alert alert-success', text: 'Tudo pronto para publicar.' }),
        el('dl', { class: 'row tb-revisao' },
            item('Título', estado.titulo), item('Necessidade', estado.necessidade), item('Categoria', categoria?.nome || '—'),
            item('Objetivo', estado.objetivo || '—'), item('Escopo', estado.descricao || '—'),
            item('Orçamento', faixa(estado.orcamentoMinimo || null, estado.orcamentoMaximo || null)),
            item('Prazo', prazo(Number(estado.prazoDias) || null)),
            item('Habilidades', estado.habilidades.join(', ') || '—'),
            item('Requisitos', listaOuTraco(porTipo('REQUISITO'))),
            item('Entregáveis', listaOuTraco(porTipo('ENTREGAVEL'))),
            item('Critérios de aceite', listaOuTraco(porTipo('CRITERIO_ACEITE')))),
    ], { salvar: false });
}

function item(rotulo, valor) {
    return [el('dt', { class: 'col-sm-3', text: rotulo }), el('dd', { class: 'col-sm-9 tb-quebra' }, valor)];
}

function etapaPublicacao() {
    const alerta = el('div', { class: 'alert alert-danger', tabindex: '-1', hidden: true });
    const publicar = el('button', { class: 'btn btn-primary btn-lg', type: 'button' }, icone('send'), 'Publicar projeto');
    publicar.addEventListener('click', () => comBotaoOcupado(publicar, async () => {
        try {
            await post(`/api/projetos/${projetoId}/publicar`);
            avisar('Projeto publicado.');
            window.location.assign(`/projects/${projetoId}`);
        } catch (erro) {
            substituir(alerta, el('strong', { text: erro.message }), erro.detalhes.length ? el('ul', {}, erro.detalhes.map((texto) => el('li', { text: texto }))) : null);
            alerta.hidden = false;
            alerta.focus();
        }
    }));
    return painel(`Etapa ${ETAPAS.length} de ${ETAPAS.length} — Publicação`, el('div', { class: 'tb-painel-corpo' },
        alerta,
        el('p', { text: 'Ao publicar, o projeto aparece para profissionais e o matching passa a sugerir quem é compatível. Depois de publicado, o escopo não pode mais ser editado.' }),
        el('div', { class: 'd-flex flex-wrap gap-2' },
            el('button', { class: 'btn btn-outline-secondary', type: 'button', on: { click: () => { etapa--; desenhar(); } } }, icone('arrow-left'), 'Voltar'),
            el('a', { class: 'btn btn-outline-secondary', href: `/projects/${projetoId}`, text: 'Manter como rascunho' }),
            publicar)));
}

function textarea(nome, valor, linhas, maximo, obrigatorio, minimo) {
    const area = el('textarea', { class: 'form-control', name: nome, rows: linhas, maxlength: maximo, required: obrigatorio, minlength: minimo });
    area.value = valor || '';
    return area;
}

function coletar(formulario) {
    const dados = new FormData(formulario);
    for (const chave of ['titulo', 'necessidade', 'objetivo', 'descricao', 'orcamentoMinimo', 'orcamentoMaximo', 'prazoDias']) {
        if (dados.has(chave)) estado[chave] = String(dados.get(chave)).trim();
    }
    if (formulario.querySelector('[name=categoriaId]')) {
        const marcada = dados.get('categoriaId');
        estado.categoriaId = marcada ? Number(marcada) : null;
    }
    if (dados.has('habilidades')) {
        estado.habilidades = String(dados.get('habilidades')).split(',').map((texto) => texto.trim()).filter(Boolean);
    }
    const lista = formulario.querySelector('.tb-itens-escopo');
    if (lista) sincronizarRequisitos(lista);
}

async function salvarRascunho() {
    const numeroOuNulo = (valor) => (valor === '' || valor === null || valor === undefined ? null : Number(valor));
    const corpo = {
        titulo: estado.titulo,
        necessidade: estado.necessidade,
        categoriaId: estado.categoriaId,
        objetivo: estado.objetivo || null,
        descricao: estado.descricao || null,
        orcamentoMinimo: numeroOuNulo(estado.orcamentoMinimo),
        orcamentoMaximo: numeroOuNulo(estado.orcamentoMaximo),
        prazoDias: numeroOuNulo(estado.prazoDias),
        habilidades: estado.habilidades,
        requisitos: estado.requisitos.filter((item) => item.descricao),
    };
    const projeto = projetoId ? await put(`/api/projetos/${projetoId}`, corpo) : await post('/api/projetos', corpo);
    if (!projetoId) {
        projetoId = projeto.id;
        history.replaceState(null, '', `/projects/${projetoId}/edit`);
    }
    pendencias = projeto.pendenciasPublicacao;
}
