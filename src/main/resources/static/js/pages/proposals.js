import { get, post } from '../core/api.js';
import { avatar, confirmar, paginacao, painel } from '../core/componentes.js';
import { carregando, el, icone, substituir, vazio } from '../core/dom.js';
import { data, moeda, prazo, status } from '../core/formato.js';
import { avisar, iniciarPagina } from '../core/layout.js';

const usuario = await iniciarPagina({ protegida: true, trilha: [{ texto: 'Painel', href: '/dashboard' }, { texto: 'Propostas' }] });
const cliente = usuario.tipoConta === 'CLIENTE';
const lista = el('div', {}, carregando());

substituir('pagina',
    el('div', { class: 'tb-cabecalho-pagina' }, el('div', {},
        el('h1', { text: cliente ? 'Propostas recebidas' : 'Propostas enviadas' }),
        el('p', { text: cliente
            ? 'Compare as propostas e aceite uma na página do projeto. Aceitar uma recusa as demais do mesmo projeto.'
            : 'Acompanhe a resposta dos clientes. Propostas pendentes podem ser canceladas.' }))),
    painel(cliente ? 'Todas as propostas dos seus projetos' : 'Suas propostas', lista));

carregar(0);

async function carregar(pagina) {
    substituir(lista, carregando());
    const resultado = await get(`/api/propostas/${cliente ? 'recebidas' : 'enviadas'}?page=${pagina}&size=15`);
    if (!resultado.conteudo.length) {
        substituir(lista, el('div', { class: 'tb-painel-corpo' }, vazio(cliente ? 'Nenhuma proposta recebida ainda.' : 'Você ainda não enviou propostas.',
            el('a', { class: 'btn btn-sm btn-primary', href: cliente ? '/projects?aba=meus' : '/projects?aba=compativeis', text: cliente ? 'Ver meus projetos' : 'Ver projetos compatíveis' }))));
        return;
    }
    substituir(lista, el('div', { class: 'table-responsive' }, el('table', { class: 'table align-middle' },
        el('thead', {}, el('tr', {},
            el('th', { scope: 'col', text: 'Projeto' }),
            el('th', { scope: 'col', text: cliente ? 'Prestador' : 'Enviada em' }),
            el('th', { scope: 'col', class: 'text-end', text: 'Valor' }),
            el('th', { scope: 'col', text: 'Prazo' }),
            el('th', { scope: 'col', text: 'Status' }),
            el('th', { scope: 'col', class: 'text-end', text: 'Ações' }))),
        el('tbody', {}, resultado.conteudo.map(linha)))), paginacao(resultado, carregar));
}

function linha(proposta) {
    return el('tr', {},
        el('td', {}, el('a', { href: `/projects/${proposta.projetoId}${cliente ? '#propostas' : ''}`, text: proposta.projetoTitulo })),
        el('td', {}, cliente
            ? el('span', { class: 'd-inline-flex gap-2 align-items-center' }, avatar(proposta.prestador),
                el('a', { href: `/u/${proposta.prestador.nomeUsuario}`, text: proposta.prestador.nomeCompleto }))
            : data(proposta.criadaEm)),
        el('td', { class: 'text-end text-nowrap', text: moeda(proposta.valor) }),
        el('td', { class: 'text-nowrap', text: prazo(proposta.prazoDias) }),
        el('td', {}, status(proposta.status, proposta.statusRotulo)),
        el('td', { class: 'text-end' }, acoes(proposta)));
}

function acoes(proposta) {
    if (proposta.status !== 'PENDING') return null;
    if (cliente) return el('a', { class: 'btn btn-sm btn-outline-primary', href: `/projects/${proposta.projetoId}#propostas`, text: 'Avaliar' });
    const cancelar = el('button', { class: 'btn btn-sm btn-outline-danger', type: 'button' }, icone('x'), 'Cancelar');
    cancelar.addEventListener('click', async () => {
        if (!await confirmar({ titulo: 'Cancelar proposta', mensagem: 'O cliente não poderá mais aceitá-la.', confirmar: 'Cancelar proposta', perigo: true })) return;
        try {
            await post(`/api/propostas/${proposta.id}/cancelar`);
            avisar('Proposta cancelada.');
            carregar(0);
        } catch (erro) {
            avisar(erro.message, 'erro');
        }
    });
    return cancelar;
}
