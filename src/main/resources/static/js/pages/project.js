import { del, get, post } from '../core/api.js';
import { avatar, blocoCompatibilidade, confirmar, paginacao, painel, seloCategoria } from '../core/componentes.js';
import { carregando, el, icone, idDaUrl, substituir, vazio } from '../core/dom.js';
import { data, dataHora, faixa, moeda, prazo, status, TIPOS_REQUISITO } from '../core/formato.js';
import { atualizarTrilha, avisar, comBotaoOcupado, iniciarPagina, mostrarErro } from '../core/layout.js';

const usuario = await iniciarPagina({ trilha: [{ texto: 'Projetos', href: '/projects' }, { texto: 'Projeto' }] });
const projetoId = idDaUrl(2);

let projeto;
try {
    projeto = await get(`/api/projetos/${projetoId}`);
} catch (erro) {
    substituir('pagina', el('div', { class: 'alert alert-warning', text: erro.status === 404 ? 'Projeto não encontrado.' : erro.message }));
    throw erro;
}
atualizarTrilha(projeto.titulo);
document.title = `${projeto.titulo} · TaskBridge`;

const ehPrestador = usuario?.tipoConta === 'PRESTADOR';

substituir('pagina',
    cabecalho(),
    el('div', { class: 'row g-4' },
        el('div', { class: 'col-lg-8' },
            painel('Necessidade', el('p', { class: 'tb-painel-corpo tb-quebra', text: projeto.necessidade })),
            painelEscopo(),
            painelRequisitos(),
            projeto.proprietario && projeto.status !== 'DRAFT' ? painelPropostas() : null,
            projeto.proprietario && projeto.aceitaPropostas ? painelMatching() : null,
            ehPrestador && projeto.aceitaPropostas ? painelEnviarProposta() : null),
        el('div', { class: 'col-lg-4' }, painelFicha(), !usuario && projeto.aceitaPropostas ? painelConvite() : null)));

if (window.location.hash === '#proposta') document.getElementById('proposta')?.scrollIntoView();

function cabecalho() {
    return el('div', { class: 'tb-cabecalho-pagina align-items-center' },
        el('div', { class: 'd-flex gap-3 align-items-center' },
            seloCategoria(projeto.categoria, true),
            el('div', {},
                el('div', { class: 'tb-sobretitulo', text: projeto.categoria?.nome || 'Sem categoria' }),
                el('h1', { text: projeto.titulo }),
                el('div', { class: 'tb-meta mt-1' },
                    status(projeto.status, projeto.statusRotulo),
                    el('span', {}, 'por ', el('a', { href: `/u/${projeto.cliente.nomeUsuario}`, text: projeto.cliente.nomeCompleto })),
                    el('span', { text: projeto.publicadoEm ? 'publicado em ' + data(projeto.publicadoEm) : 'rascunho criado em ' + data(projeto.criadoEm) })))),
        acoesDoDono());
}

function acoesDoDono() {
    if (!projeto.proprietario) return null;
    const acoes = [];
    if (projeto.status === 'DRAFT') {
        acoes.push(el('a', { class: 'btn btn-outline-secondary', href: `/projects/${projetoId}/edit` }, icone('pencil'), 'Continuar editando'));
        acoes.push(botao('Publicar', 'btn-primary', 'send', async () => {
            await post(`/api/projetos/${projetoId}/publicar`);
            avisar('Projeto publicado.');
            window.location.reload();
        }));
        acoes.push(botao('Excluir', 'btn-outline-danger', 'trash-2', async () => {
            if (!await confirmar({ titulo: 'Excluir rascunho', mensagem: 'O rascunho será apagado definitivamente.', confirmar: 'Excluir', perigo: true })) return;
            await del(`/api/projetos/${projetoId}`);
            window.location.assign('/projects?aba=meus');
        }));
    }
    if (projeto.aceitaPropostas) {
        acoes.push(botao('Cancelar projeto', 'btn-outline-danger', 'circle-x', async () => {
            if (!await confirmar({ titulo: 'Cancelar projeto', mensagem: 'As propostas pendentes serão recusadas e o projeto deixará de aparecer para profissionais.', confirmar: 'Cancelar projeto', perigo: true })) return;
            await post(`/api/projetos/${projetoId}/cancelar`);
            window.location.reload();
        }));
    }
    return el('div', { class: 'd-flex flex-wrap gap-2' }, acoes);
}

function botao(texto, classe, nomeIcone, acao) {
    const elemento = el('button', { class: 'btn ' + classe, type: 'button' }, icone(nomeIcone), texto);
    elemento.addEventListener('click', () => comBotaoOcupado(elemento, async () => {
        try {
            await acao();
        } catch (erro) {
            avisar(erro.detalhes?.length ? `${erro.message} ${erro.detalhes.join(' ')}` : erro.message, 'erro');
        }
    }));
    return elemento;
}

function painelEscopo() {
    return painel('Escopo', el('div', { class: 'tb-painel-corpo' },
        el('dl', { class: 'tb-ficha mb-3' },
            el('dt', { text: 'Objetivo' }), el('dd', { text: projeto.objetivo || '—' })),
        el('p', { class: 'tb-quebra', text: projeto.descricao || 'Escopo ainda não descrito.' }),
        projeto.habilidades.length ? el('div', {}, projeto.habilidades.map((habilidade) => el('span', { class: 'tb-etiqueta', text: habilidade }))) : null));
}

function painelRequisitos() {
    const grupos = Object.entries(TIPOS_REQUISITO).map(([tipo, rotulo]) => {
        const itens = projeto.requisitos.filter((item) => item.tipo === tipo);
        return el('div', { class: 'col-md-4' },
            el('h3', { class: 'tb-sobretitulo', text: rotulo + ` (${itens.length})` }),
            itens.length ? el('ul', { class: 'small ps-3' }, itens.map((item) => el('li', { class: 'mb-1', text: item.descricao }))) : el('p', { class: 'small text-secondary', text: '—' }));
    });
    return painel('Requisitos, entregáveis e aceite', el('div', { class: 'tb-painel-corpo row g-3' }, grupos));
}

function painelFicha() {
    const corpo = el('div', { class: 'tb-painel-corpo' },
        el('div', { class: 'tb-sobretitulo', text: 'Orçamento' }),
        el('div', { class: 'tb-valor-grande mb-3', text: faixa(projeto.orcamentoMinimo, projeto.orcamentoMaximo) }),
        el('dl', { class: 'tb-ficha' },
            el('dt', { text: 'Prazo' }), el('dd', { text: prazo(projeto.prazoDias) }),
            el('dt', { text: 'Status' }), el('dd', {}, status(projeto.status, projeto.statusRotulo)),
            el('dt', { text: 'Criado' }), el('dd', { text: dataHora(projeto.criadoEm) })));
    if (projeto.proprietario && projeto.pendenciasPublicacao.length) {
        corpo.append(el('div', { class: 'alert alert-warning mt-3 mb-0 small' }, el('strong', { text: 'Para publicar:' }),
            el('ul', {}, projeto.pendenciasPublicacao.map((texto) => el('li', { text: texto })))));
    }
    return painel('Resumo', corpo);
}

function painelConvite() {
    return painel('É profissional?', el('div', { class: 'tb-painel-corpo small' },
        el('p', { text: 'Entre com uma conta de prestador para enviar uma proposta a este projeto.' }),
        el('a', { class: 'btn btn-sm btn-primary', href: `/login?next=/projects/${projetoId}%23proposta`, text: 'Entrar' })));
}

// ------------------------------------------------------------------ dono: propostas recebidas

function painelPropostas() {
    const corpo = el('div', {}, carregando());
    carregarPropostas(corpo, 0);
    return painel('Propostas recebidas', corpo, { id: 'propostas' });
}

async function carregarPropostas(alvo, pagina) {
    const resultado = await get(`/api/projetos/${projetoId}/propostas?page=${pagina}&size=10`);
    if (!resultado.conteudo.length) {
        substituir(alvo, el('div', { class: 'tb-painel-corpo' }, vazio('Nenhuma proposta ainda. Veja abaixo os profissionais compatíveis e convide-os.')));
        return;
    }
    substituir(alvo, el('ul', { class: 'tb-lista' }, resultado.conteudo.map(itemProposta)), paginacao(resultado, (destino) => carregarPropostas(alvo, destino)));
}

function itemProposta(proposta) {
    const acoes = proposta.status === 'PENDING'
        ? el('div', { class: 'd-flex gap-2 mt-2' },
            botao('Aceitar proposta', 'btn-sm btn-primary', 'check', async () => {
                const aceitar = await confirmar({
                    titulo: 'Aceitar proposta',
                    mensagem: el('div', {},
                        el('p', {}, 'Contratar ', el('strong', { text: proposta.prestador.nomeCompleto }), ` por ${moeda(proposta.valor)} em ${prazo(proposta.prazoDias)}?`),
                        el('p', { class: 'small text-secondary mb-0', text: 'As demais propostas pendentes serão recusadas automaticamente. Em seguida você será levado ao pagamento (PIX em modo de teste).' })),
                    confirmar: 'Aceitar e contratar',
                });
                if (!aceitar) return;
                const contratacao = await post(`/api/propostas/${proposta.id}/aceitar`);
                window.location.assign(`/contracts/${contratacao.id}`);
            }),
            botao('Recusar', 'btn-sm btn-outline-secondary', 'x', async () => {
                await post(`/api/propostas/${proposta.id}/rejeitar`);
                window.location.reload();
            }))
        : null;
    return el('li', { class: 'd-flex gap-3' },
        avatar(proposta.prestador),
        el('div', { class: 'flex-grow-1 min-w-0' },
            el('div', { class: 'd-flex flex-wrap gap-2 align-items-center' },
                el('a', { class: 'tb-lista-titulo', href: `/u/${proposta.prestador.nomeUsuario}`, text: proposta.prestador.nomeCompleto }),
                status(proposta.status, proposta.statusRotulo)),
            el('div', { class: 'tb-meta' },
                el('strong', { class: 'text-dark', text: moeda(proposta.valor) }),
                el('span', { text: prazo(proposta.prazoDias) }),
                el('span', { text: 'enviada em ' + data(proposta.criadaEm) })),
            el('div', { class: 'tb-citacao', text: proposta.mensagem }),
            acoes));
}

// ------------------------------------------------------------------ dono: matching

function painelMatching() {
    const corpo = el('div', {}, carregando('Calculando compatibilidade…'));
    carregarMatching(corpo);
    return painel('Profissionais compatíveis', corpo, {
        classe: 'tb-painel-destaque',
        acao: el('span', { class: 'small text-secondary', text: 'categoria · habilidades · orçamento · prazo · disponibilidade' }),
    });
}

async function carregarMatching(alvo) {
    const sugestoes = await get(`/api/projetos/${projetoId}/profissionais-compativeis`);
    if (!sugestoes.length) {
        substituir(alvo, el('div', { class: 'tb-painel-corpo' }, vazio('Nenhum profissional compatível encontrado por enquanto.')));
        return;
    }
    substituir(alvo, el('ul', { class: 'tb-lista' }, sugestoes.map((sugestao) => {
        const bloco = blocoCompatibilidade(sugestao.pontuacao, sugestao.criterios);
        const convidar = botao('Solicitar proposta', 'btn-sm btn-primary', 'send', async () => {
            await post(`/api/projetos/${projetoId}/convites/${encodeURIComponent(sugestao.profissional.nomeUsuario)}`);
            avisar(`Convite enviado para ${sugestao.profissional.nomeCompleto}.`);
        });
        return el('li', { class: 'tb-match' },
            bloco.pontuacao,
            el('div', { class: 'min-w-0' },
                el('div', { class: 'd-flex gap-2 align-items-center' },
                    avatar(sugestao.profissional),
                    el('div', {},
                        el('a', { class: 'tb-lista-titulo', href: `/u/${sugestao.profissional.nomeUsuario}`, text: sugestao.profissional.nomeCompleto }),
                        el('div', { class: 'tb-meta' }, el('span', { text: sugestao.tituloProfissional }), el('span', { text: `${sugestao.experienciaAnos} anos de experiência` })))),
                bloco.criterios),
            el('div', { class: 'tb-match-acoes d-flex flex-column gap-2' },
                el('a', { class: 'btn btn-sm btn-outline-secondary', href: `/u/${sugestao.profissional.nomeUsuario}` }, icone('user'), 'Ver perfil'),
                convidar));
    })));
}

// ------------------------------------------------------------------ prestador: enviar proposta

function painelEnviarProposta() {
    const formulario = el('form', { class: 'tb-painel-corpo', novalidate: true },
        el('div', { class: 'alert alert-danger', 'data-erro-formulario': true, tabindex: '-1', hidden: true }),
        el('div', { class: 'row g-3' },
            el('div', { class: 'col-sm-6' },
                el('label', { class: 'form-label', for: 'valor', text: 'Valor total (R$)' }),
                el('input', { class: 'form-control', id: 'valor', name: 'valor', type: 'number', min: 1, step: '0.01', required: true })),
            el('div', { class: 'col-sm-6' },
                el('label', { class: 'form-label', for: 'prazoDias', text: 'Prazo de entrega (dias)' }),
                el('input', { class: 'form-control', id: 'prazoDias', name: 'prazoDias', type: 'number', min: 1, max: 365, required: true })),
            el('div', { class: 'col-12' },
                el('label', { class: 'form-label', for: 'mensagem', text: 'Como você vai atender ao escopo' }),
                el('textarea', { class: 'form-control', id: 'mensagem', name: 'mensagem', rows: 5, minlength: 20, maxlength: 2000, required: true }),
                el('div', { class: 'form-text', text: 'Explique a abordagem, o que está incluso e como cada entregável será feito.' }))),
        el('button', { class: 'btn btn-primary mt-3', type: 'submit' }, icone('send'), 'Enviar proposta'));
    formulario.addEventListener('submit', async (evento) => {
        evento.preventDefault();
        if (!formulario.checkValidity()) {
            formulario.reportValidity();
            return;
        }
        const dados = new FormData(formulario);
        await comBotaoOcupado(formulario.querySelector('[type=submit]'), async () => {
            try {
                await post(`/api/projetos/${projetoId}/propostas`, {
                    valor: Number(dados.get('valor')),
                    prazoDias: Number(dados.get('prazoDias')),
                    mensagem: dados.get('mensagem'),
                });
                substituir(formulario, el('div', { class: 'alert alert-success mb-0' }, 'Proposta enviada. Acompanhe em ', el('a', { href: '/proposals', text: 'Propostas' }), '.'));
            } catch (erro) {
                mostrarErro(formulario, erro);
            }
        });
    });
    return painel('Enviar proposta', formulario, { id: 'proposta' });
}
