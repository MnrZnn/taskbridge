import { el, icone } from './dom.js';
import { faixa, iniciais, moeda, prazo, status } from './formato.js';

/** Selo tipográfico da categoria — a identidade visual dos serviços, sem ícone genérico. */
export function seloCategoria(categoria, grande = false) {
    if (!categoria) {
        return el('span', { class: 'tb-selo' + (grande ? ' tb-selo-grande' : ''), title: 'Sem categoria' }, el('span', { text: '—' }));
    }
    return el('span', {
        class: 'tb-selo' + (grande ? ' tb-selo-grande' : ''),
        dataset: { categoria: categoria.slug },
        title: categoria.nome,
    }, el('span', { text: categoria.sigla }));
}

export function avatar(usuario, classe = 'tb-avatar') {
    if (usuario?.fotoUrl) return el('img', { class: classe, src: usuario.fotoUrl, alt: '' });
    return el('span', { class: classe, 'aria-hidden': 'true', text: iniciais(usuario?.nomeCompleto) });
}

export function painel(titulo, corpo, { acao, classe = '', id } = {}) {
    return el('section', { class: 'tb-painel ' + classe, id, 'aria-label': titulo },
        el('div', { class: 'tb-painel-titulo' }, el('h2', { text: titulo }), acao ? el('div', { class: 'tb-acao' }, acao) : null),
        corpo);
}

export function paginacao(pagina, aoMudar) {
    if (!pagina || pagina.totalPaginas <= 1) return null;
    const botao = (rotulo, destino, desabilitado) => el('li', { class: 'page-item' + (desabilitado ? ' disabled' : '') },
        el('button', {
            class: 'page-link', type: 'button', disabled: desabilitado,
            on: { click: () => aoMudar(destino) }, text: rotulo,
        }));
    return el('nav', { class: 'd-flex justify-content-between align-items-center px-3 py-2 border-top', 'aria-label': 'Paginação' },
        el('span', { class: 'tb-meta', text: `Página ${pagina.pagina + 1} de ${pagina.totalPaginas} · ${pagina.totalElementos} itens` }),
        el('ul', { class: 'pagination pagination-sm' },
            botao('‹ Anterior', pagina.pagina - 1, pagina.pagina === 0),
            botao('Próxima ›', pagina.pagina + 1, pagina.pagina + 1 >= pagina.totalPaginas)));
}

export function itemProjeto(projeto, { mostrarStatus = true } = {}) {
    return el('li', { class: 'd-flex gap-3 align-items-start' },
        seloCategoria(projeto.categoria),
        el('div', { class: 'flex-grow-1 min-w-0' },
            el('div', { class: 'd-flex flex-wrap gap-2 align-items-center' },
                el('a', { class: 'tb-lista-titulo', href: `/projects/${projeto.id}`, text: projeto.titulo }),
                mostrarStatus ? status(projeto.status, projeto.statusRotulo) : null),
            el('div', { class: 'small text-secondary text-truncate', text: projeto.necessidade }),
            el('div', { class: 'tb-meta mt-1' },
                el('span', { text: projeto.categoria ? projeto.categoria.nome : 'Sem categoria' }),
                el('span', { text: 'Orçamento ' + faixa(projeto.orcamentoMinimo, projeto.orcamentoMaximo) }),
                el('span', { text: 'Prazo ' + prazo(projeto.prazoDias) }))));
}

/** Pontuação + explicação por critério. A explicação é o ponto: "92%" sozinho não diz nada. */
export function blocoCompatibilidade(pontuacao, criterios) {
    return {
        pontuacao: el('div', { class: 'tb-match-pontuacao', 'aria-label': `Compatibilidade ${pontuacao}%` },
            el('strong', { text: pontuacao + '%' }),
            el('div', { class: 'tb-match-barra mx-2' }, barra(pontuacao)),
            el('small', { text: 'compatível' })),
        criterios: el('ul', { class: 'tb-criterios' }, criterios.map((criterio) =>
            el('li', { dataset: { atendido: String(criterio.atendido) }, title: `${criterio.criterio}: ${criterio.pontos}/${criterio.peso} pontos` },
                el('span', { text: criterio.explicacao })))),
    };
}

function barra(percentual) {
    const preenchimento = el('i');
    preenchimento.style.width = Math.max(0, Math.min(100, percentual)) + '%';
    return preenchimento;
}

/** Tabela de contratações; mostra "a outra parte" conforme quem está vendo. */
export function tabelaContratacoes(contratacoes, usuario) {
    const outraParte = (contratacao) => usuario.tipoConta === 'CLIENTE' ? contratacao.prestador : contratacao.cliente;
    return el('div', { class: 'table-responsive' },
        el('table', { class: 'table table-hover align-middle' },
            el('thead', {}, el('tr', {},
                el('th', { scope: 'col', text: 'Projeto' }),
                el('th', { scope: 'col', text: usuario.tipoConta === 'CLIENTE' ? 'Prestador' : 'Cliente' }),
                el('th', { scope: 'col', class: 'text-end', text: 'Valor' }),
                el('th', { scope: 'col', text: 'Status' }))),
            el('tbody', {}, contratacoes.map((contratacao) => el('tr', {},
                el('td', {}, el('a', { href: `/contracts/${contratacao.id}`, text: contratacao.projetoTitulo })),
                el('td', {}, el('a', { href: `/u/${outraParte(contratacao).nomeUsuario}`, text: outraParte(contratacao).nomeCompleto })),
                el('td', { class: 'text-end text-nowrap', text: moeda(contratacao.valor) }),
                el('td', {}, status(contratacao.status, contratacao.statusRotulo)))))));
}

/** Modal de confirmação (Bootstrap). Resolve true só se o usuário clicar em confirmar. */
export function confirmar({ titulo, mensagem, confirmar: rotulo = 'Confirmar', perigo = false }) {
    return new Promise((resolver) => {
        let confirmado = false;
        const botaoConfirmar = el('button', { class: 'btn ' + (perigo ? 'btn-danger' : 'btn-primary'), type: 'button', text: rotulo });
        const modal = el('div', { class: 'modal fade', tabindex: '-1', 'aria-labelledby': 'tb-modal-titulo', 'aria-modal': 'true', role: 'dialog' },
            el('div', { class: 'modal-dialog modal-dialog-centered' },
                el('div', { class: 'modal-content' },
                    el('div', { class: 'modal-header' },
                        el('h2', { class: 'modal-title', id: 'tb-modal-titulo', text: titulo }),
                        el('button', { class: 'btn-close', type: 'button', 'data-bs-dismiss': 'modal', 'aria-label': 'Fechar' })),
                    el('div', { class: 'modal-body' }, mensagem),
                    el('div', { class: 'modal-footer' },
                        el('button', { class: 'btn btn-outline-secondary', type: 'button', 'data-bs-dismiss': 'modal', text: 'Cancelar' }),
                        botaoConfirmar))));
        document.body.append(modal);
        const instancia = new window.bootstrap.Modal(modal);
        botaoConfirmar.addEventListener('click', () => {
            confirmado = true;
            instancia.hide();
        });
        modal.addEventListener('hidden.bs.modal', () => {
            instancia.dispose();
            modal.remove();
            resolver(confirmado);
        });
        instancia.show();
    });
}

export function botaoIcone(nomeIcone, texto, classe, aoClicar) {
    return el('button', { class: 'btn ' + classe, type: 'button', on: { click: aoClicar } }, icone(nomeIcone), texto);
}
