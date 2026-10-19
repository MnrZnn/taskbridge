import { get, post } from '../core/api.js';
import { paginacao, painel } from '../core/componentes.js';
import { carregando, el, icone, substituir, vazio } from '../core/dom.js';
import { dataHora } from '../core/formato.js';
import { iniciarPagina } from '../core/layout.js';

await iniciarPagina({ protegida: true, trilha: [{ texto: 'Painel', href: '/dashboard' }, { texto: 'Notificações' }] });
const lista = el('div', {}, carregando());
const marcarTodas = el('button', { class: 'btn btn-sm btn-outline-secondary', type: 'button' }, icone('check-check'), 'Marcar todas como lidas');
marcarTodas.addEventListener('click', async () => {
    await post('/api/notificacoes/lidas');
    carregar(0);
});

substituir('pagina',
    el('div', { class: 'tb-cabecalho-pagina' }, el('div', {}, el('h1', { text: 'Notificações' })), marcarTodas),
    painel('Todas', lista));
carregar(0);

async function carregar(pagina) {
    const resultado = await get(`/api/notificacoes?page=${pagina}&size=20`);
    if (!resultado.conteudo.length) {
        substituir(lista, el('div', { class: 'tb-painel-corpo' }, vazio('Nenhuma notificação.')));
        return;
    }
    substituir(lista, el('ul', { class: 'tb-lista' }, resultado.conteudo.map((notificacao) => el('li', { class: 'd-flex gap-3 align-items-start' },
        el('span', { class: 'tb-status', dataset: { tom: notificacao.lida ? 'cinza' : 'roxo' }, text: notificacao.lida ? 'lida' : 'nova' }),
        el('div', { class: 'flex-grow-1' },
            el('a', {
                class: 'tb-lista-titulo' + (notificacao.lida ? ' fw-normal' : ''), href: notificacao.link || '/dashboard', text: notificacao.mensagem,
                on: { click: () => { if (!notificacao.lida) post(`/api/notificacoes/${notificacao.id}/lida`); } },
            }),
            el('div', { class: 'tb-meta', text: dataHora(notificacao.criadaEm) }))))),
    paginacao(resultado, carregar));
}
