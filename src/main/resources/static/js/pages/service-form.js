import { get, post, put } from '../core/api.js';
import { painel } from '../core/componentes.js';
import { el, idDaUrl, substituir } from '../core/dom.js';
import { atualizarTrilha, comBotaoOcupado, iniciarPagina, mostrarErro } from '../core/layout.js';

await iniciarPagina({
    protegida: true,
    tipoConta: 'PRESTADOR',
    trilha: [{ texto: 'Serviços', href: '/services?aba=meus' }, { texto: 'Novo serviço' }],
});

const servicoId = idDaUrl(2);
const categorias = await get('/api/categorias');
const servico = servicoId ? await get(`/api/servicos/${servicoId}`) : null;
if (servico) atualizarTrilha('Editar serviço');

const formulario = el('form', { class: 'tb-painel-corpo', novalidate: true },
    el('div', { class: 'alert alert-danger', 'data-erro-formulario': true, tabindex: '-1', hidden: true }),
    el('div', { class: 'row g-3' },
        el('div', { class: 'col-md-8' },
            el('label', { class: 'form-label', for: 'titulo', text: 'Título do serviço' }),
            el('input', { class: 'form-control', id: 'titulo', name: 'titulo', required: true, minlength: 5, maxlength: 120, value: servico?.titulo })),
        el('div', { class: 'col-md-4' },
            el('label', { class: 'form-label', for: 'categoriaId', text: 'Categoria' }),
            el('select', { class: 'form-select', id: 'categoriaId', name: 'categoriaId', required: true },
                el('option', { value: '', text: 'Selecione…' }),
                categorias.map((categoria) => el('option', { value: categoria.id, text: categoria.nome, selected: servico?.categoria.id === categoria.id })))),
        el('div', { class: 'col-12' },
            el('label', { class: 'form-label', for: 'descricao', text: 'Descrição' }),
            descricao(),
            el('div', { class: 'form-text', text: 'O que está incluído, como você trabalha e o que o cliente recebe.' })),
        el('div', { class: 'col-sm-6' },
            el('label', { class: 'form-label', for: 'precoBase', text: 'Preço base (R$)' }),
            el('input', { class: 'form-control', id: 'precoBase', name: 'precoBase', type: 'number', min: 1, step: '0.01', required: true, value: servico?.precoBase })),
        el('div', { class: 'col-sm-6' },
            el('label', { class: 'form-label', for: 'prazoDias', text: 'Prazo típico (dias)' }),
            el('input', { class: 'form-control', id: 'prazoDias', name: 'prazoDias', type: 'number', min: 1, max: 365, required: true, value: servico?.prazoDias }),
            el('div', { class: 'form-text', text: 'Usado pelo matching para estimar se você atende ao prazo dos projetos.' }))),
    el('div', { class: 'd-flex gap-2 border-top pt-3 mt-3' },
        el('button', { class: 'btn btn-primary', type: 'submit', text: servico ? 'Salvar alterações' : 'Publicar serviço' }),
        el('a', { class: 'btn btn-outline-secondary', href: '/services?aba=meus', text: 'Cancelar' })));

function descricao() {
    const area = el('textarea', { class: 'form-control', id: 'descricao', name: 'descricao', rows: 5, required: true, minlength: 20, maxlength: 2000 });
    area.value = servico?.descricao || '';
    return area;
}

formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    if (!formulario.checkValidity()) {
        formulario.reportValidity();
        return;
    }
    const dados = new FormData(formulario);
    const corpo = {
        categoriaId: Number(dados.get('categoriaId')),
        titulo: dados.get('titulo'),
        descricao: dados.get('descricao'),
        precoBase: Number(dados.get('precoBase')),
        prazoDias: Number(dados.get('prazoDias')),
    };
    await comBotaoOcupado(formulario.querySelector('[type=submit]'), async () => {
        try {
            await (servico ? put(`/api/servicos/${servicoId}`, corpo) : post('/api/servicos', corpo));
            window.location.assign('/services?aba=meus');
        } catch (erro) {
            mostrarErro(formulario, erro);
        }
    });
});

substituir('pagina',
    el('div', { class: 'tb-cabecalho-pagina' }, el('div', {},
        el('h1', { text: servico ? 'Editar serviço' : 'Novo serviço' }),
        el('p', { text: 'Serviços ativos aparecem no catálogo e contam para o matching na categoria escolhida.' }))),
    el('div', { class: 'row' }, el('div', { class: 'col-lg-8' }, painel('Dados do serviço', formulario))));
