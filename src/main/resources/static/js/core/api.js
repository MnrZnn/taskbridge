// Cliente da API REST. Sessão vai no cookie HttpOnly (JS não enxerga); o token CSRF é obtido em
// GET /api/auth/csrf, fica só em memória e vai no header X-XSRF-TOKEN em toda requisição que altera estado.

let tokenCsrfAtual = null;

export class ErroApi extends Error {
    constructor(status, corpo) {
        super(corpo?.message || mensagemPadrao(status));
        this.status = status;
        this.campos = corpo?.campos || {};
        this.detalhes = corpo?.detalhes || [];
    }
}

function mensagemPadrao(status) {
    if (status === 0) return 'Sem conexão com o servidor.';
    if (status === 401) return 'Sua sessão expirou. Entre novamente.';
    if (status === 403) return 'Você não tem permissão para esta operação.';
    if (status === 404) return 'Não encontrado.';
    if (status === 429) return 'Muitas requisições. Aguarde um instante.';
    return 'Não foi possível concluir a operação.';
}

async function tokenCsrf() {
    if (!tokenCsrfAtual) {
        const resposta = await fetch('/api/auth/csrf', { credentials: 'same-origin', headers: { Accept: 'application/json' } });
        tokenCsrfAtual = (await resposta.json()).token;
    }
    return tokenCsrfAtual;
}

/**
 * @param {string} caminho  sempre relativo (/api/...)
 * @param {{method?: string, corpo?: any, formulario?: FormData}} opcoes
 */
export async function api(caminho, opcoes = {}, novaTentativa = true) {
    const { method = 'GET', corpo, formulario } = opcoes;
    const headers = { Accept: 'application/json' };
    const init = { method, headers, credentials: 'same-origin' };
    if (method !== 'GET') {
        headers['X-XSRF-TOKEN'] = await tokenCsrf();
    }
    if (formulario) {
        init.body = formulario;
    } else if (corpo !== undefined) {
        headers['Content-Type'] = 'application/json';
        init.body = JSON.stringify(corpo);
    }

    let resposta;
    try {
        resposta = await fetch(caminho, init);
    } catch {
        throw new ErroApi(0, null);
    }
    // Login e logout trocam o token (proteção contra fixação de sessão); busca um novo na próxima vez.
    if (caminho === '/api/auth/login' || caminho === '/api/auth/logout') tokenCsrfAtual = null;
    // Sessão expirada leva o token junto: renova uma vez e repete a requisição.
    if (resposta.status === 403 && method !== 'GET' && novaTentativa) {
        const corpoErro = await resposta.clone().json().catch(() => null);
        if (corpoErro?.message?.includes('CSRF')) {
            tokenCsrfAtual = null;
            return api(caminho, opcoes, false);
        }
    }
    if (resposta.status === 204) return null;
    const ehJson = (resposta.headers.get('Content-Type') || '').includes('application/json');
    const dados = ehJson ? await resposta.json() : null;
    if (!resposta.ok) throw new ErroApi(resposta.status, dados);
    return dados;
}

export const get = (caminho) => api(caminho);
export const post = (caminho, corpo) => api(caminho, { method: 'POST', corpo });
export const put = (caminho, corpo) => api(caminho, { method: 'PUT', corpo });
export const del = (caminho) => api(caminho, { method: 'DELETE' });

export function consulta(parametros) {
    const busca = new URLSearchParams();
    Object.entries(parametros).forEach(([chave, valor]) => {
        if (valor !== undefined && valor !== null && valor !== '') busca.set(chave, valor);
    });
    const texto = busca.toString();
    return texto ? '?' + texto : '';
}
