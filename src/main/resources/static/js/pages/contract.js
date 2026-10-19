import { get, post } from '../core/api.js';
import { avatar, confirmar, painel } from '../core/componentes.js';
import { el, icone, idDaUrl, substituir } from '../core/dom.js';
import { dataHora, moeda, prazo, status } from '../core/formato.js';
import { atualizarTrilha, avisar, comBotaoOcupado, iniciarPagina } from '../core/layout.js';

await iniciarPagina({ protegida: true, trilha: [{ texto: 'Contratações', href: '/contracts' }, { texto: 'Contratação' }] });
const contratacaoId = idDaUrl(2);

let contratacao;
try {
    contratacao = await get(`/api/contratacoes/${contratacaoId}`);
} catch (erro) {
    substituir('pagina', el('div', { class: 'alert alert-warning', text: erro.status === 404 ? 'Contratação não encontrada.' : erro.message }));
    throw erro;
}
desenhar();

function desenhar() {
    atualizarTrilha(contratacao.projetoTitulo);
    const cliente = contratacao.papel === 'CLIENTE';
    substituir('pagina',
        el('div', { class: 'tb-cabecalho-pagina' },
            el('div', {},
                el('div', { class: 'tb-sobretitulo', text: cliente ? 'Você contratou' : 'Você foi contratado' }),
                el('h1', { text: contratacao.projetoTitulo }),
                el('div', { class: 'tb-meta mt-1' },
                    status(contratacao.status, contratacao.statusRotulo),
                    el('a', { href: `/projects/${contratacao.projetoId}`, text: 'ver escopo do projeto' })))),
        linhaDoTempo(),
        el('div', { class: 'row g-4' },
            el('div', { class: 'col-lg-8' },
                contratacao.status === 'AWAITING_PAYMENT' ? painelPagamento(cliente) : null,
                painelEntregas(cliente)),
            el('div', { class: 'col-lg-4' }, painelPartes(), painelCriterios())));
}

function linhaDoTempo() {
    const etapas = [
        ['Contratação criada', true],
        ['Pagamento confirmado', contratacao.iniciadaEm !== null],
        ['Entregas em andamento', contratacao.status === 'IN_PROGRESS' || contratacao.status === 'COMPLETED'],
        ['Concluída', contratacao.status === 'COMPLETED'],
    ];
    const atual = etapas.findIndex(([, feito]) => !feito);
    return el('ol', { class: 'tb-etapas tb-etapas-4', 'aria-label': 'Andamento da contratação' }, etapas.map(([nome, feito], indice) =>
        el('li', { class: feito ? 'tb-concluida' : null, 'aria-current': indice === atual ? 'step' : null }, el('span', { text: nome }))));
}

function painelPagamento(cliente) {
    if (!cliente) {
        return painel('Pagamento', el('div', { class: 'tb-painel-corpo' },
            el('p', { class: 'mb-0', text: 'Aguardando o pagamento do cliente. Você será notificado quando for confirmado — só então as entregas são liberadas.' })));
    }
    const pagar = el('button', { class: 'btn btn-primary', type: 'button' }, icone('qr-code'),
        contratacao.pagamentoEmAbertoId ? 'Continuar pagamento PIX' : 'Pagar com PIX');
    pagar.addEventListener('click', () => comBotaoOcupado(pagar, async () => {
        try {
            const pagamento = await post(`/api/contratacoes/${contratacaoId}/pagamentos`);
            window.location.assign(`/payment/${pagamento.id}`);
        } catch (erro) {
            avisar(erro.message, 'erro');
        }
    }));
    return painel('Pagamento', el('div', { class: 'tb-painel-corpo d-flex flex-wrap gap-3 align-items-center justify-content-between' },
        el('div', {},
            el('div', { class: 'tb-sobretitulo', text: 'Valor da contratação' }),
            el('div', { class: 'tb-valor-grande', text: moeda(contratacao.valor) }),
            el('div', { class: 'small text-secondary mt-1', text: 'Ambiente de teste: o PIX é simulado e nenhum valor real é cobrado.' })),
        pagar), { classe: 'tb-painel-destaque' });
}

function painelEntregas(cliente) {
    const itens = contratacao.entregas.map((entrega, indice) => el('div', { class: 'tb-entrega' },
        el('div', { class: 'd-flex flex-wrap gap-2 align-items-center justify-content-between' },
            el('div', { class: 'd-flex gap-2 align-items-baseline' },
                el('span', { class: 'tb-entrega-numero', text: String(indice + 1).padStart(2, '0') }),
                el('strong', { text: entrega.titulo })),
            status(entrega.status, entrega.statusRotulo)),
        entrega.conteudoEnvio ? el('div', {},
            el('div', { class: 'tb-meta mt-2', text: 'Enviado em ' + dataHora(entrega.enviadaEm) }),
            el('div', { class: 'tb-citacao', text: entrega.conteudoEnvio })) : null,
        entrega.feedbackRevisao ? el('div', {},
            el('div', { class: 'tb-meta mt-2', text: 'Revisão solicitada em ' + dataHora(entrega.avaliadaEm) }),
            el('div', { class: 'tb-citacao tb-citacao-revisao', text: entrega.feedbackRevisao })) : null,
        entrega.status === 'APPROVED' ? el('div', { class: 'tb-meta mt-2', text: 'Aprovada em ' + dataHora(entrega.avaliadaEm) }) : null,
        cliente ? acoesCliente(entrega) : acoesPrestador(entrega)));
    const ajuda = contratacao.status === 'AWAITING_PAYMENT'
        ? el('div', { class: 'alert alert-info m-3 mb-0 small', text: 'As entregas são liberadas depois da confirmação do pagamento.' })
        : null;
    return painel(`Entregas (${contratacao.entregas.filter((entrega) => entrega.status === 'APPROVED').length}/${contratacao.entregas.length} aprovadas)`, el('div', {}, ajuda, itens));
}

function acoesPrestador(entrega) {
    const podeEnviar = contratacao.status === 'IN_PROGRESS' && (entrega.status === 'PENDING' || entrega.status === 'REVISION_REQUIRED');
    if (!podeEnviar) return null;
    const campo = el('textarea', { class: 'form-control form-control-sm', rows: 3, maxlength: 2000, required: true, 'aria-label': 'Descrição da entrega',
        placeholder: 'Descreva o que está sendo entregue: links, arquivos compartilhados, observações.' });
    const enviar = el('button', { class: 'btn btn-sm btn-primary mt-2', type: 'submit' }, icone('upload'), entrega.status === 'REVISION_REQUIRED' ? 'Reenviar entrega' : 'Enviar entrega');
    const formulario = el('form', { class: 'mt-2' }, campo, enviar);
    formulario.addEventListener('submit', (evento) => {
        evento.preventDefault();
        if (!campo.value.trim()) {
            campo.reportValidity();
            return;
        }
        executar(enviar, `/entregas/${entrega.id}/enviar`, { conteudo: campo.value.trim() }, 'Entrega enviada para revisão.');
    });
    return formulario;
}

function acoesCliente(entrega) {
    if (entrega.status !== 'SUBMITTED' && entrega.status !== 'UNDER_REVIEW') return null;
    const barra = el('div', { class: 'd-flex flex-wrap gap-2 mt-2' });
    if (entrega.status === 'SUBMITTED') {
        const revisar = el('button', { class: 'btn btn-sm btn-outline-secondary', type: 'button' }, icone('eye'), 'Marcar em revisão');
        revisar.addEventListener('click', () => executar(revisar, `/entregas/${entrega.id}/iniciar-revisao`));
        barra.append(revisar);
    }
    const aprovar = el('button', { class: 'btn btn-sm btn-success', type: 'button' }, icone('check'), 'Aprovar');
    aprovar.addEventListener('click', async () => {
        const ultima = contratacao.entregas.filter((item) => item.status !== 'APPROVED').length === 1;
        const ok = await confirmar({
            titulo: 'Aprovar entrega',
            mensagem: ultima ? 'Esta é a última entrega pendente: aprová-la conclui a contratação e o projeto.' : 'Uma entrega aprovada não pode voltar a ficar pendente.',
            confirmar: 'Aprovar',
        });
        if (ok) executar(aprovar, `/entregas/${entrega.id}/aprovar`, undefined, ultima ? 'Contratação concluída.' : 'Entrega aprovada.');
    });
    const pedirRevisao = el('button', { class: 'btn btn-sm btn-outline-danger', type: 'button' }, icone('rotate-ccw'), 'Solicitar revisão');
    const formularioRevisao = el('form', { class: 'mt-2', hidden: true });
    const feedback = el('textarea', { class: 'form-control form-control-sm', rows: 3, maxlength: 1000, required: true, 'aria-label': 'O que precisa ser revisado', placeholder: 'Explique o que precisa ser ajustado, com base nos critérios de aceite.' });
    const enviarRevisao = el('button', { class: 'btn btn-sm btn-danger mt-2', type: 'submit', text: 'Enviar pedido de revisão' });
    formularioRevisao.append(feedback, enviarRevisao);
    pedirRevisao.addEventListener('click', () => {
        formularioRevisao.hidden = !formularioRevisao.hidden;
        if (!formularioRevisao.hidden) feedback.focus();
    });
    formularioRevisao.addEventListener('submit', (evento) => {
        evento.preventDefault();
        if (!feedback.value.trim()) {
            feedback.reportValidity();
            return;
        }
        executar(enviarRevisao, `/entregas/${entrega.id}/solicitar-revisao`, { feedback: feedback.value.trim() }, 'Revisão solicitada.');
    });
    barra.append(aprovar, pedirRevisao);
    return el('div', {}, barra, formularioRevisao);
}

function executar(botao, caminho, corpo, mensagem) {
    return comBotaoOcupado(botao, async () => {
        try {
            contratacao = await post(`/api/contratacoes/${contratacaoId}${caminho}`, corpo);
            if (mensagem) avisar(mensagem);
            desenhar();
        } catch (erro) {
            avisar(erro.message, 'erro');
        }
    });
}

function painelPartes() {
    const parte = (rotulo, usuario) => el('div', { class: 'd-flex gap-2 align-items-center mb-2' },
        avatar(usuario),
        el('div', {}, el('div', { class: 'tb-meta', text: rotulo }), el('a', { href: `/u/${usuario.nomeUsuario}`, text: usuario.nomeCompleto })));
    return painel('Contratação', el('div', { class: 'tb-painel-corpo' },
        parte('Cliente', contratacao.cliente),
        parte('Prestador', contratacao.prestador),
        el('dl', { class: 'tb-ficha mt-3' },
            el('dt', { text: 'Valor' }), el('dd', { text: moeda(contratacao.valor) }),
            el('dt', { text: 'Prazo' }), el('dd', { text: prazo(contratacao.prazoDias) }),
            el('dt', { text: 'Criada' }), el('dd', { text: dataHora(contratacao.criadaEm) }),
            el('dt', { text: 'Iniciada' }), el('dd', { text: dataHora(contratacao.iniciadaEm) }),
            el('dt', { text: 'Concluída' }), el('dd', { text: dataHora(contratacao.concluidaEm) }))));
}

function painelCriterios() {
    return painel('Critérios de aceite', el('div', { class: 'tb-painel-corpo' },
        contratacao.criteriosAceite.length
            ? el('ul', { class: 'small ps-3 mb-0' }, contratacao.criteriosAceite.map((criterio) => el('li', { class: 'mb-1', text: criterio.descricao })))
            : el('p', { class: 'small text-secondary', text: 'Nenhum critério definido.' })));
}
