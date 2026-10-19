import { get, post } from '../core/api.js';
import { painel } from '../core/componentes.js';
import { el, icone, idDaUrl, substituir } from '../core/dom.js';
import { dataHora, moeda, status } from '../core/formato.js';
import { avisar, comBotaoOcupado, iniciarPagina } from '../core/layout.js';

await iniciarPagina({ protegida: true, trilha: [{ texto: 'Contratações', href: '/contracts' }, { texto: 'Pagamento PIX' }] });
const pagamentoId = idDaUrl(2);
const configuracao = await get('/api/config/publica');

let pagamento;
let temporizador;
try {
    pagamento = await get(`/api/pagamentos/${pagamentoId}`);
} catch (erro) {
    substituir('pagina', el('div', { class: 'alert alert-warning', text: erro.status === 404 ? 'Pagamento não encontrado.' : erro.message }));
    throw erro;
}
desenhar();
acompanhar();

function desenhar() {
    const aberto = pagamento.status === 'PENDING' || pagamento.status === 'CREATED';
    substituir('pagina',
        el('div', { class: 'tb-cabecalho-pagina' }, el('div', {},
            el('div', { class: 'tb-sobretitulo', text: 'Pagamento via PIX' }),
            el('h1', { text: pagamento.projetoTitulo }),
            el('div', { class: 'tb-meta mt-1' }, status(pagamento.status, pagamento.statusRotulo), el('span', { class: 'tb-mono', text: 'txid ' + pagamento.txid })))),
        pagamento.ambienteTeste ? el('div', { class: 'tb-teste-faixa', role: 'note', text: 'Ambiente de teste — não pague este QR Code. Nenhum valor real é cobrado.' }) : null,
        el('div', { class: 'row g-4' },
            el('div', { class: 'col-lg-8' }, aberto ? painelQrCode() : painelResultado()),
            el('div', { class: 'col-lg-4' }, painelResumo(), aberto && configuracao.simulacaoPagamentoHabilitada ? painelDebug() : null)));
}

function painelQrCode() {
    const copiar = el('button', { class: 'btn btn-sm btn-outline-secondary', type: 'button' }, icone('copy'), 'Copiar código');
    copiar.addEventListener('click', async () => {
        try {
            await navigator.clipboard.writeText(pagamento.pixCopiaECola);
            avisar('Código copiado.');
        } catch {
            avisar('Não foi possível copiar automaticamente. Selecione o texto e copie.', 'erro');
        }
    });
    return painel('Pague com PIX', el('div', { class: 'tb-painel-corpo tb-pix' },
        el('figure', { class: 'tb-pix-qr m-0' },
            el('img', { src: pagamento.qrCode, alt: 'QR Code PIX de teste para ' + moeda(pagamento.valor), width: 280, height: 280 }),
            el('figcaption', { class: 'tb-pix-qr-legenda', text: pagamento.ambienteTeste ? 'QR de teste' : 'Escaneie no app do banco' })),
        el('div', {},
            el('div', { class: 'tb-sobretitulo', text: 'Valor' }),
            el('div', { class: 'tb-valor-grande mb-3', text: moeda(pagamento.valor) }),
            el('ol', { class: 'small ps-3' },
                el('li', { text: 'Abra o app do seu banco e escolha pagar com PIX.' }),
                el('li', { text: 'Escaneie o QR Code ou use o código copia e cola.' }),
                el('li', { text: 'Esta página atualiza sozinha quando o pagamento for confirmado.' })),
            el('div', { class: 'tb-sobretitulo mt-3', text: 'PIX copia e cola' }),
            el('div', { class: 'tb-copia-cola mb-2', text: pagamento.pixCopiaECola }),
            copiar,
            el('div', { class: 'small text-secondary mt-3', 'aria-live': 'polite', id: 'contagem' }))));
}

function painelResultado() {
    const mensagens = {
        PAID: ['alert-success', 'Pagamento confirmado. A contratação está em andamento e o prestador já pode enviar as entregas.'],
        EXPIRED: ['alert-warning', 'Esta cobrança expirou. Gere uma nova cobrança na página da contratação.'],
        CANCELLED: ['alert-warning', 'Esta cobrança foi cancelada. Gere uma nova cobrança na página da contratação.'],
    };
    const [classe, texto] = mensagens[pagamento.status] || ['alert-info', pagamento.statusRotulo];
    return painel('Situação', el('div', { class: 'tb-painel-corpo' },
        el('div', { class: 'alert ' + classe, text: texto }),
        el('a', { class: 'btn btn-primary', href: `/contracts/${pagamento.contratacaoId}` }, icone('arrow-left'), 'Voltar para a contratação')));
}

function painelResumo() {
    return painel('Resumo', el('div', { class: 'tb-painel-corpo' }, el('dl', { class: 'tb-ficha' },
        el('dt', { text: 'Valor' }), el('dd', { text: moeda(pagamento.valor) }),
        el('dt', { text: 'Status' }), el('dd', {}, status(pagamento.status, pagamento.statusRotulo)),
        el('dt', { text: 'Criado' }), el('dd', { text: dataHora(pagamento.criadoEm) }),
        el('dt', { text: 'Expira' }), el('dd', { text: dataHora(pagamento.expiraEm) }),
        el('dt', { text: 'Pago' }), el('dd', { text: dataHora(pagamento.pagoEm) }),
        el('dt', { text: 'Modo' }), el('dd', { text: pagamento.modo === 'MOCK' ? 'Simulado (teste)' : 'Real' }))));
}

/** Só aparece quando o servidor diz que a simulação está habilitada (nunca em produção). */
function painelDebug() {
    const acao = (rotulo, caminho, classe) => {
        const botao = el('button', { class: 'btn btn-sm ' + classe, type: 'button', text: rotulo });
        botao.addEventListener('click', () => comBotaoOcupado(botao, async () => {
            try {
                pagamento = await post(`/api/debug/pagamentos/${pagamentoId}/${caminho}`);
                desenhar();
            } catch (erro) {
                avisar(erro.message, 'erro');
            }
        }));
        return botao;
    };
    return el('section', { class: 'tb-debug', 'aria-label': 'Ferramentas de teste' },
        el('div', { class: 'tb-debug-titulo', text: '// debug · PAYMENT_MODE=MOCK' }),
        el('p', { class: 'small mb-2', text: 'Simule a resposta do provedor PIX. Estas ações não existem em produção.' }),
        el('div', { class: 'd-grid gap-2' },
            acao('Simular pagamento', 'simular-pagamento', 'btn-success'),
            acao('Simular expiração', 'simular-expiracao', 'btn-outline-secondary'),
            acao('Simular cancelamento', 'simular-cancelamento', 'btn-outline-danger')));
}

/** Consulta o status a cada 5 s enquanto a cobrança está aberta e mostra o tempo restante. */
function acompanhar() {
    clearInterval(temporizador);
    let segundos = 0;
    temporizador = setInterval(async () => {
        if (pagamento.status !== 'PENDING' && pagamento.status !== 'CREATED') {
            clearInterval(temporizador);
            return;
        }
        const restante = Math.max(0, Math.floor((new Date(pagamento.expiraEm) - Date.now()) / 1000));
        const contagem = document.getElementById('contagem');
        if (contagem) contagem.textContent = `Expira em ${Math.floor(restante / 60)} min ${String(restante % 60).padStart(2, '0')} s`;
        segundos++;
        if (segundos % 5 === 0) {
            const atualizado = await get(`/api/pagamentos/${pagamentoId}`).catch(() => pagamento);
            if (atualizado.status !== pagamento.status) {
                pagamento = atualizado;
                desenhar();
            }
        }
    }, 1000);
}
