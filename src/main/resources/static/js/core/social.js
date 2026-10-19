import { get } from './api.js';
import { el } from './dom.js';

const PROVEDORES = {
    // Visual de cada botão segue as diretrizes de marca do provedor (logotipo oficial, cores e texto).
    GOOGLE: { texto: 'Continuar com o Google', icone: '/img/redes/google.svg', classe: 'tb-botao-google', url: '/oauth2/authorization/google' },
    GITHUB: { texto: 'Continuar com o GitHub', icone: '/img/redes/github-branco.svg', classe: 'tb-botao-github', url: '/oauth2/authorization/github' },
};

/**
 * Botões "Continuar com Google/GitHub", só para os provedores configurados no servidor.
 * São links comuns (não formulários): o navegador sai para o provedor e volta pelo backend.
 */
export async function montarLoginSocial(alvoId) {
    const alvo = document.getElementById(alvoId);
    if (!alvo) return;
    let provedores = [];
    try {
        provedores = (await get('/api/config/publica')).provedoresLogin || [];
    } catch {
        return;
    }
    const botoes = provedores.filter((id) => PROVEDORES[id]).map((id) => {
        const provedor = PROVEDORES[id];
        return el('a', { class: `btn tb-botao-social ${provedor.classe} w-100`, href: provedor.url },
            el('img', { src: provedor.icone, alt: '', width: 18, height: 18 }), provedor.texto);
    });
    if (!botoes.length) return;
    alvo.replaceChildren(el('div', { class: 'tb-divisor-ou', text: 'ou' }), el('div', { class: 'd-grid gap-2' }, botoes));
    alvo.hidden = false;
}

export const ERROS_LOGIN_SOCIAL = {
    'email-nao-verificado': 'O e-mail da sua conta no provedor não está verificado. Verifique-o lá e tente de novo.',
    'conta-indisponivel': 'Não foi possível entrar com essa conta. Use e-mail e senha ou fale com o suporte.',
    social: 'O login com o provedor foi cancelado ou falhou. Tente novamente.',
};
