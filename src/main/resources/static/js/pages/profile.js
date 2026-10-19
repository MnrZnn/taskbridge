import { get } from '../core/api.js';
import { painel, seloCategoria } from '../core/componentes.js';
import { el, icone, substituir } from '../core/dom.js';
import { data, iniciais, mesAno, moeda, MODALIDADES, prazo } from '../core/formato.js';
import { atualizarTrilha, iniciarPagina, sessao } from '../core/layout.js';
import { iconeRede, linkExterno } from '../core/redes.js';

// /profile mostra o próprio perfil público; /u/{nomeUsuario}, o de qualquer usuário.
const partes = window.location.pathname.split('/').filter(Boolean);
const proprio = partes[0] === 'profile';
const usuario = await iniciarPagina({ protegida: proprio, trilha: [{ texto: 'Início', href: '/' }, { texto: 'Perfil' }] });
const nomeUsuario = proprio ? usuario.nomeUsuario : decodeURIComponent(partes[1] || '');

let perfil;
try {
    perfil = await get(`/api/usuarios/${encodeURIComponent(nomeUsuario)}`);
} catch (erro) {
    substituir('pagina', el('div', { class: 'alert alert-warning', text: erro.status === 404 ? 'Perfil não encontrado.' : erro.message }));
    throw erro;
}
const eu = (await sessao().catch(() => null))?.nomeUsuario === perfil.nomeUsuario;
atualizarTrilha('@' + perfil.nomeUsuario);
document.title = `${perfil.nomeCompleto} · TaskBridge`;

const prestador = perfil.tipoConta === 'PRESTADOR';
const profissional = perfil.perfilProfissional;

substituir('pagina',
    el('div', { class: 'row g-4' },
        el('div', { class: 'col-lg-8' },
            el('section', { class: 'tb-painel', 'aria-label': 'Identificação' },
                el('div', { class: 'tb-perfil-cabeca' },
                    perfil.fotoUrl
                        ? el('img', { class: 'tb-perfil-foto', src: perfil.fotoUrl, alt: 'Foto de ' + perfil.nomeCompleto })
                        : el('div', { class: 'tb-perfil-foto-vazia', 'aria-hidden': 'true', text: iniciais(perfil.nomeCompleto) }),
                    el('div', { class: 'flex-grow-1 min-w-0' },
                        el('h1', { class: 'tb-perfil-nome', text: perfil.nomeCompleto }),
                        el('div', { class: 'tb-perfil-usuario', text: '@' + perfil.nomeUsuario }),
                        el('div', { class: 'tb-perfil-titulo', text: profissional?.tituloProfissional || (prestador ? 'Prestador de serviços' : 'Cliente') }),
                        el('div', { class: 'tb-meta mt-1' },
                            perfil.localizacao ? el('span', { text: perfil.localizacao }) : null,
                            el('span', { text: 'no TaskBridge desde ' + mesAno(perfil.membroDesde) }))),
                    el('div', { class: 'tb-perfil-canto' },
                        eu ? el('a', { class: 'btn btn-sm btn-outline-secondary', href: '/settings' }, icone('pencil'), 'Editar perfil') : null,
                        linksDoPerfil())),
                el('div', { class: 'tb-perfil-numeros' },
                    numero(perfil.estatisticas.projetosConcluidos, prestador ? 'trabalhos concluídos' : 'projetos concluídos'),
                    prestador ? numero(perfil.estatisticas.servicosAtivos, 'serviços ativos') : null,
                    numero(perfil.estatisticas.emAndamento, 'em andamento'),
                    prestador && profissional ? numero(profissional.experienciaAnos, 'anos de experiência') : null)),
            painel('Sobre', el('p', { class: 'tb-painel-corpo tb-quebra', text: perfil.biografia || 'Nenhuma biografia informada.' })),
            prestador ? painelServicos() : null,
            painelHistorico()),
        el('div', { class: 'col-lg-4' }, prestador ? painelProfissional() : painelCliente())));

/** Como os links do cabeçalho de canal do YouTube antigo: o primeiro com nome, os demais só com ícone. */
function linksDoPerfil() {
    if (!perfil.links.length) {
        return eu ? el('a', { class: 'tb-perfil-links-vazio', href: '/settings#links', text: '+ adicionar links' }) : null;
    }
    const itens = perfil.links.map((url, indice) => linkExterno(url,
        (rede) => [iconeRede(rede), indice === 0 ? el('span', { text: rede.nome }) : el('span', { class: 'visually-hidden', text: rede.nome })],
        'tb-perfil-link' + (indice === 0 ? ' tb-perfil-link-principal' : '')));
    return el('nav', { class: 'tb-perfil-links', 'aria-label': 'Links de ' + perfil.nomeCompleto }, itens);
}

function numero(valor, rotulo) {
    return el('div', {}, el('strong', { text: String(valor) }), el('span', { text: rotulo }));
}

function painelServicos() {
    return painel('Serviços', perfil.servicos.length
        ? el('ul', { class: 'tb-lista' }, perfil.servicos.map((servico) => el('li', { class: 'd-flex gap-3' },
            seloCategoria(servico.categoria),
            el('div', { class: 'min-w-0' },
                el('strong', { class: 'text-dark', text: servico.titulo }),
                el('p', { class: 'small text-secondary mb-1 tb-quebra', text: servico.descricao }),
                el('div', { class: 'tb-meta' }, el('span', { text: 'a partir de ' + moeda(servico.precoBase) }), el('span', { text: prazo(servico.prazoDias) }))))))
        : el('p', { class: 'tb-painel-corpo text-secondary small', text: 'Nenhum serviço ativo no momento.' }));
}

function painelHistorico() {
    return painel('Histórico', perfil.historico.length
        ? el('div', { class: 'table-responsive' }, el('table', { class: 'table' },
            el('thead', {}, el('tr', {}, el('th', { text: 'Projeto' }), el('th', { text: 'Categoria' }), el('th', { text: 'Concluído' }))),
            el('tbody', {}, perfil.historico.map((item) => el('tr', {},
                el('td', { text: item.titulo }), el('td', { text: item.categoria || '—' }), el('td', { text: data(item.concluidoEm) }))))))
        : el('p', { class: 'tb-painel-corpo text-secondary small', text: 'Nenhum projeto concluído ainda.' }));
}

function painelProfissional() {
    if (!profissional) {
        return painel('Atuação', el('p', { class: 'tb-painel-corpo small text-secondary', text: 'Perfil profissional ainda não preenchido.' }));
    }
    return painel('Atuação', el('div', { class: 'tb-painel-corpo' },
        profissional.descricao ? el('p', { class: 'small tb-quebra', text: profissional.descricao }) : null,
        el('dl', { class: 'tb-ficha mb-3' },
            el('dt', { text: 'Disponível' }), el('dd', { text: profissional.disponibilidadeRotulo }),
            el('dt', { text: 'Modalidade' }), el('dd', { text: MODALIDADES[profissional.modalidadeTrabalho] }),
            el('dt', { text: 'Faixa' }), el('dd', { text: `${moeda(profissional.faixaPrecoMinima)} – ${moeda(profissional.faixaPrecoMaxima)}` })),
        el('div', { class: 'tb-sobretitulo', text: 'Habilidades' }),
        el('div', {}, profissional.habilidades.length
            ? profissional.habilidades.map((habilidade) => el('span', { class: 'tb-etiqueta', text: habilidade }))
            : el('span', { class: 'small text-secondary', text: '—' }))));
}

function painelCliente() {
    return painel('Sobre esta conta', el('div', { class: 'tb-painel-corpo small text-secondary' },
        el('p', { class: 'mb-0', text: 'Conta de cliente: publica necessidades e contrata profissionais pelo TaskBridge.' })));
}
