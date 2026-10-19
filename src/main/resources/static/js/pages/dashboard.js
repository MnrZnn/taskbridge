import { get } from '../core/api.js';
import { avatar, blocoCompatibilidade, itemProjeto, painel, tabelaContratacoes } from '../core/componentes.js';
import { el, icone, substituir, vazio } from '../core/dom.js';
import { iniciarPagina } from '../core/layout.js';

const usuario = await iniciarPagina({ protegida: true, trilha: [{ texto: 'Painel' }] });
const dados = await get('/api/dashboard');
const cliente = usuario.tipoConta === 'CLIENTE';
const primeiroNome = usuario.nomeCompleto.split(' ')[0];

const cabecalho = el('div', { class: 'tb-cabecalho-pagina' },
    el('div', {},
        el('div', { class: 'tb-sobretitulo', text: cliente ? 'Painel do cliente' : 'Painel do prestador' }),
        el('h1', { text: `Olá, ${primeiroNome}` }),
        el('p', { text: cliente ? 'Acompanhe seus projetos, propostas e entregas.' : 'Projetos compatíveis com você e o andamento dos seus trabalhos.' })),
    cliente
        ? el('a', { class: 'btn btn-primary', href: '/projects/new' }, icone('plus'), 'Nova necessidade')
        : el('a', { class: 'btn btn-primary', href: '/services/new' }, icone('plus'), 'Novo serviço'));

const indicadores = el('div', { class: 'tb-indicadores' }, dados.indicadores.map((indicador) =>
    el('a', { class: 'tb-indicador', href: indicador.link },
        el('div', { class: 'tb-indicador-valor', text: String(indicador.valor) }),
        el('div', { class: 'tb-indicador-rotulo', text: indicador.rotulo }))));

const contratacoes = painel('Contratações ativas', dados.contratacoesAtivas.length
    ? tabelaContratacoes(dados.contratacoesAtivas, usuario)
    : el('div', { class: 'tb-painel-corpo' }, vazio('Nenhuma contratação em andamento.')),
{ acao: el('a', { href: '/contracts', text: 'Ver todas' }) });

substituir('pagina', cabecalho, indicadores, cliente ? layoutCliente() : layoutPrestador());

function layoutCliente() {
    const projetos = painel('Projetos ativos', dados.projetosAtivos.length
        ? el('ul', { class: 'tb-lista' }, dados.projetosAtivos.map((projeto) => itemProjeto(projeto)))
        : el('div', { class: 'tb-painel-corpo' }, vazio('Você ainda não tem projetos.',
            el('a', { class: 'btn btn-sm btn-primary', href: '/projects/new', text: 'Descrever uma necessidade' }))),
    { acao: el('a', { href: '/projects?aba=meus', text: 'Ver todos' }) });

    return el('div', { class: 'row g-4' },
        el('div', { class: 'col-lg-8' }, projetos, contratacoes),
        el('div', { class: 'col-lg-4' }, painelSugestoes(), painelComoFunciona()));
}

function painelSugestoes() {
    const sugestoes = dados.sugestoes;
    if (!sugestoes) {
        return painel('Profissionais sugeridos', el('div', { class: 'tb-painel-corpo small text-secondary',
            text: 'Publique um projeto para ver profissionais compatíveis com ele.' }));
    }
    const corpo = sugestoes.profissionais.length
        ? el('ul', { class: 'tb-lista' }, sugestoes.profissionais.map((sugestao) => {
            const bloco = blocoCompatibilidade(sugestao.pontuacao, sugestao.criterios.filter((criterio) => criterio.atendido).slice(0, 3));
            return el('li', {},
                el('div', { class: 'd-flex gap-2 align-items-center mb-1' },
                    avatar(sugestao.profissional),
                    el('div', { class: 'min-w-0' },
                        el('a', { class: 'tb-lista-titulo', href: `/u/${sugestao.profissional.nomeUsuario}`, text: sugestao.profissional.nomeCompleto }),
                        el('div', { class: 'tb-meta', text: sugestao.tituloProfissional })),
                    el('span', { class: 'ms-auto tb-status', dataset: { tom: 'roxo' }, text: sugestao.pontuacao + '%' })),
                bloco.criterios);
        }))
        : el('div', { class: 'tb-painel-corpo small text-secondary', text: 'Ainda não há profissionais compatíveis.' });
    return painel('Sugestões para seu projeto', el('div', {},
        el('div', { class: 'px-3 pt-2 small' }, 'Para ', el('a', { href: `/projects/${sugestoes.projetoId}`, text: sugestoes.projetoTitulo })),
        corpo), { classe: 'tb-painel-destaque' });
}

function painelComoFunciona() {
    const passos = ['Descreva a necessidade e estruture o escopo', 'Publique e compare propostas', 'Aceite uma proposta e pague via PIX (teste)', 'Revise e aprove cada entrega'];
    return painel('Próximos passos', el('ol', { class: 'tb-painel-corpo small mb-0 ps-4' }, passos.map((passo) => el('li', { class: 'mb-1', text: passo }))));
}

function layoutPrestador() {
    const compativeis = painel('Projetos compatíveis com você', dados.projetosCompativeis.length
        ? el('ul', { class: 'tb-lista' }, dados.projetosCompativeis.map(itemCompativel))
        : el('div', { class: 'tb-painel-corpo' }, vazio('Complete seu perfil profissional e cadastre serviços para receber sugestões.',
            el('a', { class: 'btn btn-sm btn-primary', href: '/settings?secao=profissional', text: 'Completar perfil' }))),
    { acao: el('a', { href: '/projects?aba=compativeis', text: 'Ver mais' }) });

    return el('div', { class: 'row g-4' },
        el('div', { class: 'col-lg-8' }, compativeis),
        el('div', { class: 'col-lg-4' }, contratacoes, painel('Atalhos', el('div', { class: 'tb-painel-corpo d-grid gap-2' },
            el('a', { class: 'btn btn-outline-secondary', href: '/services?aba=meus' }, icone('store'), 'Meus serviços'),
            el('a', { class: 'btn btn-outline-secondary', href: '/proposals' }, icone('send'), 'Minhas propostas'),
            el('a', { class: 'btn btn-outline-secondary', href: '/profile' }, icone('user'), 'Ver meu perfil público')))));
}

function itemCompativel(item) {
    const bloco = blocoCompatibilidade(item.pontuacao, item.criterios);
    const projeto = item.projeto;
    return el('li', { class: 'tb-match' },
        bloco.pontuacao,
        el('div', { class: 'min-w-0' },
            el('a', { class: 'tb-lista-titulo', href: `/projects/${projeto.id}`, text: projeto.titulo }),
            el('div', { class: 'small text-secondary', text: projeto.necessidade }),
            bloco.criterios),
        el('div', { class: 'tb-match-acoes' }, item.propostaEnviada
            ? el('span', { class: 'tb-status', dataset: { tom: 'verde' }, text: 'Proposta enviada' })
            : el('a', { class: 'btn btn-sm btn-primary', href: `/projects/${projeto.id}#proposta`, text: 'Enviar proposta' })));
}
