import { el } from './dom.js';

const moedaBR = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const dataBR = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' });
const dataHoraBR = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
const mesAnoBR = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' });

export const moeda = (valor) => (valor === null || valor === undefined ? '—' : moedaBR.format(Number(valor)));
export const data = (iso) => (iso ? dataBR.format(new Date(iso)) : '—');
export const dataHora = (iso) => (iso ? dataHoraBR.format(new Date(iso)) : '—');
export const mesAno = (iso) => (iso ? mesAnoBR.format(new Date(iso)) : '—');

export function faixa(minimo, maximo) {
    if (minimo == null && maximo == null) return 'A definir';
    return `${moeda(minimo)} – ${moeda(maximo)}`;
}

export const prazo = (dias) => (dias ? `${dias} dia${dias === 1 ? '' : 's'}` : 'A definir');

export const TIPOS_REQUISITO = {
    REQUISITO: 'Requisito',
    ENTREGAVEL: 'Entregável',
    CRITERIO_ACEITE: 'Critério de aceite',
};

export const DISPONIBILIDADES = {
    IMEDIATA: 'Imediata',
    EM_ATE_15_DIAS: 'Em até 15 dias',
    EM_ATE_30_DIAS: 'Em até 30 dias',
    INDISPONIVEL: 'Indisponível',
};

export const MODALIDADES = { REMOTO: 'Remoto', PRESENCIAL: 'Presencial', HIBRIDO: 'Híbrido' };

export const EVENTOS = {
    CONTA_CRIADA: 'Conta criada',
    LOGIN_SUCESSO: 'Login realizado',
    LOGIN_FALHA: 'Tentativa de login falhou',
    LOGIN_CONTA_BLOQUEADA: 'Login recusado: conta bloqueada temporariamente',
    LOGOUT: 'Saída da conta',
    LOGIN_SOCIAL_VINCULADO: 'Login com Google/GitHub vinculado',
    PERFIL_ATUALIZADO: 'Dados do perfil alterados',
    EMAIL_ALTERADO: 'E-mail alterado',
    SENHA_ALTERADA: 'Senha alterada',
    FOTO_ALTERADA: 'Foto de perfil alterada',
    FOTO_REMOVIDA: 'Foto de perfil removida',
    DADOS_EXPORTADOS: 'Dados exportados (LGPD)',
};

// Tom visual do selo de status. Os rótulos em português vêm da API (campo statusRotulo).
const TONS = {
    DRAFT: 'cinza', PUBLISHED: 'roxo', IN_NEGOTIATION: 'ambar', CONTRACTED: 'roxo', IN_PROGRESS: 'preto',
    COMPLETED: 'verde', CANCELLED: 'vermelho', PENDING: 'ambar', ACCEPTED: 'verde', REJECTED: 'vermelho',
    AWAITING_PAYMENT: 'ambar', SUBMITTED: 'roxo', UNDER_REVIEW: 'ambar', APPROVED: 'verde',
    REVISION_REQUIRED: 'vermelho', CREATED: 'cinza', PAID: 'verde', EXPIRED: 'vermelho',
    ATIVO: 'verde', PAUSADO: 'cinza',
};

export function status(codigo, rotulo) {
    return el('span', { class: 'tb-status', dataset: { tom: TONS[codigo] || 'cinza' }, text: rotulo || codigo });
}

export function iniciais(nome) {
    return (nome || '?').split(/\s+/).filter(Boolean).slice(0, 2).map((parte) => parte[0].toUpperCase()).join('');
}
