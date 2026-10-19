import { get } from '../core/api.js';
import { paginacao, painel, tabelaContratacoes } from '../core/componentes.js';
import { carregando, el, substituir, vazio } from '../core/dom.js';
import { iniciarPagina } from '../core/layout.js';

const usuario = await iniciarPagina({ protegida: true, trilha: [{ texto: 'Painel', href: '/dashboard' }, { texto: 'Contratações' }] });
const lista = el('div', {}, carregando());

substituir('pagina',
    el('div', { class: 'tb-cabecalho-pagina' }, el('div', {},
        el('h1', { text: 'Contratações' }),
        el('p', { text: 'Criadas quando uma proposta é aceita. Só você e a outra parte têm acesso.' }))),
    painel('Suas contratações', lista));

carregar(0);

async function carregar(pagina) {
    substituir(lista, carregando());
    const resultado = await get(`/api/contratacoes?page=${pagina}&size=15`);
    substituir(lista, resultado.conteudo.length
        ? tabelaContratacoes(resultado.conteudo, usuario)
        : el('div', { class: 'tb-painel-corpo' }, vazio('Nenhuma contratação ainda.')),
    paginacao(resultado, carregar));
}
